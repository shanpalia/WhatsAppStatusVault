package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.BentoOutline
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.viewmodel.MainViewModel
import java.io.File

@Composable
fun ReportsScreen(viewModel: MainViewModel) {
    val notifications by viewModel.allNotifications.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingReport.collectAsStateWithLifecycle()
    val generatedFile by viewModel.generatedReportFile.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val chats = remember(notifications) { notifications.map { it.sender.trim() }.filter { it.isNotBlank() }.distinct().sorted() }
    var selected by remember(chats) { mutableStateOf(chats.toSet()) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("reports_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("PDF Report", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Select the chats you want in the PDF. Only real captured records are included.", fontSize = 12.sp, color = BentoTextSecondary)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BentoOutline)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Chat list", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { selected = chats.toSet() }) { Text("All", fontSize = 11.sp) }
                        OutlinedButton(onClick = { selected = emptySet() }) { Text("None", fontSize = 11.sp) }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                if (chats.isEmpty()) {
                    Text("No chats captured yet. Enable Notification Access and receive a WhatsApp message first.", fontSize = 12.sp, color = BentoTextSecondary)
                } else {
                    chats.forEach { chat ->
                        Row(
                            Modifier.fillMaxWidth().testTag("chat_$chat"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = selected.contains(chat), onCheckedChange = { checked ->
                                selected = if (checked) selected + chat else selected - chat
                            })
                            Text(chat, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Text("${selected.size} chat(s) selected • ${notifications.count { selected.contains(it.sender) }} records", fontSize = 12.sp, color = BentoTextSecondary)

        Button(
            onClick = {
                if (selected.isEmpty()) {
                    Toast.makeText(context, "Select at least one chat", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.generatePdfReport(selected) { file ->
                        Toast.makeText(context, if (file != null) "PDF report generated" else "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = !isGenerating && selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(Modifier.width(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Generating...", color = Color.White, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.PictureAsPdf, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Generate Selected Chats PDF", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        generatedFile?.let { file: File ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F7F0)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PDF Ready", fontWeight = FontWeight.Bold, color = Color(0xFF00695C))
                    Text(file.name, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.openPdfReport(file) }) {
                            Icon(Icons.Default.OpenInNew, null); Spacer(Modifier.width(4.dp)); Text("Open")
                        }
                        OutlinedButton(onClick = { viewModel.sharePdfReport(file) }) {
                            Icon(Icons.Default.Share, null); Spacer(Modifier.width(4.dp)); Text("Share")
                        }
                    }
                }
            }
        }
    }
}
