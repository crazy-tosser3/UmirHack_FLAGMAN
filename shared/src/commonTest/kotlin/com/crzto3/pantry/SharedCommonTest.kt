package com.crzto3.pantry

import com.crzto3.pantry.server.LocalServer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedCommonTest {

    @Test
    fun normalizesBackendUrl() {
        assertEquals("http://localhost:8080", normalizeBackendUrl(" localhost:8080/ "))
        assertEquals("https://pantry.example.com", normalizeBackendUrl("https://pantry.example.com/api/v1"))
        assertEquals("http://10.0.0.5:8080/pantry", normalizeBackendUrl("http://10.0.0.5:8080/pantry/"))
        assertNull(normalizeBackendUrl(""))
        assertNull(normalizeBackendUrl("ftp://example.com"))
        assertNull(normalizeBackendUrl("http://"))
    }

    @Test
    fun rewritesCookiesForLocalOrigin() {
        assertEquals(
            "refresh=abc; Path=/api/v1/auth; HttpOnly; SameSite=Lax; Max-Age=600",
            LocalServer.rewriteSetCookie(
                "refresh=abc; Path=/api/v1/auth; Domain=pantry.example.com; Secure; HttpOnly; SameSite=None; Max-Age=600"
            ),
        )
    }

    @Test
    fun detectsLocalNetworkAddresses() {
        listOf(
            "http://192.168.0.169:8080", "http://10.0.2.2:8080", "http://172.20.1.1",
            "http://pantry.local:8080", "http://nas", "http://[fd00::1]:8080",
        ).forEach { assertTrue(isLocalNetworkUrl(it), it) }
        listOf(
            "http://localhost:8080", "https://pantry.example.com", "http://8.8.8.8", "http://172.32.0.1",
        ).forEach { assertFalse(isLocalNetworkUrl(it), it) }
    }

    @Test
    fun buildsTelegramAuthUrlForBotDomain() {
        val url = TelegramAuthRequest("123", "write").url
        assertEquals(
            "https://oauth.telegram.org/auth?bot_id=123&origin=https%3A%2F%2Fhack.crzto3.ru" +
                "&request_access=write&return_to=https%3A%2F%2Fhack.crzto3.ru%2F",
            url,
        )
    }

    @Test
    fun classifiesTelegramNavigation() {
        assertEquals(TelegramNavigation.Allow, TelegramOAuth.classify("https://oauth.telegram.org/auth/push?x=1"))
        assertEquals(TelegramNavigation.External, TelegramOAuth.classify("tg://resolve?domain=x"))
        assertEquals(TelegramNavigation.External, TelegramOAuth.classify("https://t.me/somebot"))
        assertEquals(TelegramNavigation.Finished(null), TelegramOAuth.classify("https://hack.crzto3.ru/"))

        val message = """{"event":"auth_result","result":{"id":1,"hash":"ab"}}"""
        assertEquals(
            TelegramNavigation.Finished(message),
            TelegramOAuth.classify("https://pantry-native.invalid/result#" + encodeQueryValue(message)),
        )

        val user = """{"id":42,"first_name":"Иван","auth_date":1,"hash":"f00"}"""
        val encoded = kotlin.io.encoding.Base64.UrlSafe.encode(user.encodeToByteArray()).trimEnd('=')
        assertEquals(
            TelegramNavigation.Finished(user),
            TelegramOAuth.classify("https://hack.crzto3.ru/#tgAuthResult=$encoded"),
        )
    }
}
