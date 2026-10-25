package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.SophisticatedBg
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

data class TutorialPageData(
    val stepNumber: Int,
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: ImageVector,
    val iconTint: Color,
    val features: List<TutorialFeatureItem>,
    val tip: String? = null
)

data class TutorialFeatureItem(
    val icon: ImageVector,
    val title: String,
    val description: String
)

@Composable
fun TutorialDialog(
    onDismiss: (neverShowAgain: Boolean) -> Unit,
    onPlayDit: () -> Unit = {},
    onPlayDah: () -> Unit = {}
) {
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var neverShowAgain by remember { mutableStateOf(false) }

    val pages = listOf(
        TutorialPageData(
            stepNumber = 1,
            title = "Welcome to Signal Morse",
            subtitle = "The ultimate Morse code communication suite for optical, audio, tactile, and visual signaling.",
            badge = "OVERVIEW",
            icon = Icons.AutoMirrored.Filled.HelpOutline,
            iconTint = SophisticatedPrimary,
            features = listOf(
                TutorialFeatureItem(
                    icon = Icons.Default.FlashOn,
                    title = "Transmit Signals Multi-Modal",
                    description = "Encode messages via camera torch, high-frequency sound, screen light beacon, and haptic vibration."
                ),
                TutorialFeatureItem(
                    icon = Icons.Default.CameraAlt,
                    title = "Optical Camera Receiver",
                    description = "Point your camera at blinking light sources to automatically decode incoming Morse flashes in real time."
                ),
                TutorialFeatureItem(
                    icon = Icons.Default.MenuBook,
                    title = "Comprehensive Dictionary",
                    description = "Full ITU alphabet, emergency prosigns (SOS, AR, SK), numbers, and NATO phonetic audio pronunciations."
                )
            ),
            tip = "Morse code uses short pulses called Dits (•) and 3x longer pulses called Dahs (—)."
        ),
        TutorialPageData(
            stepNumber = 2,
            title = "Transmitting Messages",
            subtitle = "Convert plain text to Morse code or paste raw Morse dots and dashes instantly.",
            badge = "TRANSMITTER",
            icon = Icons.Default.FlashOn,
            iconTint = SophisticatedPrimary,
            features = listOf(
                TutorialFeatureItem(
                    icon = Icons.Default.Sensors,
                    title = "4 Simultaneous Output Channels",
                    description = "Toggle Torch Flash for outdoor distance, Sound Tone for listening, Screen Strobe for dark rooms, and Haptics for silent tactile alerts."
                ),
                TutorialFeatureItem(
                    icon = Icons.Default.GraphicEq,
                    title = "Speed & Frequency Control",
                    description = "Adjust speed from 5 WPM (beginner) up to 40 WPM (advanced), and customize audio pitch from 400Hz to 1200Hz."
                ),
                TutorialFeatureItem(
                    icon = Icons.Default.TouchApp,
                    title = "Instant Emergency Presets",
                    description = "One-tap presets like SOS, HELP, MAYDAY, and CQ CQ CQ for immediate continuous transmission."
                )
            ),
            tip = "You can paste Morse code directly into the message box or use the 'Paste Morse' dialog!"
        ),
        TutorialPageData(
            stepNumber = 3,
            title = "Manual Telegraph Keyer",
            subtitle = "Practice real manual keying just like traditional telegraph and ham radio operators.",
            badge = "KEYER MODE",
            icon = Icons.Default.Radio,
            iconTint = SophisticatedSecondary,
            features = listOf(
                TutorialFeatureItem(
                    icon = Icons.Default.TouchApp,
                    title = "Straight Key & Hold Timing",
                    description = "Tap briefly for a Dit (•) or hold for 3x duration for a Dah (—). Real-time sidetone plays while pressed."
                ),
                TutorialFeatureItem(
                    icon = Icons.Default.GraphicEq,
                    title = "Auto-Transcribing Engine",
                    description = "Releasing the key for a letter gap automatically resolves your accumulated dots and dashes into plain text."
                ),
                TutorialFeatureItem(
                    icon = Icons.Default.VolumeUp,
                    title = "Realistic CW Audio Sidetone",
                    description = "Hann-windowed anti-click sine wave audio synthesizer provides authentic radio telegraph feel."
                )
            ),
            tip = "Switch to Keyer Mode from the Transmit screen using the 'Keyer Mode' toggle."
        ),
        TutorialPageData(
            stepNumber = 4,
            title = "Dual Optical & Acoustic Rx",
            subtitle = "Decode optical flashing lights or acoustic audio tones and radio beeps.",
            badge = "RECEIVER",
            icon = Icons.Default.Sensors,
            iconTint = SophisticatedGreen,
            features = listOf(
                TutorialFeatureItem(
                    icon = Icons.Default.CameraAlt,
                    title = "Tap-to-Aim Reticle & Zoom",
                    description = "In Optical mode, tap the camera viewfinder to lock the reticle onto distant beacons. Adjust zoom up to 5x with exposure lock."
                ),
                TutorialFeatureItem(
                    icon = Icons.Default.GraphicEq,
                    title = "Acoustic Mic Tone Decoder",
                    description = "Switch to Acoustic Mic mode to decode Morse audio tones, whistle blasts, or CW radio beeps with Goertzel frequency filtering."
                ),
                TutorialFeatureItem(
                    icon = Icons.Default.Check,
                    title = "Dual Oscilloscope & Logging",
                    description = "Live waveform visualizer graphs incoming signal energy. Save decoded transmissions directly to your persistent history log."
                )
            ),
            tip = "Use 'Auto-Tune' in Acoustic mode to automatically lock onto the dominant tone pitch!"
        ),
        TutorialPageData(
            stepNumber = 5,
            title = "Dictionary & Prosigns",
            subtitle = "Master international Morse conventions and standard emergency codes.",
            badge = "REFERENCE",
            icon = Icons.Default.MenuBook,
            iconTint = SophisticatedPrimary,
            features = listOf(
                TutorialFeatureItem(
                    icon = Icons.Default.MenuBook,
                    title = "Categorized Reference",
                    description = "Browse Letters, Numbers, Punctuation, International Prosigns (e.g. SOS, AR, SK, BT), and Q-Codes."
                ),
                TutorialFeatureItem(
                    icon = Icons.Default.VolumeUp,
                    title = "Sound & Phonetic Audio",
                    description = "Tap any entry to hear its authentic Morse cadence and spoken NATO phonetic pronunciation (Alpha, Bravo, Charlie...)."
                ),
                TutorialFeatureItem(
                    icon = Icons.Default.Check,
                    title = "Transmission History",
                    description = "All sent and received messages are logged locally so you can review, re-transmit, or copy anytime."
                )
            ),
            tip = "Search by letter name, Morse dots/dashes, or phonetic word in the Reference tab."
        )
    )

    val currentPage = pages[currentPageIndex]
    val isLastPage = currentPageIndex == pages.lastIndex

    Dialog(
        onDismissRequest = { onDismiss(neverShowAgain) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("tutorial_dialog_surface"),
            shape = RoundedCornerShape(24.dp),
            color = SophisticatedSurface,
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(SophisticatedPrimary.copy(alpha = 0.6f), SophisticatedBorder))
            ),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Bar: Badge, Step Indicator, and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(SophisticatedPrimaryContainer, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${currentPage.badge} • STEP ${currentPage.stepNumber}/${pages.size}",
                            color = SophisticatedOnPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    IconButton(
                        onClick = { onDismiss(neverShowAgain) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("tutorial_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close tutorial",
                            tint = SophisticatedTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Body Content
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState)
                ) {
                    AnimatedContent(
                        targetState = currentPage,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                        },
                        label = "TutorialSlideAnimation"
                    ) { page ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Icon + Title Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(page.iconTint.copy(alpha = 0.15f), CircleShape)
                                        .border(1.dp, page.iconTint.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = page.icon,
                                        contentDescription = null,
                                        tint = page.iconTint,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = page.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SophisticatedTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = page.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SophisticatedTextSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            // Interactive Audio Dit/Dah tester on Page 1
                            if (page.stepNumber == 1) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = SophisticatedSurfaceDark),
                                    shape = RoundedCornerShape(12.dp),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = Brush.linearGradient(listOf(SophisticatedPrimary.copy(alpha = 0.3f), SophisticatedBorder))
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "QUICK SOUND TEST",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SophisticatedSecondary,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Tap to hear the fundamental units of Morse signaling:",
                                            fontSize = 11.sp,
                                            color = SophisticatedTextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = onPlayDit,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = SophisticatedPrimary,
                                                    contentColor = SophisticatedOnPrimary
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("tutorial_dit_button"),
                                                contentPadding = PaddingValues(vertical = 8.dp)
                                            ) {
                                                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Dit (•)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = onPlayDah,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = SophisticatedSecondary,
                                                    contentColor = SophisticatedSurfaceDark
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("tutorial_dah_button"),
                                                contentPadding = PaddingValues(vertical = 8.dp)
                                            ) {
                                                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Dah (—)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Features List
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                page.features.forEach { feature ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SophisticatedSurfaceDark, RoundedCornerShape(12.dp))
                                            .border(1.dp, SophisticatedBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(SophisticatedPrimaryContainer.copy(alpha = 0.5f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = feature.icon,
                                                contentDescription = null,
                                                tint = SophisticatedPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = feature.title,
                                                color = SophisticatedTextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = feature.description,
                                                color = SophisticatedTextSecondary,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Tip banner if present
                            if (page.tip != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SophisticatedPrimary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                        .border(1.dp, SophisticatedPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "💡 Tip: ${page.tip}",
                                        color = SophisticatedSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Page Indicator Dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.indices.forEach { index ->
                        val isSelected = index == currentPageIndex
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(
                                    width = if (isSelected) 18.dp else 6.dp,
                                    height = 6.dp
                                )
                                .clip(CircleShape)
                                .background(if (isSelected) SophisticatedPrimary else SophisticatedTextMuted.copy(alpha = 0.4f))
                                .clickable { currentPageIndex = index }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentPageIndex > 0) {
                        TextButton(
                            onClick = { currentPageIndex-- },
                            modifier = Modifier.testTag("tutorial_prev_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp), tint = SophisticatedTextSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back", color = SophisticatedTextSecondary, fontSize = 13.sp)
                        }
                    } else {
                        TextButton(
                            onClick = { onDismiss(neverShowAgain) },
                            modifier = Modifier.testTag("tutorial_skip_button")
                        ) {
                            Text("Skip Tour", color = SophisticatedTextMuted, fontSize = 13.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (isLastPage) {
                                onDismiss(neverShowAgain)
                            } else {
                                currentPageIndex++
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SophisticatedPrimary,
                            contentColor = SophisticatedOnPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("tutorial_next_button")
                    ) {
                        Text(
                            text = if (isLastPage) "Get Started!" else "Next",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (!isLastPage) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // "Don't show again" Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { neverShowAgain = !neverShowAgain },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Checkbox(
                        checked = neverShowAgain,
                        onCheckedChange = { neverShowAgain = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = SophisticatedPrimary,
                            checkmarkColor = SophisticatedOnPrimary,
                            uncheckedColor = SophisticatedTextMuted
                        ),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Don't show tutorial on startup",
                        color = SophisticatedTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
