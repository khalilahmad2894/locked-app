package com.lockedapp.vault.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockedapp.vault.ui.components.NumPad
import com.lockedapp.vault.ui.components.PinDots
import com.lockedapp.vault.ui.theme.AuroraViolet
import com.lockedapp.vault.ui.theme.DangerRed
import com.lockedapp.vault.ui.theme.auroraBrush
import kotlinx.coroutines.launch

private const val PIN_LENGTH = 6

@Composable
fun SetupPinScreen(onPinCreated: (String) -> Unit) {
    var firstPin by remember { mutableStateOf<String?>(null) }
    var currentInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val isConfirmStep = firstPin != null
    val title = if (isConfirmStep) "Confirm Passcode" else "Create a Passcode"
    val subtitle = if (isConfirmStep) "Enter it one more time" else "This will protect your vault"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(auroraBrush())
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            tint = AuroraViolet,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        AnimatedContent(targetState = title, label = "title") { t ->
            Text(t, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            subtitle,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 6.dp)
        )

        AnimatedContent(
            targetState = error,
            label = "error",
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { err ->
            Text(
                err ?: " ",
                color = DangerRed,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Column(
            modifier = Modifier.padding(vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PinDots(pinLength = currentInput.length, maxLength = PIN_LENGTH, shake = shake)
        }

        NumPad(
            onDigit = { digit ->
                if (currentInput.length < PIN_LENGTH) {
                    currentInput += digit
                }
                if (currentInput.length == PIN_LENGTH) {
                    if (!isConfirmStep) {
                        firstPin = currentInput
                        currentInput = ""
                        error = null
                    } else if (currentInput == firstPin) {
                        onPinCreated(currentInput)
                    } else {
                        error = "Passcodes didn't match"
                        scope.launch { playShake(shake) }
                        firstPin = null
                        currentInput = ""
                    }
                }
            },
            onBackspace = {
                if (currentInput.isNotEmpty()) currentInput = currentInput.dropLast(1)
            },
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

internal suspend fun playShake(shake: Animatable<Float, AnimationVector1D>) {
    shake.animateTo(0f, tween(0))
    listOf(-18f, 18f, -12f, 12f, -6f, 0f).forEach { target ->
        shake.animateTo(target, tween(45))
    }
}
