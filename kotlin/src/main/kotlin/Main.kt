package com.natodrill

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.google.gson.GsonBuilder
import com.natodrill.network.CommanderServer
import com.natodrill.service.ExportService
import com.natodrill.state.CommanderState
import com.natodrill.ui.OverwatchDashboard

fun main() = application {
    // 1. Core Dependencies
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }
    val exportService = remember { ExportService(gson) }

    // 2. Application State
    val state = remember { CommanderState(exportService) }

    // 3. Network Infrastructure (9090)
    val server = remember { CommanderServer(9090, state, gson) }

    // 4. Lifecycle: Start Server
    LaunchedEffect(Unit) { server.start() }

    // 5. Tactical UI
    Window(
            onCloseRequest = { exitApplication() },
            title = "::NATO LOCKED SHIELD ** PORT WATCHDOG::",
            state =
                    androidx.compose.ui.window.rememberWindowState(
                            placement = androidx.compose.ui.window.WindowPlacement.Maximized
                    )
    ) { OverwatchDashboard(state) }
}
