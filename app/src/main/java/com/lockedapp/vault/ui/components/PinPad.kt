package com.lockedapp.vault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun PinDots(pinLength: Int, maxLength: Int = 6) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(maxLength) { index ->
            val filled = index < pinLength
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (filled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

@Composable
fun NumPad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9')
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { digit ->
                    NumKey(label = digit.toString(), modifier = Modifier.weight(1f)) { onDigit(digit) }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f))
            NumKey(label = "0", modifier = Modifier.weight(1f)) { onDigit('0') }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .clickable { onBackspace() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace")
            }
        }
    }
}

@Composable
private fun NumKey(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.headlineMedium)
    }
}
