package com.aimatrix.amxlsp.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import com.aimatrix.amxlsp.desktop.ui.AmxLspApp
import com.aimatrix.amxlsp.desktop.ui.theme.AmxLspTheme

fun main() = application {
    val windowState = rememberWindowState(
        width = 1200.dp,
        height = 800.dp
    )
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "Aimatrix Master Agent",
        state = windowState
    ) {
        AmxLspTheme {
            AmxLspApp()
        }
    }
}