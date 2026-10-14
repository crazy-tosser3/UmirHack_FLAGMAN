package com.crzto3.pantry

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

private val isMac = System.getProperty("os.name").lowercase().contains("mac")

fun main() = application {
    Window(
        onCloseRequest = {
            DesktopBrowser.dispose()
            exitApplication()
        },
        title = "Pantry",
        state = rememberWindowState(width = 1280.dp, height = 820.dp),
    ) {
        window.minimumSize = java.awt.Dimension(380, 560)
        val controller = rememberAppController()

        MenuBar {
            Menu("Pantry") {
                Item("Обновить", shortcut = KeyShortcut(Key.R, meta = isMac, ctrl = !isMac)) { controller.reload() }
                Item("Адрес сервера…", shortcut = KeyShortcut(Key.Comma, meta = isMac, ctrl = !isMac)) {
                    controller.openSettings()
                }
            }
        }

        App(controller)
    }
}
