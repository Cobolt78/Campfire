// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberIsCellularOrMetered(): Boolean {
  val context = LocalContext.current

  var isCellularOrMetered by remember(context) {
    mutableStateOf(checkIsCellularOrMetered(context))
  }

  DisposableEffect(context) {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      ?: return@DisposableEffect onDispose {}

    val callback = object : ConnectivityManager.NetworkCallback() {
      override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
        isCellularOrMetered = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
          !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
      }

      override fun onLost(network: Network) {
        isCellularOrMetered = checkIsCellularOrMetered(context)
      }
    }

    val request = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()

    try {
      cm.registerNetworkCallback(request, callback)
    } catch (_: Throwable) {
      // Permission or hardware not available; fall back to the snapshot value
    }

    onDispose {
      try {
        cm.unregisterNetworkCallback(callback)
      } catch (_: Throwable) {
        // Already unregistered or unavailable
      }
    }
  }

  return isCellularOrMetered
}

private fun checkIsCellularOrMetered(context: Context): Boolean {
  return try {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
    caps != null && (
      caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
        !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
      )
  } catch (_: Throwable) {
    false
  }
}
