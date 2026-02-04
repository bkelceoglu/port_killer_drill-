package com.natodrill.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.natodrill.state.CommanderState
import com.natodrill.ui.components.*

@Composable
fun OverwatchDashboard(state: CommanderState) {
    var showExportDialog by remember { mutableStateOf(false) }

    ThemeWrapper {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                DashboardHeader(onExportClick = { showExportDialog = true })
                Spacer(modifier = Modifier.height(16.dp))
                AgentStatusBar(state.activeAgents.keys.toList())
                Spacer(modifier = Modifier.height(24.dp))
                IntrusionSection(state)
            }
        }

        if (showExportDialog) {
            ExportDialog(
                    onDismiss = { showExportDialog = false },
                    onConfirm = { path, fileName ->
                        state.exportAlerts(path, fileName)
                        showExportDialog = false
                    }
            )
        }
    }
}
