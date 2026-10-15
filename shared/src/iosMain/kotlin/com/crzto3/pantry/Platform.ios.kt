package com.crzto3.pantry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIBarButtonItem
import platform.UIKit.UIBarButtonItemStyle
import platform.UIKit.UINavigationController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.navigationItem
import platform.UIKit.modalInPresentation
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
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

@Composable
internal actual fun TelegramAuthHost(request: TelegramAuthRequest, onResult: (String?) -> Unit) {
    val currentOnResult by rememberUpdatedState(onResult)
    DisposableEffect(request) {
        val session = TelegramAuthSession(request.url) { currentOnResult(it) }
        session.present()
        onDispose { session.dismiss() }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class TelegramAuthSession(private val url: String, private val onResult: (String?) -> Unit) {
    private var finished = false
    private val navigationDelegate = TelegramNavigationDelegate(::finish)
    private val cancelTarget = CancelTarget { finish(null) }
    private var navigationController: UINavigationController? = null

    fun present() {
        val configuration = WKWebViewConfiguration().apply {
            websiteDataStore = WKWebsiteDataStore.defaultDataStore()
            userContentController.addUserScript(
                WKUserScript(
                    source = TelegramOAuth.openerShim,
                    injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentEnd,
                    forMainFrameOnly = true,
                )
            )
        }
        val webView = WKWebView(frame = CGRectZero.readValue(), configuration = configuration).apply {
            navigationDelegate = this@TelegramAuthSession.navigationDelegate
            inspectable = true
        }
        val content = UIViewController().apply {
            view = webView
            title = "Вход через Telegram"
            navigationItem.leftBarButtonItem = UIBarButtonItem(
                title = "Отмена",
                style = UIBarButtonItemStyle.UIBarButtonItemStylePlain,
                target = cancelTarget,
                action = NSSelectorFromString("cancel"),
            )
        }
        val navigation = UINavigationController(rootViewController = content).apply {
            modalInPresentation = true
        }
        navigationController = navigation
        NSURL.URLWithString(url)?.let { webView.loadRequest(NSURLRequest.requestWithURL(it)) }
        topViewController()?.presentViewController(navigation, animated = true, completion = null)
    }

    fun dismiss() {
        navigationController?.dismissViewControllerAnimated(true, completion = null)
        navigationController = null
    }

    private fun finish(payload: String?) {
        if (finished) return
        finished = true
        onResult(payload)
    }

    private fun topViewController(): UIViewController? {
        val window = UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { scene -> scene.windows.filterIsInstance<UIWindow>() }
            .firstOrNull { it.isKeyWindow() }
            ?: return null
        var top = window.rootViewController
        while (top?.presentedViewController != null) top = top.presentedViewController
        return top
    }
}

private class CancelTarget(private val onCancel: () -> Unit) : NSObject() {
    @ObjCAction
    fun cancel() = onCancel()
}

private class TelegramNavigationDelegate(private val onFinished: (String?) -> Unit) : NSObject(), WKNavigationDelegateProtocol {
    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit,
    ) {
        val target = decidePolicyForNavigationAction.request.URL
        val policy = when (val navigation = TelegramOAuth.classify(target?.absoluteString ?: "about:blank")) {
            TelegramNavigation.Allow -> WKNavigationActionPolicy.WKNavigationActionPolicyAllow
            TelegramNavigation.External -> {
                target?.let { UIApplication.sharedApplication.openURL(it, options = emptyMap<Any?, Any>(), completionHandler = null) }
                WKNavigationActionPolicy.WKNavigationActionPolicyCancel
            }
            is TelegramNavigation.Finished -> {
                onFinished(navigation.payload)
                WKNavigationActionPolicy.WKNavigationActionPolicyCancel
            }
        }
        decisionHandler(policy)
    }
}
