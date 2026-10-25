package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedOnPrimary
import com.example.ui.theme.SophisticatedOnPrimaryContainer
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SophisticatedPrimaryContainer
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.SophisticatedSurfaceDark
import com.example.ui.theme.SophisticatedTextPrimary
import com.example.ui.theme.SophisticatedTextSecondary
import com.example.ui.viewmodel.ManualKeyerState

@Composable
fun ManualKeyerView(
    keyerState: ManualKeyerState,
    onKeyDown: () -> Unit,
    onKeyUp: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyScale by animateFloatAsState(
        targetValue = if (keyerState.isPressed) 0.92f else 1.0f,
        animationSpec = tween(durationMillis = 50),
        label = "keyScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("manual_keyer_card"),
        colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
        shape = RoundedCornerShape(24.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SophisticatedBorder, SophisticatedBorder)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = SophisticatedPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MANUAL TELEGRAPH KEY",
                        style = MaterialTheme.typography.labelMedium,
                        color = SophisticatedTextSecondary,
                        letterSpacing = 1.sp
                    )
                }

                if (keyerState.decodedText.isNotEmpty() || keyerState.currentSymbols.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("clear_manual_keyer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear",
                            tint = SophisticatedTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Output Display Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SophisticatedSurfaceDark, RoundedCornerShape(12.dp))
                    .border(1.dp, SophisticatedBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "ACTIVE BUFFER: ${keyerState.currentSymbols.ifEmpty { "..." }}",
                        color = SophisticatedPrimary,
                        fontSize = 12.sp,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = keyerState.decodedText.ifEmpty { "Tap or hold key below..." },
                        color = if (keyerState.decodedText.isEmpty()) SophisticatedTextSecondary else SophisticatedTextPrimary,
                        fontSize = 18.sp,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sophisticated Metallic Lavender Telegraph Key Knob
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(keyScale)
                    .shadow(
                        elevation = if (keyerState.isPressed) 6.dp else 16.dp,
                        shape = CircleShape,
                        ambientColor = SophisticatedPrimary,
                        spotColor = SophisticatedPrimary
                    )
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (keyerState.isPressed) {
                                listOf(SophisticatedPrimary, SophisticatedPrimaryContainer, Color(0xFF2B2144))
                            } else {
                                listOf(SophisticatedOnPrimaryContainer, SophisticatedPrimary, SophisticatedPrimaryContainer)
                            }
                        ),
                        shape = CircleShape
                    )
                    .border(4.dp, if (keyerState.isPressed) SophisticatedPrimary else SophisticatedOnPrimaryContainer, CircleShape)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            onKeyDown()
                            waitForUpOrCancellation()
                            onKeyUp()
                        }
                    }
                    .testTag("telegraph_key_paddle"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (keyerState.isPressed) "TX ON" else "TAP / HOLD",
                        color = SophisticatedOnPrimary,
                        fontSize = 13.sp,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "STRAIGHT KEY",
                        color = SophisticatedOnPrimary.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Short tap = Dot ( • )  •  Long press = Dash ( — )",
                color = SophisticatedTextSecondary,
                fontSize = 11.sp,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

