package com.example.ui.screens

import android.widget.Toast
import android.os.Build
import android.os.Environment
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.StorageAccess
import com.example.data.model.StatusMediaItem
import com.example.ui.components.FullscreenMediaViewer
import com.example.ui.components.StatusMediaGridItem
import com.example.ui.theme.BentoIndicator
import com.example.ui.theme.BentoMintContainer
import com.example.ui.theme.BentoMintText
import com.example.ui.theme.BentoOutline
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPurpleContainer
import com.example.ui.theme.BentoPurpleText
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun StatusSaverScreen(
    viewModel: MainViewModel
) {
    val statuses by viewModel.scannedStatuses.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshingStatuses.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val storageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        true
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedPackageFilter by remember { mutableStateOf("ALL") } // "ALL", "com.whatsapp", "com.whatsapp.w4b"
    var activeViewerItem by remember { mutableStateOf<StatusMediaItem?>(null) }

    if (activeViewerItem != null) {
        val item = activeViewerItem!!
        FullscreenMediaViewer(
            uri = item.uri,
            isVideo = item.isVideo,
            title = item.name,
            onBack = { activeViewerItem = null },
            onSave = {
                viewModel.saveStatus(item) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            onShare = {
                viewModel.shareStatus(item)
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("status_saver_screen")
    ) {
        // Header with Refresh & Folder Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Status Saver",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${statuses.size} statuses detected",
                    fontSize = 12.sp,
                    color = BentoTextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!storageGranted) {
                    Button(
                        onClick = { StorageAccess.openSettings(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BentoMintContainer,
                            contentColor = BentoMintText
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("grant_storage_access_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Storage access",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Allow Access", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(
                    onClick = { viewModel.refreshStatuses() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .testTag("refresh_statuses_btn")
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = BentoPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = BentoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (!storageGranted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = BentoMintContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Storage access required", fontWeight = FontWeight.Bold, color = BentoMintText)
                    Text(
                        "Allow access once. WhatsApp and WhatsApp Business .Statuses will then be detected automatically without selecting a folder.",
                        fontSize = 12.sp,
                        color = BentoMintText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { StorageAccess.openSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary)
                    ) {
                        Text("Allow Storage Access")
                    }
                }
            }
        }

        // Tabs: All, Images, Videos
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
                text = { Text("All (${statuses.size})", fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Images (${statuses.count { !it.isVideo }})", fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium) }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = { Text("Videos (${statuses.count { it.isVideo }})", fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Medium) }
            )
        }

        // Package Source Filter Bento Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                Pair("ALL", "All Apps"),
                Pair("com.whatsapp", "WhatsApp"),
                Pair("com.whatsapp.w4b", "WA Business")
            )
            filters.forEach { (pkg, label) ->
                val isSelected = selectedPackageFilter == pkg
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedPackageFilter = pkg },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) BentoPrimary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (isSelected) BentoPrimary else BentoOutline)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else BentoTextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Folder Access Guide Notice Banner (if statuses empty and no folder selected)
        if (statuses.isEmpty() && !storageGranted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F0)),
                border = BorderStroke(1.dp, BentoIndicator)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = BentoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Allow access to WhatsApp media",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00201C)
                        )
                        Text(
                            text = "Allow once to automatically detect WhatsApp and WhatsApp Business statuses",
                            fontSize = 11.sp,
                            color = Color(0xFF2E6356)
                        )
                    }
                    Button(
                        onClick = { StorageAccess.openSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Filtered Status List
        val filteredList = statuses.filter { item ->
            val matchesTab = when (selectedTabIndex) {
                1 -> !item.isVideo
                2 -> item.isVideo
                else -> true
            }
            val matchesPkg = when (selectedPackageFilter) {
                "com.whatsapp" -> item.packageSource == "com.whatsapp"
                "com.whatsapp.w4b" -> item.packageSource == "com.whatsapp.w4b"
                else -> true
            }
            matchesTab && matchesPkg
        }

        if (filteredList.isEmpty()) {
            // Real Empty State Bento Card
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
                                .background(BentoMintContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "No Statuses",
                                tint = BentoMintText,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No statuses found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Statuses you view on WhatsApp will appear here.\nMake sure you have viewed statuses on WhatsApp first, then tap Refresh.",
                            fontSize = 12.sp,
                            color = BentoTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.refreshStatuses() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refresh Now", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    StatusMediaGridItem(
                        item = item,
                        onClick = { activeViewerItem = item },
                        onSaveClick = {
                            viewModel.saveStatus(item) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onShareClick = {
                            viewModel.shareStatus(item)
                        }
                    )
                }
            }
        }
    }
}

