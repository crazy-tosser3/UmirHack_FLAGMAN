package com.crzto3.pantry

import com.crzto3.pantry.server.LocalServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.friwi.jcefmaven.CefAppBuilder
import me.friwi.jcefmaven.EnumProgress
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter
import org.cef.CefApp
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.network.CefRequest
import java.awt.Desktop
import java.io.File
import java.net.URI
import javax.swing.SwingUtilities

object DesktopBrowser {
    internal sealed interface State {
        data class Preparing(val message: String, val progress: Float? = null) : State
        data class Ready(val app: CefApp) : State
        data class Failed(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Preparing("Запуск браузерного движка…"))
    internal val state: StateFlow<State> = _state.asStateFlow()

    private val initLock = Mutex()
    private var app: CefApp? = null

    internal suspend fun init() = initLock.withLock {
        if (app != null) return@withLock
        _state.value = State.Preparing("Запуск браузерного движка…")
        try {
            app = withContext(Dispatchers.IO) { build() }
            _state.value = State.Ready(app!!)
        } catch (e: Exception) {
            _state.value = State.Failed(e.message ?: e::class.java.simpleName)
        }
    }

    private fun build(): CefApp {
        val dataDir = appDataDir()
        val builder = CefAppBuilder()
        builder.setInstallDir(File(dataDir, "jcef-bundle"))
        builder.setProgressHandler { progress, percent ->
            _state.value = when (progress) {
                EnumProgress.DOWNLOADING -> State.Preparing(
                    "Загрузка браузерного движка (только при первом запуске)…",
                    percent.takeIf { it >= 0 }?.div(100f),
                )
                EnumProgress.EXTRACTING -> State.Preparing("Распаковка браузерного движка…")
                else -> State.Preparing("Запуск браузерного движка…")
            }
        }
        builder.cefSettings.windowless_rendering_enabled = false

        val cacheRoot = File(dataDir, "cef-profile")
        builder.cefSettings.root_cache_path = cacheRoot.absolutePath
        builder.cefSettings.cache_path = File(cacheRoot, "default").absolutePath
        builder.cefSettings.persist_session_cookies = true
        builder.setAppHandler(object : MavenCefAppHandlerAdapter() {})
        return builder.build()
    }

    internal fun createBrowser(app: CefApp, url: String): CefBrowser {
        val client = app.createClient()
        client.addRequestHandler(object : CefRequestHandlerAdapter() {
            override fun onBeforeBrowse(
                browser: CefBrowser?,
                frame: CefFrame?,
                request: CefRequest,
                userGesture: Boolean,
                isRedirect: Boolean,
            ): Boolean = openIfExternal(request.url)
        })
        client.addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
            override fun onBeforePopup(
                browser: CefBrowser,
                frame: CefFrame?,
                targetUrl: String?,
                targetFrameName: String?,
            ): Boolean {
                if (targetUrl != null && !openIfExternal(targetUrl)) browser.loadURL(targetUrl)
                return true
            }
        })
        return client.createBrowser(url, false, false)
    }

    internal fun createTelegramAuthBrowser(app: CefApp, url: String, onFinished: (String?) -> Unit): CefBrowser {
        val client = app.createClient()
        val handle = { target: String ->
            when (val navigation = TelegramOAuth.classify(target)) {
                TelegramNavigation.Allow -> false
                TelegramNavigation.External -> {
                    openInSystemBrowser(target)
                    true
                }
                is TelegramNavigation.Finished -> {
                    SwingUtilities.invokeLater { onFinished(navigation.payload) }
                    true
                }
            }
        }
        client.addRequestHandler(object : CefRequestHandlerAdapter() {
            override fun onBeforeBrowse(
                browser: CefBrowser?,
                frame: CefFrame?,
                request: CefRequest,
                userGesture: Boolean,
                isRedirect: Boolean,
            ): Boolean = handle(request.url)
        })
        client.addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
            override fun onBeforePopup(
                browser: CefBrowser,
                frame: CefFrame?,
                targetUrl: String?,
                targetFrameName: String?,
            ): Boolean {
                if (targetUrl != null && !handle(targetUrl)) browser.loadURL(targetUrl)
                return true
            }
        })
        client.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                if (frame.isMain) frame.executeJavaScript(TelegramOAuth.openerShim, frame.url, 0)
            }
        })
        return client.createBrowser(url, false, false)
    }

    fun dispose() {
        app?.dispose()
        app = null
    }

    private fun openIfExternal(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        val external = uri.scheme in setOf("http", "https") && uri.host != LocalServer.HOST
        if (external) openInSystemBrowser(url)
        return external
    }

    private fun openInSystemBrowser(url: String) {
        val uri = runCatching { URI(url) }.getOrNull() ?: return
        if (Desktop.isDesktopSupported()) {
            runCatching { Desktop.getDesktop().browse(uri) }
        }
    }

    private fun appDataDir(): File {
        val os = System.getProperty("os.name").lowercase()
        val home = System.getProperty("user.home")
        val dir = when {
            os.contains("mac") -> File(home, "Library/Application Support/Pantry")
            os.contains("win") -> File(System.getenv("APPDATA") ?: home, "Pantry")
            else -> File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share", "pantry")
        }
        return dir.apply { mkdirs() }
    }
}
