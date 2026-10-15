package com.crzto3.pantry

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class TelegramAuthRequest internal constructor(val botId: String, val requestAccess: String?) {
    internal val result = CompletableDeferred<String?>()

    val url: String
        get() {
            val origin = "https://${BuildConfig.TELEGRAM_DOMAIN}"
            return buildString {
                append("https://oauth.telegram.org/auth?bot_id=").append(botId)
                append("&origin=").append(encodeQueryValue(origin))
                if (requestAccess != null) append("&request_access=").append(encodeQueryValue(requestAccess))
                append("&return_to=").append(encodeQueryValue("$origin/"))
            }
        }
}

class TelegramBridge {
    private val _request = MutableStateFlow<TelegramAuthRequest?>(null)
    val request: StateFlow<TelegramAuthRequest?> = _request.asStateFlow()

    internal suspend fun authorize(botId: String, requestAccess: String?): String? {
        _request.value?.result?.complete(null)
        val request = TelegramAuthRequest(botId, requestAccess)
        _request.value = request
        try {
            return request.result.await()
        } finally {
            _request.compareAndSet(request, null)
        }
    }

    fun finish(request: TelegramAuthRequest, payload: String?) {
        request.result.complete(payload)
        _request.compareAndSet(request, null)
    }
}

internal sealed interface TelegramNavigation {
    data object Allow : TelegramNavigation
    data object External : TelegramNavigation
    data class Finished(val payload: String?) : TelegramNavigation
}

internal object TelegramOAuth {
    private const val RESULT_HOST = "pantry-native.invalid"

    val openerShim: String = """
        (function () {
          if (window.__pantryOpener) return;
          window.__pantryOpener = true;
          var fake = {
            closed: false,
            focus: function () {},
            postMessage: function (data) {
              var text = typeof data === 'string' ? data : JSON.stringify(data);
              location.href = 'https://$RESULT_HOST/result#' + encodeURIComponent(text);
            }
          };
          try { window.opener = fake; } catch (e) {}
        })();
    """.trimIndent()

    fun classify(url: String): TelegramNavigation {
        val scheme = url.substringBefore(':').lowercase()
        if (scheme == "about" || scheme == "data" || scheme == "blob") return TelegramNavigation.Allow
        if (scheme != "http" && scheme != "https") return TelegramNavigation.External

        val host = url.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')
            .substringAfterLast('@').substringBefore(':').lowercase()
        val fragment = url.substringAfter('#', "")

        if (host == RESULT_HOST) return TelegramNavigation.Finished(percentDecode(fragment).ifEmpty { null })

        val tgResult = Regex("""[#?&]tgAuthResult=([A-Za-z0-9\-_=]*)""").find(url)?.groupValues?.get(1)
        if (tgResult != null) return TelegramNavigation.Finished(decodeBase64Url(tgResult))

        if (host == BuildConfig.TELEGRAM_DOMAIN.lowercase()) return TelegramNavigation.Finished(null)
        if (host == "oauth.telegram.org" || host == "telegram.org") return TelegramNavigation.Allow
        return TelegramNavigation.External
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeBase64Url(value: String): String? = try {
        Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
            .decode(value.trimEnd('='))
            .decodeToString()
    } catch (_: IllegalArgumentException) {
        null
    }
}

internal fun encodeQueryValue(value: String): String = buildString {
    value.encodeToByteArray().forEach { byte ->
        val c = byte.toInt().toChar()
        if (c.isLetterOrDigit() && byte >= 0 || c in "-_.~") append(c)
        else append('%').append(((byte.toInt() and 0xFF) or 0x100).toString(16).substring(1).uppercase())
    }
}

internal fun percentDecode(value: String): String {
    val bytes = ArrayList<Byte>(value.length)
    var i = 0
    while (i < value.length) {
        val c = value[i]
        if (c == '%' && i + 2 < value.length) {
            val code = value.substring(i + 1, i + 3).toIntOrNull(16)
            if (code != null) {
                bytes.add(code.toByte())
                i += 3
                continue
            }
        }
        c.toString().encodeToByteArray().forEach { bytes.add(it) }
        i++
    }
    return bytes.toByteArray().decodeToString()
}
