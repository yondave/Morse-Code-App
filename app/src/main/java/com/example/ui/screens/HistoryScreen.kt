package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MorseMessageEntity
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedGreen
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SophisticatedRed
import com.example.ui.theme.SophisticatedSecondary
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.SophisticatedTextMuted
import com.example.ui.theme.SophisticatedTextPrimary
import com.example.ui.theme.SophisticatedTextSecondary
import com.example.ui.viewmodel.MorseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: MorseViewModel,
    onSelectForTransmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messageHistory.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TRANSMISSION LOG (${messages.size})",
                color = SophisticatedTextSecondary,
                fontSize = 11.sp,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp
            )

            if (messages.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearAllHistory() },
                    modifier = Modifier.size(32.dp).testTag("clear_all_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear All",
                        tint = SophisticatedTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = SophisticatedTextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No transmissions logged yet",
                        color = SophisticatedTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Messages sent or received via flashlight will appear here",
                        color = SophisticatedTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("history_list")
            ) {
                items(messages, key = { it.id }) { message ->
                    HistoryItemCard(
                        message = message,
                        onReplay = { onSelectForTransmit(message.text) },
                        onSpeak = { viewModel.speakText(message.text) },
                        onCopy = { viewModel.copyToClipboard(message.text, "Morse Transmission") },
                        onDelete = { viewModel.deleteHistoryItem(message.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    message: MorseMessageEntity,
    onReplay: () -> Unit,
    onSpeak: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val isSent = message.type == "SENT"
    val isOptical = message.type == "OPTICAL_RX"
    val isAcoustic = message.type == "ACOUSTIC_RX"

    val (badgeIcon, badgeLabel, badgeColor) = when {
        isSent -> Triple(Icons.Default.CallMade, "SENT", SophisticatedPrimary)
        isOptical -> Triple(Icons.Default.CameraAlt, "OPTICAL RX", SophisticatedGreen)
        isAcoustic -> Triple(Icons.Default.Mic, "ACOUSTIC RX", SophisticatedSecondary)
        else -> Triple(Icons.Default.CallReceived, "RECEIVED", SophisticatedGreen)
    }

    val dateStr = remember(message.timestamp) {
        SimpleDateFormat("HH:mm:ss • MMM dd", Locale.getDefault()).format(Date(message.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("history_item_${message.id}"),
        colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SophisticatedBorder, SophisticatedBorder)))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = badgeLabel,
                        color = badgeColor,
                        fontSize = 11.sp,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  $dateStr",
                        color = SophisticatedTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Row {
                    IconButton(onClick = onReplay, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Replay", tint = SophisticatedPrimary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onSpeak, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = SophisticatedSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = SophisticatedTextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SophisticatedRed.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message.text,
                color = SophisticatedTextPrimary,
                fontSize = 16.sp,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = message.morseCode,
                color = SophisticatedPrimary,
                fontSize = 12.sp,
                style = MaterialTheme.typography.bodyMedium,
                letterSpacing = 1.sp
            )
        }
    }
}

