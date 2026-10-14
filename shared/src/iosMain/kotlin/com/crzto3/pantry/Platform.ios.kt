package com.crzto3.pantry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.crzto3.pantry.server.LocalServer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore
import platform.darwin.NSObject

internal actual val platformDefaultBackendUrl: String = "http://localhost:8080"

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun PlatformWebView(url: String, reloadKey: Int, modifier: Modifier) {
    val linksDelegate = remember { ExternalLinksDelegate() }
    val webView = remember {
        val configuration = WKWebViewConfiguration().apply {
            websiteDataStore = WKWebsiteDataStore.defaultDataStore()
        }
        WKWebView(frame = CGRectZero.readValue(), configuration = configuration).apply {
            allowsBackForwardNavigationGestures = true
            navigationDelegate = linksDelegate
            inspectable = true
        }
    }

    LaunchedEffect(url) {
        NSURL.URLWithString(url)?.let { webView.loadRequest(NSURLRequest.requestWithURL(it)) }
    }
    LaunchedEffect(reloadKey) {
        if (reloadKey > 0) webView.reload()
    }

    UIKitView(factory = { webView }, modifier = modifier)
}

private class ExternalLinksDelegate : NSObject(), WKNavigationDelegateProtocol {
    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit,
    ) {
        val target = decidePolicyForNavigationAction.request.URL
        val scheme = target?.scheme?.lowercase()
        val external = target != null && target.host != LocalServer.HOST && (scheme == "http" || scheme == "https")
        if (external) {
            UIApplication.sharedApplication.openURL(target, options = emptyMap<Any?, Any>(), completionHandler = null)
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
        } else {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
        }
    }
}

@Composable
internal actual fun rememberSettingsStore(): SettingsStore = remember { UserDefaultsSettings }

private object UserDefaultsSettings : SettingsStore {
    private const val KEY_BACKEND_URL = "backendUrl"
    private val defaults get() = NSUserDefaults.standardUserDefaults

    override var backendUrl: String?
        get() = defaults.stringForKey(KEY_BACKEND_URL)
        set(value) {
            if (value == null) defaults.removeObjectForKey(KEY_BACKEND_URL) else defaults.setObject(value, KEY_BACKEND_URL)
        }
}

@Composable
internal actual fun rememberNetworkAccess(backendUrl: String): NetworkAccess = NetworkAccess.Granted
