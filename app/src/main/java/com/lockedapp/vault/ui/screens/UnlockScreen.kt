package com.lockedapp.vault.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
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
import com.lockedapp.vault.crypto.PinManager
import com.lockedapp.vault.ui.components.NumPad
import com.lockedapp.vault.ui.components.PinDots
import com.lockedapp.vault.ui.theme.AuroraViolet
import com.lockedapp.vault.ui.theme.DangerRed
import com.lockedapp.vault.ui.theme.auroraBrush
import kotlinx.coroutines.launch

private const val PIN_LENGTH = 6

@Composable
fun UnlockScreen(
    pinManager: PinManager,
    onUnlocked: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

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
            Icons.Filled.LockOpen,
            contentDescription = null,
            tint = AuroraViolet,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text("Enter Passcode", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)

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
            PinDots(pinLength = input.length, maxLength = PIN_LENGTH, shake = shake)
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
                        error = "Wrong passcode"
                        scope.launch { playShake(shake) }
                        input = ""
                    }
                }
            },
            onBackspace = {
                if (input.isNotEmpty()) input = input.dropLast(1)
            },
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
