package com.crzto3.pantry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch

private val PantryColors = lightColorScheme(
    primary = Color(0xFF6E6AA8),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFA3A0D3),
    secondaryContainer = Color(0xFFE4E2F1),
    background = Color(0xFFF7F8FF),
    onBackground = Color(0xFF25232D),
    surface = Color(0xFFF7F8FF),
    onSurface = Color(0xFF25232D),
    error = Color(0xFFB91C1C),
)

@Composable
fun App(controller: AppController = rememberAppController()) {
    val scope = rememberCoroutineScope()

    val networkAccess = rememberNetworkAccess(controller.backendUrl)

    LaunchedEffect(controller.connectAttempt, networkAccess.ready, networkAccess.missing) {
        if (networkAccess.ready) controller.connect()
    }
    DisposableEffect(controller) { onDispose { controller.shutdown() } }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        scope.launch { controller.ensureServerAlive() }
    }

    MaterialTheme(colorScheme = PantryColors) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (val screen = controller.screen) {
                is Screen.Web -> WebScreen(screen.url, controller.reloadKey)
                Screen.Connecting -> Centered {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Подключение к ${controller.backendUrl}…", textAlign = TextAlign.Center)
                }
                is Screen.Error -> ErrorScreen(
                    message = screen.message,
                    networkAccess = networkAccess,
                    onRetry = controller::retry,
                    onSettings = controller::openSettings,
                )
                Screen.Settings -> SettingsScreen(controller)
            }

            val telegramRequest by controller.telegram.request.collectAsState()
            telegramRequest?.let { request ->
                key(request) {
                    TelegramAuthHost(request) { payload -> controller.telegram.finish(request, payload) }
                }
            }
        }
    }
}

private val WebHeaderColor = Color(0xFFF7F8FF)
private val WebPageColor = Color(0xFFEDEEF5)

@Composable
private fun WebScreen(url: String, reloadKey: Int) {
    val insets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    Box(Modifier.fillMaxSize().background(WebPageColor)) {
        Box(Modifier.fillMaxWidth().windowInsetsTopHeight(insets).background(WebHeaderColor))
        PlatformWebView(
            url = url,
            reloadKey = reloadKey,
            modifier = Modifier.fillMaxSize().windowInsetsPadding(insets),
        )
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content() }
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    networkAccess: NetworkAccess,
    onRetry: () -> Unit,
    onSettings: () -> Unit,
) = Centered {
    Text("Нет связи с сервером", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(12.dp))
    Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
    if (networkAccess.missing) {
        Spacer(Modifier.height(16.dp))
        Text(
            "Сервер находится в локальной сети, а у приложения нет разрешения на доступ к ней.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = networkAccess.request) { Text("Разрешить доступ к локальной сети") }
    }
    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onSettings) { Text("Адрес сервера") }
        Button(onClick = onRetry) { Text("Повторить") }
    }
}

@Composable
private fun SettingsScreen(controller: AppController) = Centered {
    var value by remember { mutableStateOf(controller.backendUrl) }
    var invalid by remember { mutableStateOf(false) }
    val save = { invalid = !controller.saveBackendUrl(value) }

    Text("Адрес сервера", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    Text(
        "Адрес бэкенда Pantry, например https://pantry.example.com или http://192.168.1.10:8080",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(20.dp))
    OutlinedTextField(
        value = value,
        onValueChange = { value = it; invalid = false },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = invalid,
        supportingText = if (invalid) ({ Text("Укажите адрес вида http(s)://хост[:порт]") }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { save() }),
    )
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = { value = controller.defaultBackendUrl; invalid = false }) { Text("По умолчанию") }
        OutlinedButton(onClick = controller::retry) { Text("Отмена") }
        Button(onClick = save) { Text("Сохранить") }
    }
}
