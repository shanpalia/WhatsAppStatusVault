package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ThemeMode
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MessageHistoryScreen
import com.example.ui.screens.PinLockScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SavedMediaScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StatusSaverScreen
import com.example.ui.screens.WhatsAppDirectScreen
import com.example.ui.theme.StatusVaultTheme
import com.example.ui.viewmodel.MainViewModel

enum class ScreenRoute {
    SPLASH,
    HOME,
    STATUS_SAVER,
    MESSAGE_HISTORY,
    WHATSAPP_DIRECT,
    SAVED_MEDIA,
    REPORTS,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            val isPinEnabled by viewModel.isPinEnabled.collectAsStateWithLifecycle()
            val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()

            var currentRoute by remember { mutableStateOf(ScreenRoute.SPLASH) }

            StatusVaultTheme(darkTheme = isDarkTheme) {
                if (currentRoute == ScreenRoute.SPLASH) {
                    SplashScreen(
                        onSplashFinished = {
                            currentRoute = ScreenRoute.HOME
                        }
                    )
                } else if (isPinEnabled && !isAppUnlocked) {
                    PinLockScreen(
                        onUnlockSuccess = {
                            // Automatically updates state via viewModel.unlockApp
                        },
                        onVerifyPin = { pin ->
                            viewModel.unlockApp(pin)
                        }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            if (currentRoute != ScreenRoute.HOME) {
                                TopAppBar(
                                    title = {
                                        Text(
                                            text = when (currentRoute) {
                                                ScreenRoute.STATUS_SAVER -> "Status Saver"
                                                ScreenRoute.MESSAGE_HISTORY -> "Message History"
                                                ScreenRoute.WHATSAPP_DIRECT -> "WhatsApp Direct"
                                                ScreenRoute.SAVED_MEDIA -> "Saved Media"
                                                ScreenRoute.REPORTS -> "Reports & Export"
                                                ScreenRoute.SETTINGS -> "Settings"
                                                else -> "Status Vault"
                                            },
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = { currentRoute = ScreenRoute.HOME }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back to Home"
                                            )
                                        }
                                    },
                                    actions = {
                                        if (currentRoute != ScreenRoute.SETTINGS) {
                                            IconButton(onClick = { currentRoute = ScreenRoute.SETTINGS }) {
                                                Icon(
                                                    imageVector = Icons.Default.Settings,
                                                    contentDescription = "Settings"
                                                )
                                            }
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentRoute == ScreenRoute.HOME,
                                    onClick = { currentRoute = ScreenRoute.HOME },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home", fontSize = 10.sp, fontWeight = if (currentRoute == ScreenRoute.HOME) FontWeight.Bold else FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = com.example.ui.theme.BentoOnContainer,
                                        selectedTextColor = com.example.ui.theme.BentoOnContainer,
                                        indicatorColor = com.example.ui.theme.BentoIndicator,
                                        unselectedIconColor = com.example.ui.theme.BentoTextSecondary,
                                        unselectedTextColor = com.example.ui.theme.BentoTextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_tab_home")
                                )

                                NavigationBarItem(
                                    selected = currentRoute == ScreenRoute.STATUS_SAVER,
                                    onClick = { currentRoute = ScreenRoute.STATUS_SAVER },
                                    icon = { Icon(Icons.Default.Download, contentDescription = "Status") },
                                    label = { Text("Status", fontSize = 10.sp, fontWeight = if (currentRoute == ScreenRoute.STATUS_SAVER) FontWeight.Bold else FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = com.example.ui.theme.BentoOnContainer,
                                        selectedTextColor = com.example.ui.theme.BentoOnContainer,
                                        indicatorColor = com.example.ui.theme.BentoIndicator,
                                        unselectedIconColor = com.example.ui.theme.BentoTextSecondary,
                                        unselectedTextColor = com.example.ui.theme.BentoTextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_tab_status")
                                )

                                NavigationBarItem(
                                    selected = currentRoute == ScreenRoute.MESSAGE_HISTORY,
                                    onClick = { currentRoute = ScreenRoute.MESSAGE_HISTORY },
                                    icon = { Icon(Icons.Default.History, contentDescription = "Messages") },
                                    label = { Text("Messages", fontSize = 10.sp, fontWeight = if (currentRoute == ScreenRoute.MESSAGE_HISTORY) FontWeight.Bold else FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = com.example.ui.theme.BentoOnContainer,
                                        selectedTextColor = com.example.ui.theme.BentoOnContainer,
                                        indicatorColor = com.example.ui.theme.BentoIndicator,
                                        unselectedIconColor = com.example.ui.theme.BentoTextSecondary,
                                        unselectedTextColor = com.example.ui.theme.BentoTextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_tab_messages")
                                )

                                NavigationBarItem(
                                    selected = currentRoute == ScreenRoute.WHATSAPP_DIRECT,
                                    onClick = { currentRoute = ScreenRoute.WHATSAPP_DIRECT },
                                    icon = { Icon(Icons.Default.Send, contentDescription = "Direct") },
                                    label = { Text("Direct", fontSize = 10.sp, fontWeight = if (currentRoute == ScreenRoute.WHATSAPP_DIRECT) FontWeight.Bold else FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = com.example.ui.theme.BentoOnContainer,
                                        selectedTextColor = com.example.ui.theme.BentoOnContainer,
                                        indicatorColor = com.example.ui.theme.BentoIndicator,
                                        unselectedIconColor = com.example.ui.theme.BentoTextSecondary,
                                        unselectedTextColor = com.example.ui.theme.BentoTextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_tab_direct")
                                )

                                NavigationBarItem(
                                    selected = currentRoute == ScreenRoute.SAVED_MEDIA,
                                    onClick = { currentRoute = ScreenRoute.SAVED_MEDIA },
                                    icon = { Icon(Icons.Default.Bookmark, contentDescription = "Vault") },
                                    label = { Text("Vault", fontSize = 10.sp, fontWeight = if (currentRoute == ScreenRoute.SAVED_MEDIA) FontWeight.Bold else FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = com.example.ui.theme.BentoOnContainer,
                                        selectedTextColor = com.example.ui.theme.BentoOnContainer,
                                        indicatorColor = com.example.ui.theme.BentoIndicator,
                                        unselectedIconColor = com.example.ui.theme.BentoTextSecondary,
                                        unselectedTextColor = com.example.ui.theme.BentoTextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_tab_saved")
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentRoute) {
                                ScreenRoute.HOME -> HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToStatus = { currentRoute = ScreenRoute.STATUS_SAVER },
                                    onNavigateToMessages = { currentRoute = ScreenRoute.MESSAGE_HISTORY },
                                    onNavigateToDirect = { currentRoute = ScreenRoute.WHATSAPP_DIRECT },
                                    onNavigateToSaved = { currentRoute = ScreenRoute.SAVED_MEDIA },
                                    onNavigateToReports = { currentRoute = ScreenRoute.REPORTS },
                                    onNavigateToSettings = { currentRoute = ScreenRoute.SETTINGS }
                                )
                                ScreenRoute.STATUS_SAVER -> StatusSaverScreen(
                                    viewModel = viewModel
                                )
                                ScreenRoute.MESSAGE_HISTORY -> MessageHistoryScreen(
                                    viewModel = viewModel
                                )
                                ScreenRoute.WHATSAPP_DIRECT -> WhatsAppDirectScreen(
                                    viewModel = viewModel
                                )
                                ScreenRoute.SAVED_MEDIA -> SavedMediaScreen(
                                    viewModel = viewModel
                                )
                                ScreenRoute.REPORTS -> ReportsScreen(
                                    viewModel = viewModel
                                )
                                ScreenRoute.SETTINGS -> SettingsScreen(
                                    viewModel = viewModel
                                )
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}
