package com.natodrill.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ThemeWrapper(content: @Composable () -> Unit) {
    MaterialTheme(
            colorScheme =
                    darkColorScheme(
                            primary = Color(0xFFD32F2F),
                            surface = Color(0xFF121212),
                            onSurface = Color.White
                    )
    ) { content() }
}

@Composable
fun RowScope.TableCell(
        text: String,
        weight: Float,
        isHeader: Boolean = false,
        color: Color = Color.White,
        fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
            text = text,
            modifier = Modifier.weight(weight).padding(4.dp),
            style =
                    if (isHeader) MaterialTheme.typography.labelSmall
                    else MaterialTheme.typography.bodySmall,
            color = if (isHeader) Color.Gray else color,
            fontWeight = if (isHeader) FontWeight.Bold else fontWeight,
            maxLines = 1
    )
}
