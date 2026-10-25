package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.morse.MorseCategory
import com.example.core.morse.MorseDictionaryEntry
import com.example.core.morse.MorseEngine
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedOnPrimary
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SophisticatedSecondary
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.SophisticatedSurfaceDark
import com.example.ui.theme.SophisticatedTextPrimary
import com.example.ui.theme.SophisticatedTextSecondary
import com.example.ui.viewmodel.MorseViewModel

@Composable
fun DictionaryScreen(
    viewModel: MorseViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<MorseCategory?>(null) }

    val filteredList = remember(searchQuery, selectedCategory) {
        MorseEngine.DICTIONARY.filter { entry ->
            val matchesCategory = selectedCategory == null || entry.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                    entry.character.contains(searchQuery, ignoreCase = true) ||
                    entry.code.contains(searchQuery) ||
                    entry.phonetic.contains(searchQuery, ignoreCase = true) ||
                    entry.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Search Input
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search Morse code, letters, prosigns...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = SophisticatedTextSecondary) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val clip = viewModel.getClipboardText()
                                if (clip.isNotBlank()) {
                                    searchQuery = clip
                                }
                            },
                            modifier = Modifier.testTag("dictionary_paste_button")
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste to search", tint = SophisticatedPrimary)
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SophisticatedTextSecondary)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dictionary_search_input"),
                textStyle = TextStyle(
                    textDirection = TextDirection.Ltr,
                    fontSize = 15.sp,
                    color = SophisticatedTextPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SophisticatedPrimary,
                    unfocusedBorderColor = SophisticatedBorder,
                    focusedTextColor = SophisticatedTextPrimary,
                    unfocusedTextColor = SophisticatedTextPrimary,
                    focusedContainerColor = SophisticatedSurface,
                    unfocusedContainerColor = SophisticatedSurface
                ),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SophisticatedPrimary,
                    selectedLabelColor = SophisticatedOnPrimary,
                    containerColor = SophisticatedSurface,
                    labelColor = SophisticatedTextPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedCategory == null,
                    borderColor = SophisticatedBorder,
                    selectedBorderColor = SophisticatedPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )
            MorseCategory.entries.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                    label = {
                        Text(
                            when (cat) {
                                MorseCategory.LETTERS -> "Letters"
                                MorseCategory.NUMBERS -> "Numbers"
                                MorseCategory.PUNCTUATION -> "Symbols"
                                MorseCategory.PROSIGNS -> "Prosigns"
                            }
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SophisticatedPrimary,
                        selectedLabelColor = SophisticatedOnPrimary,
                        containerColor = SophisticatedSurface,
                        labelColor = SophisticatedTextPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedCategory == cat,
                        borderColor = SophisticatedBorder,
                        selectedBorderColor = SophisticatedPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Grid of Morse Entries
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("dictionary_grid")
        ) {
            items(filteredList) { entry ->
                DictionaryCard(
                    entry = entry,
                    onPlay = { viewModel.playSingleEntry(entry) }
                )
            }
        }
    }
}

@Composable
private fun DictionaryCard(
    entry: MorseDictionaryEntry,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .testTag("dict_item_${entry.character}"),
        colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SophisticatedBorder, SophisticatedBorder)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.character,
                    color = SophisticatedTextPrimary,
                    fontSize = 22.sp,
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Listen",
                        tint = SophisticatedSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = entry.code,
                    color = SophisticatedPrimary,
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Ltr),
                    letterSpacing = 2.sp
                )
            }

            if (entry.phonetic.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.phonetic,
                    color = SophisticatedTextSecondary,
                    fontSize = 11.sp,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

