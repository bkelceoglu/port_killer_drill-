package com.natodrill.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.natodrill.model.Alert
import com.natodrill.state.CommanderState

@Composable
fun IntrusionSection(state: CommanderState) {
    Column {
        IntrusionHeader(
                isAutoKillEnabled = state.isAutoKillEnabled.value,
                onAutoKillChange = { state.isAutoKillEnabled.value = it }
        )
        Spacer(modifier = Modifier.height(8.dp))
        AlertTable(state.alerts) { alert -> state.killProcess(alert.hostname, alert.pid) }
    }
}

@Composable
private fun IntrusionHeader(isAutoKillEnabled: Boolean, onAutoKillChange: (Boolean) -> Unit) {
    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Intrusion Alerts", style = MaterialTheme.typography.titleMedium)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                    text = "Auto Kill",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isAutoKillEnabled) Color.Red else Color.Gray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                    checked = isAutoKillEnabled,
                    onCheckedChange = onAutoKillChange,
                    colors =
                            SwitchDefaults.colors(
                                    checkedThumbColor = Color.Red,
                                    checkedTrackColor = Color.Red.copy(alpha = 0.5f)
                            )
            )
        }
    }
}

@Composable
fun AlertTable(alerts: List<Alert>, onNeutralize: (Alert) -> Unit) {
    val state = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize()) {
        AlertTableHeader()

        if (alerts.isEmpty()) {
            EmptyAlertsPlaceholder()
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(end = 12.dp), state = state) {
                    items(alerts) { alert ->
                        AlertRow(alert, onNeutralize)
                        HorizontalDivider(color = Color(0xFF333333))
                    }
                }
                AlertTableScrollbar(state)
            }
        }
    }
}

@Composable
private fun AlertTableHeader() {
    Surface(color = Color(0xFF242424), shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            TableCell("ORIGIN", weight = 0.12f, isHeader = true)
            TableCell("IP", weight = 0.13f, isHeader = true)
            TableCell("TYPE", weight = 0.12f, isHeader = true)
            TableCell("PID", weight = 0.08f, isHeader = true)
            TableCell("PROCESS", weight = 0.2f, isHeader = true)
            TableCell("PORT", weight = 0.08f, isHeader = true)
            TableCell("PROTO", weight = 0.1f, isHeader = true)
            TableCell("ACTION", weight = 0.17f, isHeader = true)
        }
    }
}

@Composable
private fun EmptyAlertsPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("NO INTRUSIONS DETECTED ... WELL YET :) ", color = Color.Gray)
    }
}

@Composable
private fun BoxScope.AlertTableScrollbar(state: LazyListState) {
    androidx.compose.foundation.VerticalScrollbar(
            adapter = androidx.compose.foundation.rememberScrollbarAdapter(state),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            style =
                    androidx.compose.foundation.ScrollbarStyle(
                            minimalHeight = 16.dp,
                            thickness = 8.dp,
                            shape = RoundedCornerShape(4.dp),
                            hoverDurationMillis = 300,
                            unhoverColor = Color.LightGray.copy(alpha = 0.5f),
                            hoverColor = Color.LightGray
                    )
    )
}

@Composable
fun AlertRow(alert: Alert, onNeutralize: (Alert) -> Unit) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp).background(Color.Transparent),
            verticalAlignment = Alignment.CenterVertically
    ) {
        TableCell(alert.hostname, weight = 0.12f, color = Color.Cyan)
        TableCell(alert.ipAddress, weight = 0.13f, color = Color.Gray)
        TableCell(alert.type, weight = 0.12f, color = Color.Red)
        TableCell(alert.pid.toString(), weight = 0.08f)
        TableCell(alert.name, weight = 0.2f, fontWeight = FontWeight.Bold)
        TableCell(alert.port.toString(), weight = 0.08f)
        TableCell(alert.protocol, weight = 0.1f)

        Box(modifier = Modifier.weight(0.17f), contentAlignment = Alignment.Center) {
            NeutralizeButton(alert, onNeutralize)
        }
    }
}

@Composable
private fun NeutralizeButton(alert: Alert, onNeutralize: (Alert) -> Unit) {
    val buttonLabel = if (alert.isKilled) "KILLED" else "NEUTRALIZE"
    Button(
            onClick = { onNeutralize(alert) },
            enabled = !alert.isKilled,
            colors =
                    ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            disabledContainerColor = Color(0xFF2E7D32)
                    ),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Text(
                buttonLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
        )
    }
}
