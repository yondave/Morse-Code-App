package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedGreen
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SophisticatedSecondary
import com.example.ui.theme.SophisticatedSurfaceDark
import com.example.ui.theme.SophisticatedTextMuted

@Composable
fun SignalOscilloscope(
    history: List<Float>,
    threshold: Float,
    isSignalOn: Boolean,
    title: String = "SIGNAL OSCILLOSCOPE",
    signalMetricText: String = "THRESHOLD ${(threshold * 100).toInt()}%",
    signalActiveText: String = if (isSignalOn) "ACTIVE" else "STANDBY",
    accentColor: Color = SophisticatedPrimary,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = if (isSignalOn) SophisticatedGreen else SophisticatedSecondary.copy(alpha = 0.5f),
        label = "statusColor"
    )

    val wavePath = androidx.compose.runtime.remember { Path() }
    val dashEffect = androidx.compose.runtime.remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) }
    val glowStroke = androidx.compose.runtime.remember { Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val lineStroke = androidx.compose.runtime.remember { Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(115.dp)
            .background(SophisticatedSurfaceDark, RoundedCornerShape(14.dp))
            .border(1.dp, SophisticatedBorder, RoundedCornerShape(14.dp))
            .padding(10.dp)
            .testTag("signal_oscilloscope")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Background Tactical Grid Lines
            val gridColor = SophisticatedBorder.copy(alpha = 0.35f)
            for (i in 1..3) {
                val y = height * (i / 4f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }
            for (i in 1..5) {
                val x = width * (i / 6f)
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 0.8f
                )
            }

            // Draw Threshold Line (Dashed Accent)
            val threshY = height * (1f - threshold.coerceIn(0f, 1f))
            drawLine(
                color = accentColor.copy(alpha = 0.85f),
                start = Offset(0f, threshY),
                end = Offset(width, threshY),
                strokeWidth = 1.5f,
                pathEffect = dashEffect
            )

            // Draw Signal Waveform
            if (history.size >= 2) {
                wavePath.reset()
                val stepX = width / (history.size - 1).coerceAtLeast(1)

                history.forEachIndexed { index, value ->
                    val x = index * stepX
                    val y = height * (1f - value.coerceIn(0f, 1f))
                    if (index == 0) {
                        wavePath.moveTo(x, y)
                    } else {
                        wavePath.lineTo(x, y)
                    }
                }

                // Waveform glow and trace
                val waveColor = if (isSignalOn) SophisticatedGreen else accentColor
                drawPath(
                    path = wavePath,
                    color = waveColor.copy(alpha = 0.35f),
                    style = glowStroke
                )
                drawPath(
                    path = wavePath,
                    color = waveColor,
                    style = lineStroke
                )
            }
        }

        // Oscilloscope Top HUD Header
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Status Indicator LED
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$title • $signalActiveText",
                color = if (isSignalOn) SophisticatedGreen else SophisticatedTextMuted,
                fontSize = 9.sp,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = signalMetricText,
                color = accentColor,
                fontSize = 9.sp,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
