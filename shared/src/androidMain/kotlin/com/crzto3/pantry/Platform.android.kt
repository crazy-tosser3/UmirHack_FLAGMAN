package com.crzto3.pantry

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.crzto3.pantry.server.LocalServer

internal actual val platformDefaultBackendUrl: String = "http://10.0.2.2:8080"

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal actual fun PlatformWebView(url: String, reloadKey: Int, modifier: Modifier) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    BackHandler(enabled = canGoBack) { webView?.goBack() }

    AndroidView(
        modifier = modifier.imePadding(),
        factory = { context ->
            WebView.setWebContentsDebuggingEnabled(
                context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
            )
            CookieManager.getInstance().setAcceptCookie(true)
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        if (request.url.host == LocalServer.HOST) return false
                        openExternally(context, request.url.toString())
                        return true
                    }

                    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                        canGoBack = view.canGoBack()
                    }
                }
                tag = url
                loadUrl(url)
                webView = this
            }
        },
        update = { view ->
            if (view.tag != url) {
                view.tag = url
                view.loadUrl(url)
            }
        },
    )

    LaunchedEffect(reloadKey) {
        if (reloadKey > 0) webView?.reload()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        CookieManager.getInstance().flush()
    }
    DisposableEffect(Unit) {
        onDispose {
            CookieManager.getInstance().flush()
            webView?.destroy()
        }
    }
}

private fun openExternally(context: Context, url: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: ActivityNotFoundException) {
    }
}

@Composable
internal actual fun rememberSettingsStore(): SettingsStore {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val prefs = context.getSharedPreferences("pantry", Context.MODE_PRIVATE)
        object : SettingsStore {
            override var backendUrl: String?
                get() = prefs.getString(KEY_BACKEND_URL, null)
                set(value) {
                    val editor = prefs.edit()
                    if (value == null) editor.remove(KEY_BACKEND_URL) else editor.putString(KEY_BACKEND_URL, value)
                    editor.apply()
                }
        }
    }
}

private const val KEY_BACKEND_URL = "backendUrl"

private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
private const val API_LOCAL_NETWORK_PERMISSION = 37

@Composable
internal actual fun rememberNetworkAccess(backendUrl: String): NetworkAccess {
    if (Build.VERSION.SDK_INT < API_LOCAL_NETWORK_PERMISSION || !isLocalNetworkUrl(backendUrl)) {
        return NetworkAccess.Granted
    }

    val context = LocalContext.current
    val isGranted = {
        ContextCompat.checkSelfPermission(context, ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED
    }
    var granted by remember { mutableStateOf(isGranted()) }
    var answered by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        answered = true
    }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(ACCESS_LOCAL_NETWORK)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = isGranted()
    }

    return NetworkAccess(
        ready = granted || answered,
        missing = !granted,
        request = {
            val activity = context.findActivity()
            val canAsk = activity != null &&
                (!answered || ActivityCompat.shouldShowRequestPermissionRationale(activity, ACCESS_LOCAL_NETWORK))
            if (canAsk) {
                launcher.launch(ACCESS_LOCAL_NETWORK)
            } else {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        },
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
