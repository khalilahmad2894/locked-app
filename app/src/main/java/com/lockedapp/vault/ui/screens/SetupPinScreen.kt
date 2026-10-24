package com.lockedapp.vault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lockedapp.vault.ui.components.NumPad
import com.lockedapp.vault.ui.components.PinDots

private const val PIN_LENGTH = 6

@Composable
fun SetupPinScreen(onPinCreated: (String) -> Unit) {
    var firstPin by remember { mutableStateOf<String?>(null) }
    var currentInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val isConfirmStep = firstPin != null
    val title = if (isConfirmStep) "Confirm your PIN" else "Create a 6-digit PIN"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (error != null) {
            Text(
                error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Column(
            modifier = Modifier.padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PinDots(pinLength = currentInput.length, maxLength = PIN_LENGTH)
        }

        NumPad(
            onDigit = { digit ->
                if (currentInput.length < PIN_LENGTH) {
                    currentInput += digit
                    error = null
                }
                if (currentInput.length == PIN_LENGTH) {
                    if (!isConfirmStep) {
                        firstPin = currentInput
                        currentInput = ""
                    } else {
                        if (currentInput == firstPin) {
                            onPinCreated(currentInput)
                        } else {
                            error = "PINs don't match, try again"
                            firstPin = null
                            currentInput = ""
                        }
                    }
                }
            },
            onBackspace = {
                if (currentInput.isNotEmpty()) currentInput = currentInput.dropLast(1)
            },
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}
