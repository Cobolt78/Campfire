// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android

import android.app.Application
import android.app.SearchManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.campfire.audioplayer.impl.AudioPlayerService
import app.campfire.core.logging.LogPriority
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import app.campfire.android.di.ActivityComponent
import app.campfire.android.toast.AndroidToast
import app.campfire.common.compose.toast.LocalToast
import app.campfire.core.ActivityIntentProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.logging.bark
import app.campfire.core.navigation.DeepLink
import app.campfire.core.session.serverUrl
import app.campfire.core.toast.GlobalToaster
import app.campfire.tracing.DiTraceSections
import app.campfire.tracing.Trace
import app.campfire.tracing.trace
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private lateinit var component: ActivityComponent

  private val deepLinkFlow = MutableStateFlow<DeepLink>(DeepLink.None)

  override fun onCreate(savedInstanceState: Bundle?) = Trace.trace("MainActivity.onCreate") {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    bark { "MainActivity::onCreate()" }

    component = Trace.trace(DiTraceSections.ACTIVITY_GRAPH) {
      ComponentHolder.component<ActivityComponent.Factory>()
        .create(this)
        .also {
          ComponentHolder.updateComponent(lifecycleScope, it)
        }
    }

    // Register all flow launchers
    component.componentActivityPlugins.forEach { launcher ->
      launcher.register(this)
    }

    // Upgrade path: a user already signed in to a LAN server before the app began targeting
    // Android 17+ won't pass through the login screen, so prompt for local-network access here
    // if their active server is private and the permission is missing. Waits for the session to
    // restore (it may still be Loading at onCreate), then requests once. No-ops otherwise.
    lifecycleScope.launch {
      val serverUrl = component.userSessionManager.observe()
        .mapNotNull { it.serverUrl }
        .first()
      component.localNetworkPermission.requestIfNeeded(serverUrl)
    }

    // Parse Deeplink & Voice Intent
    intent?.let {
      updateDeepLink(it)
      handleVoiceIntent(it)
    }

    // Configure toaster
    val toaster = AndroidToast(this)
    GlobalToaster.register(toaster)

    setContent {
      val deepLink by remember {
        deepLinkFlow
      }.collectAsState()

      val uriHandler = remember {
        object : UriHandler {
          override fun openUri(uri: String) {
            val intent = CustomTabsIntent.Builder().build()
            intent.launchUrl(this@MainActivity, uri.toUri())
          }
        }
      }

      CompositionLocalProvider(
        LocalToast provides toaster,
        LocalUriHandler provides uriHandler,
      ) {
        component.campfireContent(
          ::finish,
          deepLink,
          Modifier,
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)

    updateDeepLink(intent)
    handleVoiceIntent(intent)
  }

  private fun handleVoiceIntent(intent: Intent?) {
    if (intent == null) return
    val action = intent.action
    if (action == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) {
      val query = intent.getStringExtra(SearchManager.QUERY)
        ?: intent.getStringExtra("query")
      val isPause = intent.getBooleanExtra("action_pause", false) ||
        intent.getStringExtra("query") == "action_pause"

      bark(LogPriority.INFO) { "MainActivity: handling voice MEDIA_PLAY_FROM_SEARCH (query='$query', isPause=$isPause)" }
      lifecycleScope.launch {
        try {
          val sessionToken = SessionToken(
            this@MainActivity,
            ComponentName(this@MainActivity, AudioPlayerService::class.java),
          )
          val controllerFuture = MediaController.Builder(this@MainActivity, sessionToken).buildAsync()
          controllerFuture.addListener(
            {
              try {
                val controller = controllerFuture.get()
                if (isPause) {
                  controller.pause()
                } else if (!query.isNullOrBlank()) {
                  val mediaItem = MediaItem.Builder()
                    .setMediaId("search:$query")
                    .setRequestMetadata(
                      MediaItem.RequestMetadata.Builder()
                        .setSearchQuery(query)
                        .build(),
                    )
                    .build()
                  controller.setMediaItem(mediaItem)
                  controller.prepare()
                  controller.play()
                } else {
                  controller.play()
                }
              } catch (e: Exception) {
                bark(LogPriority.ERROR, throwable = e) { "Failed to execute voice playback command" }
              }
            },
            ContextCompat.getMainExecutor(this@MainActivity),
          )
        } catch (e: Exception) {
          bark(LogPriority.ERROR, throwable = e) { "Failed to bind media controller for voice intent" }
        }
      }
    }
  }

  private fun updateDeepLink(intent: Intent) {
    val deepLink = intent.toDeepLink(
      allowAutomation = BuildConfig.DEBUG,
      nonce = System.nanoTime(),
    )
    if (deepLink != DeepLink.None) {
      deepLinkFlow.value = deepLink
    }
  }

  override fun onStart() {
    super.onStart()
    bark { "MainActivity::onStart()" }
    component.offlineDownloadManager.resumeDownloads()
  }

  override fun onDestroy() {
    super.onDestroy()
    bark { "MainActivity::onDestroy()" }
    GlobalToaster.unregister()
    component.componentActivityPlugins.forEach { launcher ->
      launcher.unregister()
    }
    ComponentHolder.removeComponent(component)
  }
}

@ContributesBinding(AppScope::class)
@Inject
class MainActivityIntentProvider(
  private val application: Application,
) : ActivityIntentProvider {

  override fun provide(): Intent {
    return Intent(application, MainActivity::class.java)
  }
}
