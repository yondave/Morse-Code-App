package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.morse.MorseEngine
import com.example.ui.components.ManualKeyerView
import com.example.ui.components.VisualBeacon
import com.example.ui.theme.SophisticatedBg
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedGreen
import com.example.ui.theme.SophisticatedOnPrimary
import com.example.ui.theme.SophisticatedOnPrimaryContainer
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SophisticatedPrimaryContainer
import com.example.ui.theme.SophisticatedRed
import com.example.ui.theme.SophisticatedSecondary
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.SophisticatedSurfaceDark
import com.example.ui.theme.SophisticatedTextMuted
import com.example.ui.theme.SophisticatedTextPrimary
import com.example.ui.theme.SophisticatedTextSecondary
import com.example.ui.viewmodel.MorseViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransmitScreen(
    viewModel: MorseViewModel,
    modifier: Modifier = Modifier
) {
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val translatedMorse by viewModel.translatedMorse.collectAsStateWithLifecycle()
    val wpm by viewModel.wpm.collectAsStateWithLifecycle()
    val pitchHz by viewModel.pitchHz.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val torchEnabled by viewModel.torchEnabled.collectAsStateWithLifecycle()
    val screenFlashEnabled by viewModel.screenFlashEnabled.collectAsStateWithLifecycle()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val loopEnabled by viewModel.loopEnabled.collectAsStateWithLifecycle()
    val playbackProgress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    val manualKeyerState by viewModel.manualKeyerState.collectAsStateWithLifecycle()

    var showKeyer by remember { mutableStateOf(false) }
    var showPasteMorseDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Beacon & Transmission Status Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transmission_status_card"),
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
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (playbackProgress.isPlaying) SophisticatedGreen else SophisticatedTextMuted)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (playbackProgress.isPlaying) "TRANSMITTING LIVE" else "TRANSMITTER READY",
                            color = if (playbackProgress.isPlaying) SophisticatedGreen else SophisticatedTextSecondary,
                            fontSize = 11.sp,
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = "$wpm WPM • ${pitchHz}Hz",
                        color = SophisticatedPrimary,
                        fontSize = 11.sp,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Center Beacon
                VisualBeacon(
                    isActive = playbackProgress.isSignalActive,
                    size = 90.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Active Character Highlighting Ticker
                if (playbackProgress.isPlaying && playbackProgress.currentCharIndex >= 0 && playbackProgress.currentCharIndex < inputText.length) {
                    val activeChar = inputText[playbackProgress.currentCharIndex]
                    val activeSymbol = playbackProgress.currentMorseToken?.morseSymbol ?: ""
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Sending '$activeChar' ",
                            color = SophisticatedTextPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "($activeSymbol)",
                            color = SophisticatedPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else {
                    Text(
                        text = if (playbackProgress.isPlaying) "Transmitting stream..." else "Ready to broadcast",
                        color = SophisticatedTextSecondary,
                        fontSize = 13.sp,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Transmit Action Button
                Button(
                    onClick = {
                        if (playbackProgress.isPlaying) {
                            viewModel.stopTransmission()
                        } else {
                            viewModel.startTransmission()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("transmit_main_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (playbackProgress.isPlaying) SophisticatedRed else SophisticatedPrimary,
                        contentColor = if (playbackProgress.isPlaying) Color.White else SophisticatedOnPrimary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = if (playbackProgress.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (playbackProgress.isPlaying) "Stop" else "Transmit",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (playbackProgress.isPlaying) "STOP TRANSMISSION" else "TRANSMIT MORSE CODE",
                        style = MaterialTheme.typography.titleSmall,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Emergency Presets
        Text(
            text = "QUICK TRANSMISSION PRESETS",
            color = SophisticatedTextSecondary,
            fontSize = 11.sp,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 4.dp, bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            viewModel.quickPresets.forEach { preset ->
                FilterChip(
                    selected = inputText == preset,
                    onClick = { viewModel.onInputTextChanged(preset) },
                    label = { Text(preset) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SophisticatedPrimary,
                        selectedLabelColor = SophisticatedOnPrimary,
                        containerColor = SophisticatedSurface,
                        labelColor = SophisticatedTextPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = inputText == preset,
                        borderColor = SophisticatedBorder,
                        selectedBorderColor = SophisticatedPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Text Input Field & Translated Morse Output
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("message_input_card"),
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            shape = RoundedCornerShape(24.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SophisticatedBorder, SophisticatedBorder)))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.onInputTextChanged(it) },
                        label = { Text("Message to Encode") },
                        placeholder = { Text("Enter text (e.g. SOS, HELP) or paste Morse...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("message_text_input"),
                        textStyle = TextStyle(
                            textDirection = TextDirection.Ltr,
                            fontSize = 16.sp,
                            color = SophisticatedTextPrimary
                        ),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.pasteFromClipboard() },
                                    modifier = Modifier.testTag("paste_clipboard_button")
                                ) {
                                    Icon(
                                        Icons.Default.ContentPaste,
                                        contentDescription = "Paste from clipboard",
                                        tint = SophisticatedPrimary
                                    )
                                }
                                if (inputText.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.onInputTextChanged("") },
                                        modifier = Modifier.testTag("clear_input_button")
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear text",
                                            tint = SophisticatedTextSecondary
                                        )
                                    }
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SophisticatedPrimary,
                            unfocusedBorderColor = SophisticatedBorder,
                            focusedLabelColor = SophisticatedPrimary,
                            unfocusedLabelColor = SophisticatedTextSecondary,
                            focusedTextColor = SophisticatedTextPrimary,
                            unfocusedTextColor = SophisticatedTextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Inline Morse Code Auto-Detection & 1-Tap Converter
                val isRawMorse = remember(inputText) { MorseEngine.isMorseCode(inputText) }
                val decodedCandidate = remember(inputText, isRawMorse) {
                    if (isRawMorse) MorseEngine.morseToText(inputText) else ""
                }

                AnimatedVisibility(
                    visible = isRawMorse && decodedCandidate.isNotBlank() && decodedCandidate != inputText,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .testTag("morse_detected_banner"),
                        colors = CardDefaults.cardColors(containerColor = SophisticatedSurfaceDark),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.linearGradient(listOf(SophisticatedPrimary, SophisticatedSecondary))
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = null,
                                    tint = SophisticatedPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Morse code detected!",
                                        color = SophisticatedSecondary,
                                        fontSize = 11.sp,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = "Decodes to: \"$decodedCandidate\"",
                                        color = SophisticatedTextPrimary,
                                        fontSize = 13.sp,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.onInputTextChanged(decodedCandidate) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SophisticatedPrimary,
                                    contentColor = SophisticatedOnPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("convert_inline_morse_button")
                            ) {
                                Text("Convert", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Morse Code Translation Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SophisticatedSurfaceDark, RoundedCornerShape(12.dp))
                        .border(1.dp, SophisticatedBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MORSE CODE ENCODING",
                                color = SophisticatedPrimary,
                                fontSize = 10.sp,
                                style = MaterialTheme.typography.labelSmall
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { showPasteMorseDialog = true },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("paste_morse_header_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste Morse Code",
                                        tint = SophisticatedPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.copyToClipboard(translatedMorse, "Morse Code") },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("copy_morse_header_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Morse",
                                        tint = SophisticatedTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = translatedMorse.ifEmpty { "• • •  — — —  • • •" },
                                color = if (translatedMorse.isEmpty()) SophisticatedTextMuted else SophisticatedPrimary,
                                fontSize = 16.sp,
                                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Multi-Channel Output Hardware Switches
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            shape = RoundedCornerShape(24.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SophisticatedBorder, SophisticatedBorder)))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "TRANSMISSION CHANNELS",
                    color = SophisticatedTextSecondary,
                    fontSize = 11.sp,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutputToggleRow(
                    icon = Icons.Default.FlashOn,
                    title = "Camera LED Flashlight",
                    subtitle = "Beams signals via phone's optical flash LED",
                    checked = torchEnabled,
                    onCheckedChange = { viewModel.toggleTorch(it) },
                    tint = SophisticatedPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutputToggleRow(
                    icon = Icons.Default.VolumeUp,
                    title = "Audio Sidetone Beeps",
                    subtitle = "Pure sine wave CW telegraph audio",
                    checked = soundEnabled,
                    onCheckedChange = { viewModel.toggleSound(it) },
                    tint = SophisticatedSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutputToggleRow(
                    icon = Icons.Default.GraphicEq,
                    title = "Screen Visual Flash",
                    subtitle = "Pulses screen display synchronously",
                    checked = screenFlashEnabled,
                    onCheckedChange = { viewModel.toggleScreenFlash(it) },
                    tint = SophisticatedGreen
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutputToggleRow(
                    icon = Icons.Default.Vibration,
                    title = "Haptic Tactile Pulses",
                    subtitle = "Vibrates device during dots and dashes",
                    checked = vibrationEnabled,
                    onCheckedChange = { viewModel.toggleVibration(it) },
                    tint = SophisticatedPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutputToggleRow(
                    icon = Icons.Default.Repeat,
                    title = "Continuous Repeat Loop",
                    subtitle = "Repeats transmission until stopped",
                    checked = loopEnabled,
                    onCheckedChange = { viewModel.toggleLoop(it) },
                    tint = SophisticatedOnPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Speed & Audio Tone Pitch Sliders
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = SophisticatedPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TRANSMISSION SPEED",
                            color = SophisticatedTextSecondary,
                            fontSize = 11.sp,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Text(
                        text = "$wpm WPM",
                        color = SophisticatedPrimary,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Slider(
                    value = wpm.toFloat(),
                    onValueChange = { viewModel.setWpm(it.toInt()) },
                    valueRange = 5f..35f,
                    steps = 29,
                    modifier = Modifier.testTag("wpm_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = SophisticatedPrimary,
                        activeTrackColor = SophisticatedPrimary,
                        inactiveTrackColor = SophisticatedSurfaceDark
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("5 WPM (Slow / Learn)", color = SophisticatedTextSecondary, fontSize = 10.sp)
                    Text("15 WPM (Standard)", color = SophisticatedTextSecondary, fontSize = 10.sp)
                    Text("35 WPM (Fast)", color = SophisticatedTextSecondary, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = SophisticatedSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AUDIO TONE PITCH",
                            color = SophisticatedTextSecondary,
                            fontSize = 11.sp,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Text(
                        text = "$pitchHz Hz",
                        color = SophisticatedSecondary,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Slider(
                    value = pitchHz.toFloat(),
                    onValueChange = { viewModel.setPitch(it.toInt()) },
                    valueRange = 400f..1200f,
                    steps = 15,
                    modifier = Modifier.testTag("pitch_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = SophisticatedSecondary,
                        activeTrackColor = SophisticatedSecondary,
                        inactiveTrackColor = SophisticatedSurfaceDark
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive Straight Key Telegraph Toggle
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
                        text = "MANUAL TELEGRAPH KEYER",
                        color = SophisticatedTextSecondary,
                        fontSize = 11.sp,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Button(
                        onClick = { showKeyer = !showKeyer },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showKeyer) SophisticatedPrimaryContainer else SophisticatedSurfaceDark,
                            contentColor = if (showKeyer) SophisticatedOnPrimaryContainer else SophisticatedPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (showKeyer) "Hide Keyer" else "Open Keyer")
                    }
                }

                AnimatedVisibility(visible = showKeyer) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        ManualKeyerView(
                            keyerState = manualKeyerState,
                            onKeyDown = { viewModel.onManualKeyDown() },
                            onKeyUp = { viewModel.onManualKeyUp() },
                            onClear = { viewModel.clearManualKeyerText() }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showPasteMorseDialog) {
        PasteMorseDialog(
            initialMorse = viewModel.getClipboardText().takeIf { MorseEngine.isMorseCode(it) } ?: "",
            onDismiss = { showPasteMorseDialog = false },
            onApply = { decodedText ->
                viewModel.onInputTextChanged(decodedText)
                showPasteMorseDialog = false
            },
            onGetClipboard = { viewModel.getClipboardText() }
        )
    }
}

@Composable
fun PasteMorseDialog(
    initialMorse: String = "",
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
    onGetClipboard: () -> String
) {
    var morseText by remember { mutableStateOf(initialMorse) }
    val decodedText = remember(morseText) { MorseEngine.morseToText(morseText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = null,
                    tint = SophisticatedPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Paste & Decode Morse", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Paste or construct Morse dots (.) and dashes (-). Letter breaks can use spaces and words can use slashes (/).",
                    color = SophisticatedTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(
                        value = morseText,
                        onValueChange = { morseText = it },
                        placeholder = { Text("e.g. ... --- ...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("paste_morse_input_field"),
                        textStyle = TextStyle(
                            textDirection = TextDirection.Ltr,
                            fontSize = 15.sp,
                            color = SophisticatedTextPrimary
                        ),
                        trailingIcon = {
                            if (morseText.isNotEmpty()) {
                                IconButton(onClick = { morseText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SophisticatedTextSecondary)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SophisticatedPrimary,
                            unfocusedBorderColor = SophisticatedBorder,
                            focusedTextColor = SophisticatedTextPrimary,
                            unfocusedTextColor = SophisticatedTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar: Paste from clipboard + symbol keypad
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val clip = onGetClipboard()
                            if (clip.isNotBlank()) {
                                morseText = clip
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SophisticatedSurfaceDark,
                            contentColor = SophisticatedPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("dialog_paste_clip_button")
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste Clip", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { morseText += "•" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SophisticatedSurfaceDark,
                            contentColor = SophisticatedTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("•", fontSize = 14.sp)
                    }

                    Button(
                        onClick = { morseText += "—" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SophisticatedSurfaceDark,
                            contentColor = SophisticatedTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("—", fontSize = 14.sp)
                    }

                    Button(
                        onClick = { morseText += " " },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SophisticatedSurfaceDark,
                            contentColor = SophisticatedTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("Space", fontSize = 10.sp)
                    }

                    IconButton(
                        onClick = {
                            if (morseText.isNotEmpty()) {
                                morseText = morseText.dropLast(1)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Backspace",
                            tint = SophisticatedTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Decoded Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SophisticatedSurfaceDark, RoundedCornerShape(10.dp))
                        .border(1.dp, SophisticatedBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "DECODED TEXT PREVIEW",
                            color = SophisticatedSecondary,
                            fontSize = 10.sp,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = decodedText.ifEmpty { "(Enter or paste Morse code above)" },
                            color = if (decodedText.isEmpty()) SophisticatedTextMuted else SophisticatedTextPrimary,
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (decodedText.isNotBlank()) {
                        onApply(decodedText)
                    }
                },
                enabled = decodedText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SophisticatedPrimary,
                    contentColor = SophisticatedOnPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("apply_morse_dialog_button")
            ) {
                Text("Insert Decoded Text")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SophisticatedTextSecondary)
            }
        },
        containerColor = SophisticatedSurface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun OutputToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) tint else SophisticatedTextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = SophisticatedTextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = subtitle,
                    color = SophisticatedTextSecondary,
                    fontSize = 11.sp,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SophisticatedOnPrimary,
                checkedTrackColor = SophisticatedPrimary,
                uncheckedThumbColor = SophisticatedTextSecondary,
                uncheckedTrackColor = SophisticatedSurfaceDark
            )
        )
    }
}

