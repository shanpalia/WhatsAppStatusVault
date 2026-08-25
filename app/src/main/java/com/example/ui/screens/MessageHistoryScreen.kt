package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NotificationItem
import com.example.service.WhatsAppNotificationListener
import com.example.ui.theme.BentoAmberContainer
import com.example.ui.theme.BentoAmberText
import com.example.ui.theme.BentoMintContainer
import com.example.ui.theme.BentoMintText
import com.example.ui.theme.BentoOutline
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPurpleContainer
import com.example.ui.theme.BentoPurpleText
import com.example.ui.theme.BentoRedContainer
import com.example.ui.theme.BentoRedText
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageHistoryScreen(
    viewModel: MainViewModel,
    openRemovedOnly: Boolean = false,
    onConsumedRemovedRoute: () -> Unit = {}
) {
    val allNotifications by viewModel.allNotifications.collectAsStateWithLifecycle()
    val removedNotifications by viewModel.removedNotifications.collectAsStateWithLifecycle()
    val isNotifAccessGranted by viewModel.isNotificationAccessGranted.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchNotificationQuery.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTabIndex by remember { mutableIntStateOf(if (openRemovedOnly) 2 else 0) }
    var selectedNotifDetail by remember { mutableStateOf<NotificationItem?>(null) }
    var selectedChatSender by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(openRemovedOnly) {
        if (openRemovedOnly) {
            selectedTabIndex = 2
            onConsumedRemovedRoute()
        }
        viewModel.checkNotificationAccess()
    }

    if (selectedChatSender != null) {
        val sender = selectedChatSender!!
        val chatItems = allNotifications
            .filter { it.sender.trim().equals(sender.trim(), ignoreCase = true) }
            .sortedBy { it.timestamp }

        BackHandler {
            selectedChatSender = null
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedChatSender = null }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sender.ifBlank { "Unknown sender" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${chatItems.size} messages • ${chatItems.count { it.isRemoved }} deleted",
                        fontSize = 12.sp,
                        color = BentoTextSecondary
                    )
                }
            }

            if (chatItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No captured messages for this chat.",
                        color = BentoTextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(chatItems, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.isRemoved)
                                    BentoRedContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (item.isRemoved) BentoRedText.copy(alpha = 0.25f)
                                else BentoOutline
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (item.isRemoved) "DELETED MESSAGE" else "MESSAGE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isRemoved) BentoRedText else BentoPrimary
                                    )
                                    Text(
                                        text = SimpleDateFormat(
                                            "dd MMM yyyy, HH:mm:ss",
                                            Locale.getDefault()
                                        ).format(Date(item.timestamp)),
                                        fontSize = 10.sp,
                                        color = BentoTextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = item.messageText.ifBlank {
                                        if (item.isRemoved)
                                            "(Message text was not included in the notification)"
                                        else
                                            "(No text captured)"
                                    },
                                    fontSize = 15.sp,
                                    lineHeight = 21.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (item.isRemoved && item.removedTimestamp != null) {
                                    Spacer(modifier = Modifier.height(7.dp))
                                    Text(
                                        text = "Deleted at: " + SimpleDateFormat(
                                            "dd MMM yyyy, HH:mm:ss",
                                            Locale.getDefault()
                                        ).format(Date(item.removedTimestamp)),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = BentoRedText
                                    )
                                }

                                if (item.packageSource.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "App: ${item.packageSource}",
                                        fontSize = 10.sp,
                                        color = BentoTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedNotifDetail != null) {
        val notif = selectedNotifDetail!!
        AlertDialog(
            onDismissRequest = { selectedNotifDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (notif.isGroup) Icons.Default.Group else Icons.Default.Person,
                        contentDescription = null,
                        tint = BentoPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(notif.sender, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Message Content:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextSecondary
                    )
                    Text(
                        text = notif.messageText,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Captured: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(notif.timestamp))}",
                        fontSize = 11.sp,
                        color = BentoTextSecondary
                    )
                    if (notif.isRemoved && notif.removedTimestamp != null) {
                        Text(
                            text = "Message removed • ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(notif.removedTimestamp))}",
                            fontSize = 11.sp,
                            color = BentoRedText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "App: ${if (notif.packageSource.contains("w4b")) "WhatsApp Business" else "WhatsApp Messenger"}",
                        fontSize = 11.sp,
                        color = BentoTextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedNotifDetail = null }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("message_history_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Message History",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${allNotifications.size} notifications captured",
                    fontSize = 12.sp,
                    color = BentoTextSecondary
                )
            }


        }

        // Notification Access Warning (if disabled)
        if (!isNotifAccessGranted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BentoAmberContainer),
                border = BorderStroke(1.dp, BentoAmberText.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "Notice",
                        tint = BentoAmberText,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notification Access Disabled",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoAmberText
                        )
                        Text(
                            text = "Enable access to capture WhatsApp notifications.",
                            fontSize = 11.sp,
                            color = BentoAmberText.copy(alpha = 0.8f)
                        )
                    }
                    Button(
                        onClick = {
                            WhatsAppNotificationListener.openNotificationAccessSettings(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BentoAmberText),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Bento Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchNotificationQuery(it) },
            placeholder = { Text("Search sender or message text...", fontSize = 13.sp, color = BentoTextSecondary) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = BentoPrimary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchNotificationQuery("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = BentoTextSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BentoPrimary,
                unfocusedBorderColor = BentoOutline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("notification_search_input")
        )

        // Tabs: All, Recent, Removed
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = BentoPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = BentoPrimary
                )
            }
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("All (${allNotifications.size})", fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Recent", fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium) }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = { Text("Removed (${removedNotifications.size})", fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Medium) }
            )
        }

        val baseList = when (selectedTabIndex) {
            1 -> allNotifications.take(30)
            2 -> removedNotifications
            else -> allNotifications
        }

        val displayedList = if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.sender.contains(searchQuery, ignoreCase = true) ||
                it.messageText.contains(searchQuery, ignoreCase = true)
            }
        }

        if (displayedList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, BentoOutline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(BentoPurpleContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "No Notifications",
                                tint = BentoPurpleText,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTabIndex == 2) "No removed notifications" else "No notifications captured",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedTabIndex == 2)
                                "When a WhatsApp message notification is cleared, it will be highlighted here."
                            else
                                "Real incoming WhatsApp notifications will be automatically captured and preserved here.",
                            fontSize = 12.sp,
                            color = BentoTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayedList, key = { it.id }) { notif ->
                    NotificationCard(
                        item = notif,
                        onClick = {
                            selectedChatSender = notif.sender.trim().ifBlank { "Unknown sender" }
                        },
                        onDelete = { /* Individual deletion disabled; history is preserved. */ }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    item: NotificationItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .testTag("notif_card_${item.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (item.isRemoved) BentoRedContainer else BentoOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.isRemoved) BentoRedContainer else BentoMintContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.isGroup) Icons.Default.Group else Icons.Default.Person,
                            contentDescription = "Sender",
                            tint = if (item.isRemoved) BentoRedText else BentoMintText,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = item.sender,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = if (item.packageSource.contains("w4b")) "WhatsApp Business" else "WhatsApp",
                            fontSize = 10.sp,
                            color = BentoTextSecondary
                        )
                    }
                }

                Text(
                    text = SimpleDateFormat("HH:mm • dd MMM", Locale.getDefault()).format(Date(item.timestamp)),
                    fontSize = 11.sp,
                    color = BentoTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.messageText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge
                if (item.isRemoved) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BentoRedContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Deleted by Sender",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoRedText
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BentoMintContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Active Log",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoMintText
                        )
                    }
                }

                // Captured history is permanent; there is no per-message delete action.
            }
        }
    }
}

