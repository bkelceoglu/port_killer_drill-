package com.natodrill.network

import com.google.gson.Gson
import com.natodrill.state.CommanderState
import java.net.InetSocketAddress
import java.net.ServerSocket

class CommanderServer(
        private val port: Int,
        private val state: CommanderState,
        private val gson: Gson
) {
    fun start() {
        Thread.ofVirtual().start { serverLoop() }
    }

    private fun serverLoop() {
        while (true) {
            try {
                runServerInstance()
            } catch (e: Exception) {
                handleServerError(e)
            }
        }
    }

    private fun runServerInstance() {
        val server = ServerSocket()
        server.reuseAddress = true
        server.bind(InetSocketAddress(port))
        println("Commander listening on port $port (modular)...")

        while (true) {
            val socket = server.accept()
            ClientHandler(socket, state, gson).start()
        }
    }

    private fun handleServerError(e: Exception) {
        println("Server error: ${e.message}. Retrying in 2s...")
        Thread.sleep(2000)
    }
}
