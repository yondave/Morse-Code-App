package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CameraPreviewView
import com.example.ui.components.SignalOscilloscope
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedGreen
import com.example.ui.theme.SophisticatedOnPrimary
import com.example.ui.theme.SophisticatedOnPrimaryContainer
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SophisticatedPrimaryContainer
import com.example.ui.theme.SophisticatedSecondary
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.SophisticatedSurfaceDark
import com.example.ui.theme.SophisticatedTextMuted
import com.example.ui.theme.SophisticatedTextPrimary
import com.example.ui.theme.SophisticatedTextSecondary
import com.example.ui.viewmodel.MorseViewModel
import com.example.ui.viewmodel.ReceiverMode

@Composable
fun ReceiveScreen(
    viewModel: MorseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val receiverMode by viewModel.receiverMode.collectAsStateWithLifecycle()
    val opticalState by viewModel.opticalState.collectAsStateWithLifecycle()
    val opticalZoom by viewModel.opticalZoom.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val transmitWpm by viewModel.wpm.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Camera Permission State
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Audio Permission State
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted && receiverMode == ReceiverMode.ACOUSTIC) {
            viewModel.startAudioListening()
        }
    }

    // Auto-manage audio listener when in acoustic mode
    DisposableEffect(receiverMode, hasAudioPermission) {
        if (receiverMode == ReceiverMode.ACOUSTIC && hasAudioPermission) {
            viewModel.startAudioListening()
        }
        onDispose {
            if (receiverMode == ReceiverMode.ACOUSTIC) {
                viewModel.stopAudioListening()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dual-Mode Tactical Receiver Selector
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("receiver_mode_card"),
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SophisticatedBorder, SophisticatedBorder)))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Optical Flashlight Mode Chip
                FilterChip(
                    selected = receiverMode == ReceiverMode.OPTICAL,
                    onClick = { viewModel.setReceiverMode(ReceiverMode.OPTICAL) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "OPTICAL CAMERA",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_chip_optical"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SophisticatedPrimary,
                        selectedLabelColor = SophisticatedOnPrimary,
                        selectedLeadingIconColor = SophisticatedOnPrimary,
                        containerColor = SophisticatedSurfaceDark,
                        labelColor = SophisticatedTextSecondary,
                        iconColor = SophisticatedTextSecondary
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                // Acoustic Audio Mode Chip
                FilterChip(
                    selected = receiverMode == ReceiverMode.ACOUSTIC,
                    onClick = {
                        viewModel.setReceiverMode(ReceiverMode.ACOUSTIC)
                        if (hasAudioPermission) {
                            viewModel.startAudioListening()
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "ACOUSTIC MIC",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_chip_acoustic"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SophisticatedPrimary,
                        selectedLabelColor = SophisticatedOnPrimary,
                        selectedLeadingIconColor = SophisticatedOnPrimary,
                        containerColor = SophisticatedSurfaceDark,
                        labelColor = SophisticatedTextSecondary,
                        iconColor = SophisticatedTextSecondary
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =========================================================================
        // MODE 1: OPTICAL CAMERA RECEIVER
        // =========================================================================
        if (receiverMode == ReceiverMode.OPTICAL) {
            if (!hasCameraPermission) {
                // Camera Permission Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("camera_permission_card"),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                    shape = RoundedCornerShape(24.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SophisticatedBorder, SophisticatedBorder)))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Camera Access Required",
                            style = MaterialTheme.typography.titleMedium,
                            color = SophisticatedTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To receive and decode optical Morse code from other flashlights, point your camera at the light source.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SophisticatedTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier.testTag("grant_camera_permission_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SophisticatedPrimary, contentColor = SophisticatedOnPrimary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("ENABLE OPTICAL RECEIVER")
                        }
                    }
                }
            } else {
                // Live Viewfinder Card with Tap-to-Aim Reticle
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("camera_viewfinder_card"),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                    shape = RoundedCornerShape(24.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SophisticatedBorder, SophisticatedBorder)))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (opticalState.isLightDetected) SophisticatedGreen else SophisticatedPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (opticalState.isLightDetected) "LIGHT FLASH DETECTED" else "TAP SCREEN TO AIM RETICLE",
                                    color = if (opticalState.isLightDetected) SophisticatedGreen else SophisticatedPrimary,
                                    fontSize = 11.sp,
                                    style = MaterialTheme.typography.labelSmall,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (opticalState.isFixedWpm) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Fixed Speed",
                                        tint = SophisticatedPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "FIXED ${opticalState.fixedWpm} WPM",
                                        color = SophisticatedPrimary,
                                        fontSize = 11.sp,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                } else {
                                    Text(
                                        text = "AUTO ${opticalState.detectedWpm} WPM",
                                        color = SophisticatedSecondary,
                                        fontSize = 11.sp,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Camera Viewport with Dynamic Reticle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SophisticatedSurfaceDark)
                        ) {
                            CameraPreviewView(
                                opticalDecoder = viewModel.opticalDecoder,
                                isLightDetected = opticalState.isLightDetected,
                                roiX = opticalState.roiX,
                                roiY = opticalState.roiY,
                                isExposureLocked = opticalState.isExposureLocked,
                                zoomLinear = opticalZoom,
                                onRoiTapped = { x, y -> viewModel.setOpticalRoi(x, y) },
                                onResetRoi = { viewModel.resetOpticalRoi() }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Zoom Slider & Exposure Lock Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Camera Zoom",
                                tint = SophisticatedPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ZOOM ${(1f + opticalZoom * 4f).toInt()}x",
                                color = SophisticatedTextSecondary,
                                fontSize = 10.sp,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = opticalZoom,
                                onValueChange = { viewModel.setOpticalZoom(it) },
                                valueRange = 0f..1f,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("camera_zoom_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = SophisticatedPrimary,
                                    activeTrackColor = SophisticatedPrimary,
                                    inactiveTrackColor = SophisticatedSurfaceDark
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { viewModel.toggleOpticalExposureLock() },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        if (opticalState.isExposureLocked) SophisticatedPrimary else SophisticatedSurfaceDark,
                                        CircleShape
                                    )
                                    .testTag("exposure_lock_button")
                            ) {
                                Icon(
                                    imageVector = if (opticalState.isExposureLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Exposure Lock",
                                    tint = if (opticalState.isExposureLocked) SophisticatedOnPrimary else SophisticatedTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Quick Zoom Preset Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val zoomPresets = listOf(0f to "1.0x", 0.25f to "2.0x", 0.5f to "3.0x", 1.0f to "5.0x")
                            zoomPresets.forEach { (fraction, label) ->
                                val isSelected = kotlin.math.abs(opticalZoom - fraction) < 0.05f
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setOpticalZoom(fraction) },
                                    label = { Text(label, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SophisticatedPrimaryContainer,
                                        selectedLabelColor = SophisticatedOnPrimaryContainer,
                                        containerColor = SophisticatedSurfaceDark,
                                        labelColor = SophisticatedTextSecondary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Real-Time Optical Oscilloscope Waveform
                SignalOscilloscope(
                    history = opticalState.luminanceHistory,
                    threshold = opticalState.thresholdLuminance,
                    isSignalOn = opticalState.isLightDetected,
                    title = "OPTICAL LUMINANCE OSCILLOSCOPE",
                    signalMetricText = "THRESHOLD ${(opticalState.thresholdLuminance * 100).toInt()}%"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Optical Decoded Message Box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("decoded_message_card"),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                    shape = RoundedCornerShape(24.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SophisticatedBorder, SophisticatedBorder)))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OPTICAL DECODED MESSAGE",
                                color = SophisticatedPrimary,
                                fontSize = 11.sp,
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.sp
                            )

                            Row {
                                if (opticalState.decodedText.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.speakText(opticalState.decodedText) },
                                        modifier = Modifier.size(32.dp).testTag("speak_decoded_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Speak Text",
                                            tint = SophisticatedSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.copyToClipboard(opticalState.decodedText, "Decoded Morse") },
                                        modifier = Modifier.size(32.dp).testTag("copy_decoded_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Text",
                                            tint = SophisticatedTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.saveDecodedMessage() },
                                        modifier = Modifier.size(32.dp).testTag("save_decoded_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BookmarkAdd,
                                            contentDescription = "Save to Log",
                                            tint = SophisticatedGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.clearDecodedOpticalText() },
                                    modifier = Modifier.size(32.dp).testTag("clear_decoded_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = SophisticatedTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SophisticatedSurfaceDark, RoundedCornerShape(12.dp))
                                .border(1.dp, SophisticatedBorder, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = opticalState.decodedText.ifEmpty { "Aim reticle at flashing light source..." },
                                    color = if (opticalState.decodedText.isEmpty()) SophisticatedTextMuted else SophisticatedTextPrimary,
                                    fontSize = 18.sp,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Current Symbol: ",
                                        color = SophisticatedTextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = opticalState.currentLetterSymbols.ifEmpty { "(monitoring)" },
                                        color = SophisticatedPrimary,
                                        fontSize = 13.sp,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                if (opticalState.rawMorseStream.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Raw Stream: ${opticalState.rawMorseStream}",
                                        color = SophisticatedSecondary.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Receiver Speed Tuning Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("decoder_speed_card"),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                    shape = RoundedCornerShape(24.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SophisticatedBorder, SophisticatedBorder)))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = SophisticatedPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RECEIVER DECODING SPEED",
                                    color = SophisticatedTextSecondary,
                                    fontSize = 11.sp,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Text(
                                text = if (opticalState.isFixedWpm) "${opticalState.fixedWpm} WPM (Fixed)" else "Auto-Adaptive",
                                color = if (opticalState.isFixedWpm) SophisticatedPrimary else SophisticatedSecondary,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Slider(
                            value = opticalState.fixedWpm.toFloat(),
                            onValueChange = {
                                viewModel.setReceiverFixedWpmEnabled(true)
                                viewModel.setReceiverWpm(it.toInt())
                            },
                            valueRange = 4f..35f,
                            steps = 30,
                            modifier = Modifier.testTag("receiver_wpm_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = SophisticatedPrimary,
                                activeTrackColor = SophisticatedPrimary,
                                inactiveTrackColor = SophisticatedSurfaceDark
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.matchTransmitterWpm() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("match_transmitter_wpm_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SophisticatedSurfaceDark,
                                contentColor = SophisticatedPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(SophisticatedBorder, SophisticatedBorder)))
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Match Transmit Speed ($transmitWpm WPM)", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // MODE 2: ACOUSTIC AUDIO MIC RECEIVER
        // =========================================================================
        if (receiverMode == ReceiverMode.ACOUSTIC) {
            if (!hasAudioPermission) {
                // Audio Permission Request Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("audio_permission_card"),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                    shape = RoundedCornerShape(24.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SophisticatedBorder, SophisticatedBorder)))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = "Microphone",
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Microphone Access Required",
                            style = MaterialTheme.typography.titleMedium,
                            color = SophisticatedTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To listen for Morse code beeps, radio transmissions, or acoustic audio tones, Signal Morse needs access to the microphone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SophisticatedTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            modifier = Modifier.testTag("grant_audio_permission_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SophisticatedPrimary, contentColor = SophisticatedOnPrimary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("ENABLE ACOUSTIC RECEIVER")
                        }
                    }
                }
            } else {
                // Acoustic Tuning & Status Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("acoustic_status_card"),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                    shape = RoundedCornerShape(24.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SophisticatedBorder, SophisticatedBorder)))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (audioState.isToneDetected) SophisticatedGreen else SophisticatedPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (audioState.isToneDetected) "ACOUSTIC TONE DETECTED" else "LISTENING FOR AUDIO TONE",
                                    color = if (audioState.isToneDetected) SophisticatedGreen else SophisticatedPrimary,
                                    fontSize = 11.sp,
                                    style = MaterialTheme.typography.labelSmall,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // Auto-Tune Button
                            Button(
                                onClick = { viewModel.autoTuneAudioPitch() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SophisticatedPrimaryContainer,
                                    contentColor = SophisticatedOnPrimaryContainer
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("audio_autotune_button")
                            ) {
                                if (audioState.isAutoTuneActive) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = SophisticatedPrimary)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Auto-Tune", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Target Tone Frequency Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Target Tone Pitch",
                                color = SophisticatedTextPrimary,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${audioState.targetPitchHz} Hz",
                                color = SophisticatedPrimary,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Slider(
                            value = audioState.targetPitchHz.toFloat(),
                            onValueChange = { viewModel.setAudioPitch(it.toInt()) },
                            valueRange = 400f..1200f,
                            steps = 32,
                            modifier = Modifier.testTag("audio_pitch_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = SophisticatedPrimary,
                                activeTrackColor = SophisticatedPrimary,
                                inactiveTrackColor = SophisticatedSurfaceDark
                            )
                        )

                        // Quick Pitch Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val pitchPresets = listOf(550, 750, 800, 1000)
                            pitchPresets.forEach { pitch ->
                                val isSelected = audioState.targetPitchHz == pitch
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setAudioPitch(pitch) },
                                    label = {
                                        Text(
                                            text = if (pitch == 750) "750 Hz (Ham ★)" else "$pitch Hz",
                                            fontSize = 10.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SophisticatedPrimaryContainer,
                                        selectedLabelColor = SophisticatedOnPrimaryContainer,
                                        containerColor = SophisticatedSurfaceDark,
                                        labelColor = SophisticatedTextSecondary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Squelch / Noise Gate Threshold Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Squelch Gate (Noise Filter)",
                                color = SophisticatedTextPrimary,
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${(audioState.squelchThreshold * 100).toInt()}% • ${audioState.audioLevelDb.toInt()} dB",
                                color = SophisticatedSecondary,
                                fontSize = 12.sp,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        Slider(
                            value = audioState.squelchThreshold,
                            onValueChange = { viewModel.setAudioSquelch(it) },
                            valueRange = 0.10f..0.80f,
                            modifier = Modifier.testTag("audio_squelch_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = SophisticatedSecondary,
                                activeTrackColor = SophisticatedSecondary,
                                inactiveTrackColor = SophisticatedSurfaceDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Real-Time Acoustic Oscilloscope Waveform
                SignalOscilloscope(
                    history = audioState.waveformHistory,
                    threshold = audioState.squelchThreshold,
                    isSignalOn = audioState.isToneDetected,
                    title = "ACOUSTIC SPECTRUM OSCILLOSCOPE",
                    signalMetricText = "SQUELCH ${(audioState.squelchThreshold * 100).toInt()}% • ${audioState.targetPitchHz} Hz"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Acoustic Decoded Message Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("acoustic_decoded_card"),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                    shape = RoundedCornerShape(24.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SophisticatedBorder, SophisticatedBorder)))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ACOUSTIC DECODED MESSAGE",
                                color = SophisticatedPrimary,
                                fontSize = 11.sp,
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.sp
                            )

                            Row {
                                if (audioState.decodedText.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.speakText(audioState.decodedText) },
                                        modifier = Modifier.size(32.dp).testTag("speak_audio_decoded_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Speak Text",
                                            tint = SophisticatedSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.copyToClipboard(audioState.decodedText, "Decoded Audio Morse") },
                                        modifier = Modifier.size(32.dp).testTag("copy_audio_decoded_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Text",
                                            tint = SophisticatedTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.saveDecodedAudioMessage() },
                                        modifier = Modifier.size(32.dp).testTag("save_audio_decoded_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BookmarkAdd,
                                            contentDescription = "Save to Log",
                                            tint = SophisticatedGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.clearDecodedAudioText() },
                                    modifier = Modifier.size(32.dp).testTag("clear_audio_decoded_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = SophisticatedTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SophisticatedSurfaceDark, RoundedCornerShape(12.dp))
                                .border(1.dp, SophisticatedBorder, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = audioState.decodedText.ifEmpty { "Listening for audio beeps or radio tones..." },
                                    color = if (audioState.decodedText.isEmpty()) SophisticatedTextMuted else SophisticatedTextPrimary,
                                    fontSize = 18.sp,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Current Symbol: ",
                                        color = SophisticatedTextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = audioState.currentLetterSymbols.ifEmpty { "(listening)" },
                                        color = SophisticatedPrimary,
                                        fontSize = 13.sp,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                if (audioState.rawMorseStream.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Raw Stream: ${audioState.rawMorseStream}",
                                        color = SophisticatedSecondary.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Acoustic WPM Timing Control
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                    shape = RoundedCornerShape(24.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SophisticatedBorder, SophisticatedBorder)))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Acoustic Timing Speed",
                                color = SophisticatedTextSecondary,
                                fontSize = 11.sp,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "${audioState.detectedWpm} WPM",
                                color = SophisticatedPrimary,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Slider(
                            value = audioState.detectedWpm.toFloat(),
                            onValueChange = { viewModel.setAudioWpm(it.toInt()) },
                            valueRange = 5f..35f,
                            steps = 30,
                            modifier = Modifier.testTag("audio_wpm_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = SophisticatedPrimary,
                                activeTrackColor = SophisticatedPrimary,
                                inactiveTrackColor = SophisticatedSurfaceDark
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
