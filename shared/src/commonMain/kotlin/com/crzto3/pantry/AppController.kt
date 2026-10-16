package com.crzto3.pantry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.crzto3.pantry.server.LocalServer

internal sealed interface Screen {
    data object Connecting : Screen
    data object Settings : Screen
    data class Error(val message: String) : Screen
    data class Web(val url: String) : Screen
}

class AppController internal constructor(private val settings: SettingsStore) {
    internal val defaultBackendUrl: String =
        normalizeBackendUrl(BuildConfig.BACKEND_URL) ?: platformDefaultBackendUrl

    var backendUrl: String by mutableStateOf(settings.backendUrl ?: defaultBackendUrl)
        private set

    internal var screen: Screen by mutableStateOf(Screen.Connecting)
        private set

    internal var connectAttempt by mutableIntStateOf(0)
        private set

    internal var reloadKey by mutableIntStateOf(0)
        private set

    private var server: LocalServer? = null

    val telegram = TelegramBridge()

    fun reload() {
        if (screen is Screen.Web) reloadKey++ else retry()
    }

    fun openSettings() {
        screen = Screen.Settings
    }

    internal fun retry() {
        connectAttempt++
    }

    internal fun saveBackendUrl(raw: String): Boolean {
        val url = normalizeBackendUrl(raw) ?: return false
        settings.backendUrl = url.takeIf { it != defaultBackendUrl }
        backendUrl = url
        retry()
        return true
    }

    internal suspend fun connect() {
        screen = Screen.Connecting

        val current = server?.takeIf { it.backendUrl == backendUrl } ?: run {
            server?.stop()
            LocalServer(backendUrl, telegram).also { server = it }
        }

        val localUrl = try {
            current.start()
        } catch (e: Exception) {
            screen = Screen.Error("Не удалось запустить встроенный сервер: ${e.describe()}")
            return
        }

        current.checkBackend().onFailure {
            screen = Screen.Error("Сервер $backendUrl недоступен.\n${it.describe()}")
            return
        }

        screen = Screen.Web(localUrl)
    }

    internal suspend fun ensureServerAlive() {
        val current = server ?: return
        if (screen !is Screen.Web || current.isAlive()) return
        try {
            screen = Screen.Web(current.restart())
            reloadKey++
        } catch (e: Exception) {
            screen = Screen.Error("Не удалось перезапустить встроенный сервер: ${e.describe()}")
        }
    }

    internal fun shutdown() {
        server?.stop()
        server = null
    }

    private fun Throwable.describe(): String = message ?: this::class.simpleName ?: "неизвестная ошибка"
}

@Composable
fun rememberAppController(): AppController {
    val settings = rememberSettingsStore()
    return remember(settings) { AppController(settings) }
}

internal fun normalizeBackendUrl(raw: String): String? {
    val url = raw.trim().let { if (it.contains("://")) it else "http://$it" }
    val scheme = url.substringBefore("://").lowercase()
    if (scheme != "http" && scheme != "https") return null

    val rest = url.substringAfter("://")
        .trimEnd('/')
        .removeSuffix("/api/v1")
        .removeSuffix("/api")
        .trimEnd('/')
    if (rest.isBlank()) return null
    return "$scheme://$rest"
}
