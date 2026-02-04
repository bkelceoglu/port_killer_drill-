package com.natodrill.network

import com.google.gson.Gson
import com.natodrill.model.Alert
import com.natodrill.model.Handshake
import com.natodrill.state.CommanderState
import java.io.PrintWriter
import java.net.Socket
import java.util.Scanner

class ClientHandler(
        private val socket: Socket,
        private val state: CommanderState,
        private val gson: Gson
) {
    private var currentHostname: String? = null

    fun start() {
        Thread.ofVirtual().start {
            try {
                processIncomingMessages()
            } catch (e: Exception) {
                handleConnectionError(e)
            } finally {
                cleanup()
            }
        }
    }

    private fun processIncomingMessages() {
        val scanner = Scanner(socket.getInputStream())
        while (scanner.hasNextLine()) {
            val line = scanner.nextLine()
            handleMessage(line)
        }
    }

    private fun handleMessage(line: String) {
        try {
            val genericMap = gson.fromJson(line, Map::class.java)
            when (genericMap["type"]) {
                "HANDSHAKE" -> handleHandshake(line)
                "NEW_PORT" -> handleAlert(line)
            }
        } catch (e: Exception) {
            println("Error parsing message: ${e.message}")
        }
    }

    private fun handleHandshake(line: String) {
        val handshake = gson.fromJson(line, Handshake::class.java)
        currentHostname = handshake.hostname
        state.registerAgent(handshake.hostname, socket)
        sendWelcomeAcknowledgement()
    }

    private fun sendWelcomeAcknowledgement() {
        try {
            val writer = PrintWriter(socket.getOutputStream(), true)
            writer.println("{\"type\": \"WELCOME\"}")
        } catch (e: Exception) {
            println("Error sending welcome: ${e.message}")
        }
    }

    private fun handleAlert(line: String) {
        val alertInfo = gson.fromJson(line, Alert::class.java)
        val alert = alertInfo.copy(ipAddress = socket.inetAddress.hostAddress)
        state.addAlert(alert)
        handleAutoKill(alert)
    }

    private fun handleAutoKill(alert: Alert) {
        if (state.isAutoKillEnabled.value) {
            Thread.ofVirtual().start {
                try {
                    Thread.sleep(1000)
                    state.killProcess(alert.hostname, alert.pid)
                } catch (e: Exception) {
                    println("Auto-kill failed: ${e.message}")
                }
            }
        }
    }

    private fun handleConnectionError(e: Exception) {
        println("Connection lost with $currentHostname: ${e.message}")
    }

    private fun cleanup() {
        currentHostname?.let { state.unregisterAgent(it) }
        socket.close()
    }
}
