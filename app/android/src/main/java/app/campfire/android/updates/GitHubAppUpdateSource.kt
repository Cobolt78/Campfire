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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
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

  private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  override fun changes(): Flow<Unit> = _changes.asSharedFlow()

  @Volatile
  private var cachedUpdate: AppUpdate? = null

  @Volatile
  private var cachedDownloadUrl: String? = null

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
    val cached = cachedUpdate

    if (!force && cached != null && (now - lastCheck) in 0 until COOLDOWN_DURATION_MS) {
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

        val jsonString = response.body.string()
        val releaseJson = JSONObject(jsonString)

        val tagName = releaseJson.optString("tag_name", "")
        val releaseName = releaseJson.optString("name", "")
        val body = releaseJson.optString("body", "")

        val remoteVersionCode = parseVersionCode(tagName)
          ?: parseVersionCode(releaseName)
          ?: return@withContext CheckUpdateResult.Error("Unrecognized release version tag")

        appStateSettings.lastAppUpdateCheckTimestamp = now

        // Check if remote version is strictly newer than current installed app
        if (remoteVersionCode <= applicationInfo.versionCode) {
          cachedUpdate = null
          cachedDownloadUrl = null
          _changes.tryEmit(Unit)
          return@withContext CheckUpdateResult.UpToDate
        }

        val remoteVersionName = parseVersionName(tagName).ifEmpty {
          parseVersionName(releaseName)
        }.ifEmpty {
          tagName.removePrefix("v")
        }

        // Find matching APK asset for current flavor
        val assetsArray = releaseJson.optJSONArray("assets")
          ?: return@withContext CheckUpdateResult.Error("No release assets found")
        var matchedDownloadUrl: String? = null
        val isFoss = applicationInfo.flavor == Flavor.Foss

        for (i in 0 until assetsArray.length()) {
          val asset = assetsArray.getJSONObject(i)
          val assetName = asset.optString("name", "").lowercase()
          val downloadUrl = asset.optString("browser_download_url", "")

          if (!assetName.endsWith(".apk")) continue

          if (isFoss && assetName.contains("foss")) {
            matchedDownloadUrl = downloadUrl
            break
          } else if (!isFoss && (assetName.contains("standard") || !assetName.contains("foss"))) {
            matchedDownloadUrl = downloadUrl
            break
          }
        }

        if (matchedDownloadUrl.isNullOrBlank()) {
          bark(LogPriority.WARN) { "No matching APK asset found in release $tagName for flavor ${applicationInfo.flavor}" }
          return@withContext CheckUpdateResult.Error("No matching APK asset found for flavor ${applicationInfo.flavor}")
        }

        cachedDownloadUrl = matchedDownloadUrl

        val update = AppUpdate(
          versionName = remoteVersionName,
          versionCode = remoteVersionCode,
          releaseNotes = body.ifBlank { null },
        )
        cachedUpdate = update
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
    private const val RELEASES_API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
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
