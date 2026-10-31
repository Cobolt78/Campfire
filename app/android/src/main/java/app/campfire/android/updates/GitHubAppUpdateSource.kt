// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.updates

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

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

    val downloadUrl = cachedDownloadUrl ?: run {
      getAvailableUpdate()
      cachedDownloadUrl
    }

    if (downloadUrl.isNullOrBlank()) {
      bark(LogPriority.ERROR) { "No download URL available for update" }
      emit(AppUpdateProgress(-1L, -1L, Status.Failed))
      return@flow
    }

    val updateDir = File(application.cacheDir, "updates")
    if (!updateDir.exists()) {
      updateDir.mkdirs()
    }
    val apkFile = File(updateDir, "campfire-update.apk")
    if (apkFile.exists()) {
      apkFile.delete()
    }

    try {
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
          apkFile.outputStream().use { output ->
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

        emit(
          AppUpdateProgress(
            bytes = bytesDownloaded,
            totalBytes = totalBytes,
            status = Status.Downloaded,
          ),
        )

        launchPackageInstaller(apkFile)
      }
    } catch (e: Exception) {
      bark(LogPriority.ERROR, throwable = e) { "Error downloading/installing update" }
      emit(AppUpdateProgress(-1L, -1L, Status.Failed))
    }
  }.flowOn(Dispatchers.IO)

  private fun launchPackageInstaller(apkFile: File) {
    try {
      val contentUri = FileProvider.getUriForFile(
        application,
        "${application.packageName}.update_provider",
        apkFile,
      )

      val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(contentUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }

      application.startActivity(installIntent)
    } catch (e: Exception) {
      bark(LogPriority.ERROR, throwable = e) { "Failed to launch package installer" }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !application.packageManager.canRequestPackageInstalls()) {
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
