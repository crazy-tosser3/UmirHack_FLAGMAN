package com.crzto3.pantry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import java.awt.BorderLayout
import java.util.prefs.Preferences
import javax.swing.JPanel

internal actual val platformDefaultBackendUrl: String = "http://localhost:8080"

@Composable
internal actual fun PlatformWebView(url: String, reloadKey: Int, modifier: Modifier) {
    LaunchedEffect(Unit) { DesktopBrowser.init() }

    when (val state = DesktopBrowser.state.collectAsState().value) {
        is DesktopBrowser.State.Ready -> {
            val browser = remember(state) { DesktopBrowser.createBrowser(state.app, url) }
            LaunchedEffect(browser, url) {
                if (browser.url != url) browser.loadURL(url)
            }
            LaunchedEffect(browser, reloadKey) {
                if (reloadKey > 0) browser.reload()
            }
            DisposableEffect(browser) {
                onDispose { browser.close(true) }
            }
            SwingPanel(
                modifier = modifier,
                factory = { JPanel(BorderLayout()).apply { add(browser.uiComponent, BorderLayout.CENTER) } },
            )
        }

        is DesktopBrowser.State.Preparing -> Box(modifier.padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val progress = state.progress
                if (progress == null) {
                    CircularProgressIndicator()
                } else {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.width(320.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(state.message, textAlign = TextAlign.Center)
            }
        }

        is DesktopBrowser.State.Failed -> Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Не удалось запустить браузерный движок:\n${state.message}", textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal actual fun rememberSettingsStore(): SettingsStore = remember { PreferencesSettings }

private object PreferencesSettings : SettingsStore {
    private const val KEY_BACKEND_URL = "backendUrl"
    private val prefs: Preferences = Preferences.userRoot().node("com/crzto3/pantry")

    override var backendUrl: String?
        get() = prefs.get(KEY_BACKEND_URL, null)
        set(value) {
            if (value == null) prefs.remove(KEY_BACKEND_URL) else prefs.put(KEY_BACKEND_URL, value)
            prefs.flush()
        }
}

@Composable
internal actual fun rememberNetworkAccess(backendUrl: String): NetworkAccess = NetworkAccess.Granted

@Composable
internal actual fun TelegramAuthHost(request: TelegramAuthRequest, onResult: (String?) -> Unit) {
    val currentOnResult by rememberUpdatedState(onResult)
    var finished by remember { mutableStateOf(false) }
    val finish = { payload: String? ->
        if (!finished) {
            finished = true
            currentOnResult(payload)
        }
    }

    LaunchedEffect(Unit) { DesktopBrowser.init() }

    DialogWindow(
        onCloseRequest = { finish(null) },
        title = "Вход через Telegram",
        state = rememberDialogState(size = DpSize(560.dp, 720.dp)),
    ) {
        TelegramAuthFrame(onCancel = { finish(null) }) {
            when (val state = DesktopBrowser.state.collectAsState().value) {
                is DesktopBrowser.State.Ready -> {
                    val browser = remember(state) {
                        DesktopBrowser.createTelegramAuthBrowser(state.app, request.url) { finish(it) }
                    }
                    DisposableEffect(browser) { onDispose { browser.close(true) } }
                    SwingPanel(
                        modifier = Modifier.fillMaxSize(),
                        factory = { JPanel(BorderLayout()).apply { add(browser.uiComponent, BorderLayout.CENTER) } },
                    )
                }
                is DesktopBrowser.State.Preparing -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(state.message, textAlign = TextAlign.Center)
                    }
                }
                is DesktopBrowser.State.Failed -> LaunchedEffect(state) { finish(null) }
            }
        }
    }
}
