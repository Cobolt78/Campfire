// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.updates

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.Flavor
import app.campfire.core.di.AppScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.settings.api.AppStateSettings
import app.campfire.updates.source.AppUpdate
import app.campfire.updates.source.AppUpdateProgress
import app.campfire.updates.source.AppUpdateProgress.Status
import app.campfire.updates.source.AppUpdateSource
import app.campfire.updates.source.CheckUpdateResult
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

/**
 * [AppUpdateSource] backed by GitHub Releases API for custom releases.
 * Checks Cobolt78/Campfire for newer releases, matches the flavor (FOSS vs Standard),
 * downloads the APK into app cache, and launches Android's package installer.
 */
@ContributesBinding(AppScope::class, replaces = [NoOpUpdateSource::class])
@Inject
class GitHubAppUpdateSource(
  private val application: Application,
  private val applicationInfo: ApplicationInfo,
  private val appStateSettings: AppStateSettings,
) : AppUpdateSource {

  private val okHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(30, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .followRedirects(true)
      .followSslRedirects(true)
      .build()
  }

  private val _changes = MutableSharedFlow<Unit>(
    replay = 1,
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )
  override fun changes(): Flow<Unit> = _changes.asSharedFlow()

  @Volatile
  private var cachedUpdate: AppUpdate? = null

  @Volatile
  private var cachedDownloadUrl: String? = null

  private fun getPersistedUpdate(): AppUpdate? {
    val code = appStateSettings.cachedUpdateVersionCode
    val name = appStateSettings.cachedUpdateVersionName
    val url = appStateSettings.cachedUpdateDownloadUrl
    if (code > applicationInfo.versionCode && !name.isNullOrBlank() && !url.isNullOrBlank()) {
      cachedDownloadUrl = url
      return AppUpdate(
        versionName = name,
        versionCode = code,
        releaseNotes = appStateSettings.cachedUpdateReleaseNotes,
      )
    }
    return null
  }

  override val isSupported: Boolean = true

  override fun isSignedIn(): Boolean = true

  override suspend fun signIn() = Unit

  override suspend fun isUpdateAvailable(): Boolean = getAvailableUpdate() != null

  override suspend fun getAvailableUpdate(): AppUpdate? {
    return when (val result = checkForUpdates(force = false)) {
      is CheckUpdateResult.Available -> result.update
      else -> null
    }
  }

  override suspend fun checkForUpdates(force: Boolean): CheckUpdateResult = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val lastCheck = appStateSettings.lastAppUpdateCheckTimestamp
    val cached = cachedUpdate ?: getPersistedUpdate().also { cachedUpdate = it }

    if (!force && cached != null && (now - lastCheck) in 0 until COOLDOWN_DURATION_MS) {
      _changes.tryEmit(Unit)
      return@withContext CheckUpdateResult.Available(cached)
    }
    if (!force && cached == null && (now - lastCheck) in 0 until COOLDOWN_DURATION_MS) {
      return@withContext CheckUpdateResult.UpToDate
    }

    try {
      val request = Request.Builder()
        .url(RELEASES_API_URL)
        .header("Accept", "application/vnd.github+json")
        .header("User-Agent", applicationInfo.userAgent)
        .build()

      okHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          bark(LogPriority.WARN) { "GitHub Releases API returned HTTP ${response.code}" }
          return@withContext if (cached != null) {
            if (force) {
              appStateSettings.appUpdateDismissedVersionCode = 0L
              _changes.tryEmit(Unit)
            }
            CheckUpdateResult.Available(cached)
          } else {
            CheckUpdateResult.Error("HTTP ${response.code}")
          }
        }

        val jsonString = response.body.string().trim()
        val releasesArray = JSONArray(jsonString)
        if (releasesArray.length() == 0) {
          return@withContext CheckUpdateResult.UpToDate
        }

        var newestVersionCode: Long = 0L
        var newestVersionName: String = ""
        var newestDownloadUrl: String? = null
        var newestBody: String = ""
        val isFoss = applicationInfo.flavor == Flavor.Foss

        for (i in 0 until releasesArray.length()) {
          val releaseJson = releasesArray.getJSONObject(i)
          if (releaseJson.optBoolean("draft", false)) continue

          val tagName = releaseJson.optString("tag_name", "")
          val releaseName = releaseJson.optString("name", "")
          val vCode = parseVersionCode(tagName) ?: parseVersionCode(releaseName) ?: continue

          if (vCode <= applicationInfo.versionCode) continue
          if (vCode <= newestVersionCode) continue

          val assetsArray = releaseJson.optJSONArray("assets") ?: continue
          var matchedUrl: String? = null
          for (j in 0 until assetsArray.length()) {
            val asset = assetsArray.getJSONObject(j)
            val assetName = asset.optString("name", "").lowercase()
            val downloadUrl = asset.optString("browser_download_url", "")

            if (!assetName.endsWith(".apk")) continue

            if (isFoss && assetName.contains("foss")) {
              matchedUrl = downloadUrl
              break
            } else if (!isFoss && (assetName.contains("standard") || !assetName.contains("foss"))) {
              matchedUrl = downloadUrl
              break
            }
          }

          if (matchedUrl != null) {
            val vName = parseVersionName(tagName).ifEmpty {
              parseVersionName(releaseName)
            }.ifEmpty {
              tagName.removePrefix("v")
            }
            newestVersionCode = vCode
            newestVersionName = vName
            newestDownloadUrl = matchedUrl
            newestBody = releaseJson.optString("body", "")
          }
        }

        appStateSettings.lastAppUpdateCheckTimestamp = now

        if (newestVersionCode <= applicationInfo.versionCode || newestDownloadUrl == null) {
          cachedUpdate = null
          cachedDownloadUrl = null
          appStateSettings.cachedUpdateVersionCode = 0L
          appStateSettings.cachedUpdateVersionName = null
          appStateSettings.cachedUpdateDownloadUrl = null
          appStateSettings.cachedUpdateReleaseNotes = null
          _changes.tryEmit(Unit)
          return@withContext CheckUpdateResult.UpToDate
        }

        cachedDownloadUrl = newestDownloadUrl
        val update = AppUpdate(
          versionName = newestVersionName,
          versionCode = newestVersionCode,
          releaseNotes = newestBody.ifBlank { null },
        )
        cachedUpdate = update
        appStateSettings.cachedUpdateVersionCode = newestVersionCode
        appStateSettings.cachedUpdateVersionName = newestVersionName
        appStateSettings.cachedUpdateDownloadUrl = newestDownloadUrl
        appStateSettings.cachedUpdateReleaseNotes = update.releaseNotes

        if (force) {
          appStateSettings.appUpdateDismissedVersionCode = 0L
        }
        _changes.tryEmit(Unit)
        CheckUpdateResult.Available(update)
      }
    } catch (e: Exception) {
      bark(LogPriority.WARN, throwable = e) { "Failed to check for GitHub updates" }
      if (cached != null) {
        if (force) {
          appStateSettings.appUpdateDismissedVersionCode = 0L
          _changes.tryEmit(Unit)
        }
        CheckUpdateResult.Available(cached)
      } else {
        CheckUpdateResult.Error(e.message ?: "Failed to connect to GitHub")
      }
    }
  }

  override suspend fun installUpdate(): Flow<AppUpdateProgress> = flow {
    emit(AppUpdateProgress(-1L, -1L, Status.Pending))

    if (!ensureInstallPermission()) {
      emit(AppUpdateProgress(-1L, -1L, Status.Canceled))
      return@flow
    }

    val downloadUrl = cachedDownloadUrl ?: run {
      getAvailableUpdate()
      cachedDownloadUrl
    }

    if (downloadUrl.isNullOrBlank()) {
      bark(LogPriority.ERROR) { "No download URL available for update" }
      emit(AppUpdateProgress(-1L, -1L, Status.Failed))
      return@flow
    }

    val versionCode = cachedUpdate?.versionCode ?: appStateSettings.cachedUpdateVersionCode
    val updateDir = File(application.cacheDir, "updates")
    if (!updateDir.exists()) {
      updateDir.mkdirs()
    }
    val apkFile = File(updateDir, "campfire-update-$versionCode.apk")
    val partFile = File(updateDir, "campfire-update-$versionCode.apk.part")

    try {
      if (!apkFile.exists() || apkFile.length() <= 0L) {
        if (partFile.exists()) {
          partFile.delete()
        }

        val request = Request.Builder()
          .url(downloadUrl)
          .header("User-Agent", applicationInfo.userAgent)
          .build()

        okHttpClient.newCall(request).execute().use { response ->
          if (!response.isSuccessful) {
            bark(LogPriority.ERROR) { "Failed to download update APK: HTTP ${response.code}" }
            emit(AppUpdateProgress(-1L, -1L, Status.Failed))
            return@flow
          }

          val body = response.body
          val totalBytes = body.contentLength()
          var bytesDownloaded = 0L
          var lastEmitTime = 0L

          val buffer = ByteArray(32 * 1024)
          body.byteStream().use { input ->
            partFile.outputStream().use { output ->
              var read: Int
              while (input.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
                bytesDownloaded += read
                val now = System.currentTimeMillis()
                if (now - lastEmitTime > 100L || bytesDownloaded == totalBytes) {
                  lastEmitTime = now
                  emit(
                    AppUpdateProgress(
                      bytes = bytesDownloaded,
                      totalBytes = totalBytes,
                      status = Status.Downloading,
                    ),
                  )
                }
              }
              output.flush()
            }
          }

          if (totalBytes > 0L && bytesDownloaded != totalBytes) {
            partFile.delete()
            bark(LogPriority.ERROR) { "Incomplete APK download: $bytesDownloaded / $totalBytes" }
            emit(AppUpdateProgress(-1L, -1L, Status.Failed))
            return@flow
          }

          updateDir.listFiles()?.forEach { file ->
            if (file != partFile) {
              file.delete()
            }
          }
          if (!partFile.renameTo(apkFile)) {
            partFile.delete()
            bark(LogPriority.ERROR) { "Failed to finalize downloaded APK" }
            emit(AppUpdateProgress(-1L, -1L, Status.Failed))
            return@flow
          }
        }
      } else {
        val cachedSize = apkFile.length()
        emit(AppUpdateProgress(cachedSize, cachedSize, Status.Downloading))
      }

      if (!ensureInstallPermission()) {
        emit(AppUpdateProgress(-1L, -1L, Status.Canceled))
        return@flow
      }

      val apkSize = apkFile.length()
      when (val outcome = commitInstallSession(apkFile)) {
        is GitHubInstallReceiver.InstallSessionOutcome.PendingUserAction -> {
          emit(AppUpdateProgress(apkSize, apkSize, Status.Downloaded))
          withContext(Dispatchers.Main) {
            application.startActivity(outcome.confirmIntent)
          }
        }

        GitHubInstallReceiver.InstallSessionOutcome.Success -> {
          emit(AppUpdateProgress(apkSize, apkSize, Status.Downloaded))
        }

        GitHubInstallReceiver.InstallSessionOutcome.Canceled -> {
          emit(AppUpdateProgress(-1L, -1L, Status.Canceled))
        }

        is GitHubInstallReceiver.InstallSessionOutcome.Failed -> {
          bark(LogPriority.ERROR) { "Install session failed: ${outcome.message}" }
          emit(AppUpdateProgress(-1L, -1L, Status.Failed))
        }

        GitHubInstallReceiver.InstallSessionOutcome.TimedOut -> {
          bark(LogPriority.ERROR) { "Timed out waiting for PackageInstaller session response" }
          emit(AppUpdateProgress(apkSize, apkSize, Status.Downloaded))
        }
      }
    } catch (e: SecurityException) {
      bark(LogPriority.ERROR, throwable = e) { "Install permission denied" }
      openUnknownAppSourcesSettings()
      emit(AppUpdateProgress(-1L, -1L, Status.Canceled))
    } catch (e: Exception) {
      bark(LogPriority.ERROR, throwable = e) { "Error downloading/installing update" }
      emit(AppUpdateProgress(-1L, -1L, Status.Failed))
    }
  }.flowOn(Dispatchers.IO)

  private fun ensureInstallPermission(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !application.packageManager.canRequestPackageInstalls()) {
      openUnknownAppSourcesSettings()
      return false
    }
    return true
  }

  private fun openUnknownAppSourcesSettings() {
    try {
      val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
        data = Uri.parse("package:${application.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      application.startActivity(settingsIntent)
    } catch (settingsError: Exception) {
      bark(LogPriority.ERROR, throwable = settingsError) { "Failed to open unknown app sources settings" }
    }
  }

  private suspend fun commitInstallSession(apkFile: File): GitHubInstallReceiver.InstallSessionOutcome {
    val packageInstaller = application.packageManager.packageInstaller
    for (existing in packageInstaller.mySessions) {
      runCatching { packageInstaller.abandonSession(existing.sessionId) }
    }

    val apkSize = apkFile.length()
    val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
      setAppPackageName(application.packageName)
      setSize(apkSize)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        setPackageSource(PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE)
      }
    }

    val sessionId = packageInstaller.createSession(params)
    val resultDeferred = GitHubInstallReceiver.registerPendingSession(sessionId)
    var committed = false
    try {
      packageInstaller.openSession(sessionId).use { session ->
        session.openWrite("campfire-update.apk", 0, apkSize).use { output ->
          apkFile.inputStream().use { input ->
            input.copyTo(output, bufferSize = 32 * 1024)
          }
          session.fsync(output)
        }

        val statusIntent = Intent(application, GitHubInstallReceiver::class.java).apply {
          action = GitHubInstallReceiver.ACTION_INSTALL_STATUS
        }
        val pendingIntent = PendingIntent.getBroadcast(
          application,
          sessionId,
          statusIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        session.commit(pendingIntent.intentSender)
        committed = true
      }

      return withTimeoutOrNull(15_000L) {
        resultDeferred.await()
      } ?: run {
        GitHubInstallReceiver.unregisterPendingSession(sessionId)
        GitHubInstallReceiver.InstallSessionOutcome.TimedOut
      }
    } catch (t: Throwable) {
      GitHubInstallReceiver.unregisterPendingSession(sessionId)
      if (!committed) {
        runCatching { packageInstaller.abandonSession(sessionId) }
      }
      throw t
    }
  }

  companion object {
    private const val GITHUB_REPO = "Cobolt78/Campfire"
    private const val RELEASES_API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases?per_page=10"
    private val COOLDOWN_DURATION_MS = TimeUnit.HOURS.toMillis(1)

    private fun parseVersionCode(raw: String): Long? {
      val match = Regex("""(?:v)?(\d+)\.(\d+)\.(\d+)(?:[.-]rc(\d+))?""", RegexOption.IGNORE_CASE).find(raw)
        ?: return null
      val (major, minor, patch, rc) = match.destructured
      val rcCode = rc.toIntOrNull() ?: 99
      return major.toLong() * 1_000_000L +
        minor.toLong() * 10_000L +
        patch.toLong() * 100L +
        rcCode.toLong()
    }

    private fun parseVersionName(raw: String): String {
      val match = Regex("""(?:v)?(\d+\.\d+\.\d+(?:[.-]rc\d+)?)""", RegexOption.IGNORE_CASE).find(raw)
      return match?.groupValues?.get(1) ?: ""
    }
  }
}
