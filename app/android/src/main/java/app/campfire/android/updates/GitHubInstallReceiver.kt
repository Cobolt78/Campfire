// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.updates

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred

class GitHubInstallReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent?) {
    if (intent == null || intent.action != ACTION_INSTALL_STATUS) return

    val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
    val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
    val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

    when (status) {
      PackageInstaller.STATUS_PENDING_USER_ACTION -> {
        val confirmIntent: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
          @Suppress("DEPRECATION")
          intent.getParcelableExtra(Intent.EXTRA_INTENT)
        }
        if (confirmIntent != null) {
          confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          val delivered = completeSession(sessionId, InstallSessionOutcome.PendingUserAction(confirmIntent))
          if (!delivered) {
            runCatching { context.startActivity(confirmIntent) }
          }
        } else {
          bark(LogPriority.ERROR) { "PackageInstaller requested user action without confirm intent: $message" }
          completeSession(sessionId, InstallSessionOutcome.Failed("Missing confirmation intent"))
        }
      }

      PackageInstaller.STATUS_SUCCESS -> {
        bark { "PackageInstaller session $sessionId succeeded" }
        completeSession(sessionId, InstallSessionOutcome.Success)
      }

      PackageInstaller.STATUS_FAILURE_ABORTED -> {
        bark { "PackageInstaller session $sessionId aborted: $message" }
        completeSession(sessionId, InstallSessionOutcome.Canceled)
      }

      else -> {
        bark(LogPriority.ERROR) { "PackageInstaller session $sessionId failed (status=$status): $message" }
        completeSession(sessionId, InstallSessionOutcome.Failed(message))
      }
    }
  }

  sealed interface InstallSessionOutcome {
    data class PendingUserAction(val confirmIntent: Intent) : InstallSessionOutcome
    data object Success : InstallSessionOutcome
    data object Canceled : InstallSessionOutcome
    data class Failed(val message: String?) : InstallSessionOutcome
    data object TimedOut : InstallSessionOutcome
  }

  companion object {
    const val ACTION_INSTALL_STATUS = "app.campfire.android.updates.INSTALL_STATUS"
    private val pendingSessions = ConcurrentHashMap<Int, CompletableDeferred<InstallSessionOutcome>>()

    fun registerPendingSession(sessionId: Int): CompletableDeferred<InstallSessionOutcome> {
      return CompletableDeferred<InstallSessionOutcome>().also { deferred ->
        pendingSessions[sessionId] = deferred
      }
    }

    fun unregisterPendingSession(sessionId: Int) {
      pendingSessions.remove(sessionId)
    }

    private fun completeSession(sessionId: Int, outcome: InstallSessionOutcome): Boolean {
      val deferred = pendingSessions.remove(sessionId) ?: return false
      return deferred.complete(outcome)
    }
  }
}
