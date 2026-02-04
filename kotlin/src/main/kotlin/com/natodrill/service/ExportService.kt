package com.natodrill.service

import com.google.gson.Gson
import com.natodrill.model.Alert
import java.io.File
import java.io.PrintWriter

class ExportService(private val gson: Gson) {

    fun export(directory: String, baseName: String, alerts: List<Alert>) {
        Thread.ofVirtual().start {
            try {
                val dir = prepareDirectory(directory)
                writeJson(dir, baseName, alerts)
                writeCsv(dir, baseName, alerts)
            } catch (e: Exception) {
                println("Export failed: ${e.message}")
            }
        }
    }

    private fun prepareDirectory(path: String): File {
        val dir = File(path)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun writeJson(dir: File, baseName: String, alerts: List<Alert>) {
        val file = File(dir, "$baseName.json")
        file.writeText(gson.toJson(alerts))
        println("Exported JSON to ${file.absolutePath}")
    }

    private fun writeCsv(dir: File, baseName: String, alerts: List<Alert>) {
        val file = File(dir, "$baseName.csv")
        file.printWriter().use { out ->
            writeCsvHeader(out)
            alerts.forEach { writeCsvRow(out, it) }
        }
        println("Exported CSV to ${file.absolutePath}")
    }

    private fun writeCsvHeader(out: PrintWriter) {
        out.println("Hostname,IP,Type,PID,Process,Port,Protocol,Status")
    }

    private fun writeCsvRow(out: PrintWriter, alert: Alert) {
        val status = if (alert.isKilled) "KILLED" else "ACTIVE"
        val row =
                "${alert.hostname},${alert.ipAddress},${alert.type},${alert.pid},${alert.name},${alert.port},${alert.protocol},$status"
        out.println(row)
    }
}
