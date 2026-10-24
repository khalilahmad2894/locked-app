package com.lockedapp.vault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.lockedapp.vault.biometric.BiometricHelper
import com.lockedapp.vault.crypto.PinManager
import com.lockedapp.vault.ui.components.NumPad
import com.lockedapp.vault.ui.components.PinDots

private const val PIN_LENGTH = 6

@Composable
fun UnlockScreen(
    activity: FragmentActivity,
    pinManager: PinManager,
    onUnlocked: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val biometricEnabled = pinManager.isBiometricEnabled() && BiometricHelper.isAvailable(activity)

    fun triggerBiometric() {
        BiometricHelper.authenticate(
            activity,
            onSuccess = onUnlocked,
            onError = { /* user can still type PIN */ }
        )
    }

    LaunchedEffect(Unit) {
        if (biometricEnabled) triggerBiometric()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Enter PIN", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
            PinDots(pinLength = input.length, maxLength = PIN_LENGTH)
        }

        NumPad(
            onDigit = { digit ->
                if (input.length < PIN_LENGTH) {
                    input += digit
                }
                if (input.length == PIN_LENGTH) {
                    if (pinManager.verifyPin(input)) {
                        onUnlocked()
                    } else {
                        error = "Wrong PIN, try again"
                        input = ""
                    }
                }
            },
            onBackspace = {
                if (input.isNotEmpty()) input = input.dropLast(1)
            },
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        if (biometricEnabled) {
            ClickableText(
                text = AnnotatedString("Use Face / Fingerprint Unlock"),
                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(top = 24.dp),
                onClick = { triggerBiometric() }
            )
        }
    }
}
