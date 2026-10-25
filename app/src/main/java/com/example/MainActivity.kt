package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.TutorialDialog
import com.example.ui.screens.DictionaryScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ReceiveScreen
import com.example.ui.screens.TransmitScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SophisticatedBg
import com.example.ui.theme.SophisticatedNavBg
import com.example.ui.theme.SophisticatedOnPrimary
import com.example.ui.theme.SophisticatedPrimary
import com.example.ui.theme.SophisticatedTextPrimary
import com.example.ui.theme.SophisticatedTextSecondary
import com.example.ui.viewmodel.MorseViewModel

enum class MorseNavTab(val title: String, val icon: ImageVector, val tag: String) {
    TRANSMIT("Transmit", Icons.Default.FlashOn, "nav_transmit"),
    RECEIVE("Dual Rx", Icons.Default.Sensors, "nav_receive"),
    DICTIONARY("Reference", Icons.Default.MenuBook, "nav_dictionary"),
    HISTORY("Log", Icons.Default.History, "nav_history")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MorseAppRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorseAppRoot(
    viewModel: MorseViewModel = viewModel()
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val isScreenLit by viewModel.isScreenLit.collectAsStateWithLifecycle()
    val showTutorial by viewModel.showTutorial.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = SophisticatedBg,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(SophisticatedPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = SophisticatedOnPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Signal Morse",
                                style = MaterialTheme.typography.titleMedium,
                                color = SophisticatedTextPrimary,
                                letterSpacing = 0.5.sp
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.openTutorial() },
                            modifier = Modifier.testTag("app_tutorial_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "App Tutorial & Guide",
                                tint = SophisticatedPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = SophisticatedBg,
                        titleContentColor = SophisticatedTextPrimary
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = SophisticatedNavBg,
                    contentColor = SophisticatedTextPrimary,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    MorseNavTab.entries.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SophisticatedOnPrimary,
                                selectedTextColor = SophisticatedPrimary,
                                indicatorColor = SophisticatedPrimary,
                                unselectedIconColor = SophisticatedTextSecondary,
                                unselectedTextColor = SophisticatedTextSecondary
                            ),
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTabIndex) {
                    0 -> TransmitScreen(
                        viewModel = viewModel
                    )
                    1 -> ReceiveScreen(
                        viewModel = viewModel
                    )
                    2 -> DictionaryScreen(
                        viewModel = viewModel
                    )
                    3 -> HistoryScreen(
                        viewModel = viewModel,
                        onSelectForTransmit = { text ->
                            viewModel.onInputTextChanged(text)
                            selectedTabIndex = 0
                        }
                    )
                }
            }
        }

        // Screen Flash Synchronous Light Overlay
        if (isScreenLit) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SophisticatedPrimary.copy(alpha = 0.5f))
                    .testTag("screen_flash_overlay")
            )
        }

        // Onboarding Tutorial Dialog for First-Time Users & On-Demand Help
        if (showTutorial) {
            TutorialDialog(
                onDismiss = { neverShowAgain ->
                    viewModel.dismissTutorial(neverShowAgain)
                },
                onPlayDit = { viewModel.playDitAudio() },
                onPlayDah = { viewModel.playDahAudio() }
            )
        }
    }
}

