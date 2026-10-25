package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedOnPrimary
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.SophisticatedTextSecondary

@Composable
fun VisualBeacon(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.12f else 1.0f,
        animationSpec = tween(durationMillis = 80),
        label = "beaconScale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isActive) SophisticatedPrimary else SophisticatedSurface,
        animationSpec = tween(durationMillis = 80),
        label = "beaconBg"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isActive) SophisticatedOnPrimary else SophisticatedTextSecondary,
        animationSpec = tween(durationMillis = 80),
        label = "beaconIcon"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(
                elevation = if (isActive) 24.dp else 2.dp,
                shape = CircleShape,
                ambientColor = if (isActive) SophisticatedPrimary else Color.Transparent,
                spotColor = if (isActive) SophisticatedPrimary else Color.Transparent
            )
            .background(bgColor, CircleShape)
            .border(2.dp, if (isActive) SophisticatedPrimary else SophisticatedBorder, CircleShape)
            .testTag("visual_signal_beacon"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.FlashOn,
            contentDescription = "Signal Flash",
            tint = iconColor,
            modifier = Modifier.size(size / 2)
        )
    }
}

