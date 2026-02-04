package com.natodrill.model

data class Alert(
        val type: String,
        val hostname: String,
        val ipAddress: String = "",
        val pid: Int,
        val name: String,
        val port: Int,
        val protocol: String,
        val isKilled: Boolean = false
)
