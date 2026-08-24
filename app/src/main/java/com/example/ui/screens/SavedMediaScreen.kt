package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SavedMediaItem
import com.example.ui.components.FullscreenMediaViewer
import com.example.ui.components.SavedMediaGridItem
import com.example.ui.theme.BentoMintContainer
import com.example.ui.theme.BentoMintText
import com.example.ui.theme.BentoOutline
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPurpleContainer
import com.example.ui.theme.BentoPurpleText
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SavedMediaScreen(
    viewModel: MainViewModel
) {
    val savedImages by viewModel.savedImages.collectAsStateWithLifecycle()
    val savedVideos by viewModel.savedVideos.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var sortNewestFirst by remember { mutableStateOf(true) }
    var itemToDelete by remember { mutableStateOf<SavedMediaItem?>(null) }
    var activeViewerItem by remember { mutableStateOf<SavedMediaItem?>(null) }

    if (itemToDelete != null) {
        val item = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Saved Media", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${item.fileName}' from your vault?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSavedMedia(item) { success ->
                            if (success) {
                                Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
                            }
                        }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (activeViewerItem != null) {
        val item = activeViewerItem!!
        FullscreenMediaViewer(
            uri = item.uri,
            isVideo = item.isVideo,
            title = item.fileName,
            onBack = { activeViewerItem = null },
            onShare = {
                viewModel.shareSavedMedia(item)
            },
            onDelete = {
                itemToDelete = item
                activeViewerItem = null
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("saved_media_screen")
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
                    text = "Saved Vault",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${savedImages.size + savedVideos.size} files stored locally",
                    fontSize = 12.sp,
                    color = BentoTextSecondary
                )
            }

            IconButton(
                onClick = { sortNewestFirst = !sortNewestFirst },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "Sort",
                    tint = BentoPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Tabs: Images, Videos
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
                text = { Text("Images (${savedImages.size})", fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Videos (${savedVideos.size})", fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium) }
            )
        }

        val rawList = if (selectedTabIndex == 0) savedImages else savedVideos
        val displayedList = if (sortNewestFirst) {
            rawList.sortedByDescending { it.savedAt }
        } else {
            rawList.sortedBy { it.savedAt }
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
                                .background(BentoMintContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "No Saved Media",
                                tint = BentoMintText,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTabIndex == 0) "No saved images" else "No saved videos",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Statuses you save from the Status Saver tab will be permanently stored here in your phone storage and Gallery.",
                            fontSize = 12.sp,
                            color = BentoTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
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
                items(displayedList, key = { it.id }) { item ->
                    SavedMediaGridItem(
                        item = item,
                        onClick = { activeViewerItem = item },
                        onDeleteClick = { itemToDelete = item },
                        onShareClick = { viewModel.shareSavedMedia(item) }
                    )
                }
            }
        }
    }
}

