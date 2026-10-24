package com.lockedapp.vault.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockedapp.vault.ui.theme.AuroraPink
import com.lockedapp.vault.ui.theme.AuroraViolet

/** iOS-style passcode dots: empty rings that fill with a gradient dot as digits are entered. */
@Composable
fun PinDots(pinLength: Int, maxLength: Int = 6, shake: Animatable<Float, AnimationVector1D>? = null) {
    val offset = shake?.value ?: 0f
    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.graphicsLayer { translationX = offset }
    ) {
        repeat(maxLength) { index ->
            val filled = index < pinLength
            val scale by animateFloatAsState(
                targetValue = if (filled) 1f else 0.001f,
                animationSpec = spring(dampingRatio = 0.5f),
                label = "dot-scale"
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(AuroraViolet, AuroraPink)))
                )
            }
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
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally)
            ) {
                row.forEach { digit ->
                    NumKey(label = digit.toString()) { onDigit(digit) }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally)
        ) {
            Box(modifier = Modifier.size(72.dp))
            NumKey(label = "0") { onDigit('0') }
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(if (pressed) 0.88f else 1f, tween(100), label = "bk")
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .scale(scale)
                        .size(28.dp)
                        .clickable(
                            interactionSource = interaction,
                            indication = null
                        ) { onBackspace() }
                )
            }
        }
    }
}

@Composable
private fun NumKey(label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.90f else 1f, tween(100), label = "key-scale")

    Box(
        modifier = Modifier
            .size(72.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
