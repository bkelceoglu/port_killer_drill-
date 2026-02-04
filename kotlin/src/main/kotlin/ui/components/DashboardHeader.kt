package com.natodrill.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DashboardHeader(onExportClick: () -> Unit) {
    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
                text = "::NATO LOCKED SHIELD ** PORT WATCHDOG::",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
        )

        Button(
                onClick = onExportClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                shape = RoundedCornerShape(4.dp)
        ) { Text("EXPORT LOGS", style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable
fun AgentStatusBar(hostnames: List<String>) {
    Column {
        Text("ACTIVE AGENTS", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        if (hostnames.isEmpty()) {
            Text(
                    "WAITING FOR AGENTS...",
                    color = Color.Yellow,
                    style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
            ) { items(hostnames) { hostname -> AgentStatusBox(hostname) } }
        }
    }
}

@Composable
fun AgentStatusBox(hostname: String) {
    Box(
            modifier =
                    Modifier.border(1.dp, Color.Green, RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E3A1E), RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(Color.Green, RoundedCornerShape(4.dp)))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                    text = hostname.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Green,
                    fontWeight = FontWeight.Bold
            )
        }
    }
}
