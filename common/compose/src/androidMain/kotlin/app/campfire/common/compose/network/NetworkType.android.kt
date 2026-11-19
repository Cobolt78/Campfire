// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private fun isCurrentlyCellularOrMetered(context: Context): Boolean {
  return try {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val activeNetwork = cm?.activeNetwork
    val capabilities = activeNetwork?.let { cm.getNetworkCapabilities(it) }
    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true ||
      capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false
  } catch (_: Throwable) {
    false
  }
}

@Composable
actual fun rememberIsCellularOrMetered(): Boolean {
  val context = LocalContext.current
  return isCurrentlyCellularOrMetered(context)
}

@Composable
actual fun rememberIsCellularOrMeteredChecker(): () -> Boolean {
  val context = LocalContext.current
  return remember(context) {
    { isCurrentlyCellularOrMetered(context) }
  }
}
