package com.example.ui.screens

import com.example.BuildConfig

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ThemeMode
import com.example.data.repository.UpdateCheckResult
import com.example.service.WhatsAppNotificationListener
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isPinEnabled by viewModel.isPinEnabled.collectAsStateWithLifecycle()
    val autoRefresh by viewModel.autoRefresh.collectAsStateWithLifecycle()
    val notifyNewStatus by viewModel.notifyNewStatus.collectAsStateWithLifecycle()
    val isNotifAccessGranted by viewModel.isNotificationAccessGranted.collectAsStateWithLifecycle()
    val folderUri by viewModel.statusFolderUri.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    var showPinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    val isWhatsAppInstalled = remember {
        try {
            context.packageManager.getPackageInfo("com.whatsapp", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    val isWhatsAppBusinessInstalled = remember {
        try {
            context.packageManager.getPackageInfo("com.whatsapp.w4b", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                // Ignore if flags can't be persisted
            }
            viewModel.setStatusFolderUri(uri.toString())
            Toast.makeText(context, "Status folder access granted!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkNotificationAccess()
    }

    // PIN Setup / Change Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                newPinInput = ""
                confirmPinInput = ""
                pinError = null
            },
            title = { Text(if (isPinEnabled) "Change Vault PIN" else "Set Vault PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter a 4-6 digit numeric PIN to protect WhatsApp Status Vault:")
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) newPinInput = it
                        },
                        label = { Text("New PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) confirmPinInput = it
                        },
                        label = { Text("Confirm PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    if (pinError != null) {
                        Text(text = pinError!!, color = Color(0xFFD32F2F), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.length < 4) {
                            pinError = "PIN must be at least 4 digits"
                        } else if (newPinInput != confirmPinInput) {
                            pinError = "PINs do not match"
                        } else {
                            viewModel.setPin(newPinInput)
                            showPinDialog = false
                            newPinInput = ""
                            confirmPinInput = ""
                            pinError = null
                            Toast.makeText(context, "Vault PIN saved successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884))
                ) {
                    Text("Save PIN", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialog = false
                    newPinInput = ""
                    confirmPinInput = ""
                    pinError = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Theme Picker Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose App Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ThemeOptionRow("System Default", themeMode == ThemeMode.SYSTEM) {
                        viewModel.setThemeMode(ThemeMode.SYSTEM)
                        showThemeDialog = false
                    }
                    ThemeOptionRow("Light Theme", themeMode == ThemeMode.LIGHT) {
                        viewModel.setThemeMode(ThemeMode.LIGHT)
                        showThemeDialog = false
                    }
                    ThemeOptionRow("Dark Theme", themeMode == ThemeMode.DARK) {
                        viewModel.setThemeMode(ThemeMode.DARK)
                        showThemeDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy & Security", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "WhatsApp Status Vault is built with a strict offline-first, local-only architecture by ShanPalia.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "1. Zero Cloud Sync: None of your photos, videos, notifications, or contact information are ever transmitted to any external server.",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "2. No Private Database Tampering: This application does not attempt to breach or alter WhatsApp's private encrypted database or system files.",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "3. Transparent Android APIs: Statuses are read exclusively via standard Android MediaStore/Storage Access Framework, and message logs are recorded via official NotificationListenerService with user consent.",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "4. Data Deletion: You retain full ownership and can delete individual items or clear all history at any time.",
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = "Settings & Privacy",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Manage system permissions, security, and preferences",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section: Permissions & App Status
        SettingsSectionCard(title = "Permissions & System Integrations") {
            // Notification Access
            SettingRow(
                icon = Icons.Default.Notifications,
                title = "Notification Access",
                subtitle = if (isNotifAccessGranted) "Enabled (Capturing active & removed messages)" else "Disabled (Tap to grant access)",
                statusBadge = if (isNotifAccessGranted) "Active" else "Action Needed",
                badgeColor = if (isNotifAccessGranted) Color(0xFF008069) else Color(0xFFD97706),
                onClick = { WhatsAppNotificationListener.openNotificationAccessSettings(context) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // WhatsApp Status Folder Access
            SettingRow(
                icon = Icons.Default.Folder,
                title = "WhatsApp Status Media Access",
                subtitle = if (!folderUri.isNullOrBlank()) "Folder Access Granted" else "Tap to choose Status folder",
                statusBadge = if (!folderUri.isNullOrBlank()) "Granted" else "Select",
                badgeColor = if (!folderUri.isNullOrBlank()) Color(0xFF008069) else Color(0xFF0288D1),
                onClick = { folderPickerLauncher.launch(null) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Installed WhatsApp Detection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("WhatsApp Messenger", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Package: com.whatsapp", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = if (isWhatsAppInstalled) "Installed" else "Not Installed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isWhatsAppInstalled) Color(0xFF008069) else Color(0xFF888888)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("WhatsApp Business", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Package: com.whatsapp.w4b", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = if (isWhatsAppBusinessInstalled) "Installed" else "Not Installed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isWhatsAppBusinessInstalled) Color(0xFF008069) else Color(0xFF888888)
                )
            }
        }

        // Section: App Preferences
        SettingsSectionCard(title = "App Preferences") {
            // Auto Refresh
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Scan Statuses", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Automatically scan available statuses on launch", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = autoRefresh,
                    onCheckedChange = { viewModel.setAutoRefresh(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF00A884))
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Theme Mode
            SettingRow(
                icon = Icons.Default.DarkMode,
                title = "App Theme",
                subtitle = when (themeMode) {
                    ThemeMode.SYSTEM -> "System Default"
                    ThemeMode.LIGHT -> "Light Theme"
                    ThemeMode.DARK -> "Dark Theme"
                },
                onClick = { showThemeDialog = true }
            )
        }

        // Section: Security & App Lock
        SettingsSectionCard(title = "Security & App Lock") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Vault PIN Lock", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(
                        text = if (isPinEnabled) "PIN protection enabled" else "Require PIN code to open vault",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isPinEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = { showPinDialog = true }) {
                            Text("Change", fontSize = 12.sp, color = Color(0xFF00A884))
                        }
                        TextButton(onClick = {
                            viewModel.disablePin()
                            Toast.makeText(context, "App lock disabled", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Disable", fontSize = 12.sp, color = Color(0xFFD32F2F))
                        }
                    }
                } else {
                    Button(
                        onClick = { showPinDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Set PIN", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        // Section: Updates & Information
        SettingsSectionCard(title = "Software Updates & About") {
            // Version Check Card
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Check for Updates", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Current Version: ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { viewModel.checkForUpdates(currentVersionCode = BuildConfig.VERSION_CODE, currentVersionName = BuildConfig.VERSION_NAME) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("check_updates_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Update",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Check",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Update Check Result Banner
                when (val result = updateState) {
                    is UpdateCheckResult.Checking -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connecting to update server...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    is UpdateCheckResult.UpToDate -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8F7F0), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF008069), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("You're up to date! (v${result.currentVersion})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF004D3C))
                        }
                    }
                    is UpdateCheckResult.UpdateAvailable -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("New Version ${result.updateInfo.versionName} Available!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                                if (result.updateInfo.releaseNotes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    result.updateInfo.releaseNotes.forEach { note ->
                                        Text("• $note", fontSize = 11.sp, color = Color(0xFF1565C0))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (result.updateInfo.apkUrl.isNotBlank()) {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.updateInfo.apkUrl))
                                            context.startActivity(intent)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Download Update APK", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                    is UpdateCheckResult.Error -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Unable to check for updates", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                Text(result.message, fontSize = 10.sp, color = Color(0xFFB71C1C))
                            }
                            TextButton(onClick = {
                                viewModel.checkForUpdates(currentVersionCode = BuildConfig.VERSION_CODE, currentVersionName = BuildConfig.VERSION_NAME)
                            }) {
                                Text("Retry", fontSize = 11.sp, color = Color(0xFFD32F2F))
                            }
                        }
                    }
                    else -> {}
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Privacy Policy Link
            SettingRow(
                icon = Icons.Default.Policy,
                title = "Privacy Policy",
                subtitle = "Read local-only privacy & terms",
                onClick = { showPrivacyDialog = true }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // About Developer
            SettingRow(
                icon = Icons.Default.Info,
                title = "Developer & Credits",
                subtitle = "ShanPalia • WhatsApp Status Vault",
                onClick = {}
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    statusBadge: String? = null,
    badgeColor: Color = Color(0xFF008069),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (statusBadge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = statusBadge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }
    }
}

@Composable
fun ThemeOptionRow(
    title: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onSelect)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, fontSize = 14.sp)
    }
}
