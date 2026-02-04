package com.natodrill.state

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.natodrill.model.Alert
import com.natodrill.service.ExportService
import java.io.PrintWriter
import java.net.Socket

class CommanderState(private val exportService: ExportService) {
    val alerts = mutableStateListOf<Alert>()
    val isAutoKillEnabled = mutableStateOf(false)
    val activeAgents: SnapshotStateMap<String, Socket> = mutableStateMapOf()

    fun killProcess(hostname: String, pid: Int) {
        val socket = activeAgents[hostname] ?: return
        sendKillCommand(socket, hostname, pid)
        markAlertAsKilled(hostname, pid)
    }

    private fun sendKillCommand(socket: Socket, hostname: String, pid: Int) {
        Thread.ofVirtual().start {
            try {
                val writer = PrintWriter(socket.getOutputStream(), true)
                writer.println("{\"kill_pid\": $pid}")
            } catch (e: Exception) {
                println("Error sending kill command to $hostname: ${e.message}")
            }
        }
    }

    private fun markAlertAsKilled(hostname: String, pid: Int) {
        for (i in alerts.indices) {
            val alert = alerts[i]
            if (alert.hostname == hostname && alert.pid == pid) {
                alerts[i] = alert.copy(isKilled = true)
            }
        }
    }

    fun exportAlerts(directory: String, baseName: String) {
        exportService.export(directory, baseName, alerts.toList())
    }

    fun addAlert(alert: Alert) {
        alerts.add(0, alert)
    }

    fun registerAgent(hostname: String, socket: Socket) {
        activeAgents[hostname] = socket
        println("Agent registered: $hostname")
    }

    fun unregisterAgent(hostname: String) {
        activeAgents.remove(hostname)
        println("Agent disconnected: $hostname")
    }
}
