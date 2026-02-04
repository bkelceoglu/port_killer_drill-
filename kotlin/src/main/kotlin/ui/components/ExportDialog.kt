package com.natodrill.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import javax.swing.JFileChooser

@Composable
fun ExportDialog(onDismiss: () -> Unit, onConfirm: (path: String, filename: String) -> Unit) {
    var fileName by remember { mutableStateOf("") }
    var path by remember { mutableStateOf(System.getProperty("user.dir") ?: "") }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Export Alert History") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    FileNameInput(fileName) { fileName = it }
                    SaveLocationInput(path) { path = it }
                }
            },
            confirmButton = {
                Button(
                        onClick = {
                            if (fileName.isNotBlank() && path.isNotBlank()) {
                                onConfirm(path, fileName)
                            }
                        }
                ) { Text("Export Now") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
            containerColor = Color(0xFF242424),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
    )
}

@Composable
private fun FileNameInput(value: String, onValueChange: (String) -> Unit) {
    Column {
        Text("Filename:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("e.g., drill_day_1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SaveLocationInput(path: String, onPathChange: (String) -> Unit) {
    Column {
        Text("Save Location:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                    value = path,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.width(8.dp))
            BrowseButton { pickDirectory(path)?.let { onPathChange(it) } }
        }
    }
}

@Composable
private fun BrowseButton(onClick: () -> Unit) {
    Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
    ) { Text("Browse", style = MaterialTheme.typography.labelSmall) }
}

private fun pickDirectory(initialPath: String): String? {
    val chooser = JFileChooser(initialPath)
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    chooser.dialogTitle = "Select Destination Folder"
    val result = chooser.showOpenDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile.absolutePath
    } else null
}
