package com.crzto3.pantry

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal expect val platformDefaultBackendUrl: String

@Composable
internal expect fun PlatformWebView(url: String, reloadKey: Int, modifier: Modifier)

internal interface SettingsStore {
    var backendUrl: String?
}

@Composable
internal expect fun rememberSettingsStore(): SettingsStore

internal class NetworkAccess(val ready: Boolean, val missing: Boolean, val request: () -> Unit) {
    companion object {
        val Granted = NetworkAccess(ready = true, missing = false, request = {})
    }
}

@Composable
internal expect fun rememberNetworkAccess(backendUrl: String): NetworkAccess
