package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.data.model.DashboardStats
import com.example.data.model.NotificationItem
import com.example.data.model.SavedMediaItem
import com.example.data.model.StatusMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportGenerator(private val context: Context) {

    suspend fun generatePdfReport(
        stats: DashboardStats,
        statuses: List<StatusMediaItem>,
        savedMedia: List<SavedMediaItem>,
        notifications: List<NotificationItem>
    ): Result<File> = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val generatedAt = dateFormat.format(Date())

        try {
            val pageWidth = 595
            val pageHeight = 842
            var pageNumber = 1

            // --- PAGE 1: Overview & Summary ---
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // Header Background Banner
            paint.color = Color.parseColor("#00A884")
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 120f, paint)

            // Header Title
            paint.color = Color.WHITE
            paint.textSize = 22f
            paint.isFakeBoldText = true
            canvas.drawText("WhatsApp Status Vault", 36f, 50f, paint)

            // Subtitle
            paint.textSize = 12f
            paint.isFakeBoldText = false
            canvas.drawText("Comprehensive Activity & Media Export Report", 36f, 75f, paint)
            canvas.drawText("Developer: ShanPalia  |  Generated: $generatedAt", 36f, 98f, paint)

            var yPos = 150f

            // Section: Statistics Overview
            paint.color = Color.parseColor("#008069")
            paint.textSize = 15f
            paint.isFakeBoldText = true
            canvas.drawText("1. System & Media Counters Summary", 36f, yPos, paint)

            yPos += 20f
            paint.color = Color.parseColor("#E8F7F0")
            val statsCardRect = RectF(36f, yPos, (pageWidth - 36).toFloat(), yPos + 105f)
            canvas.drawRoundRect(statsCardRect, 10f, 10f, paint)

            paint.color = Color.parseColor("#111B21")
            paint.textSize = 11f
            paint.isFakeBoldText = false

            val col1 = 52f
            val col2 = 300f
            var statY = yPos + 26f

            canvas.drawText("• Available Real Statuses: ${stats.availableStatuses}", col1, statY, paint)
            canvas.drawText("• Saved Images in Vault: ${stats.savedImages}", col2, statY, paint)
            statY += 24f
            canvas.drawText("• Saved Videos in Vault: ${stats.savedVideos}", col1, statY, paint)
            canvas.drawText("• Captured Message Notifications: ${stats.capturedNotifications}", col2, statY, paint)
            statY += 24f
            canvas.drawText("• Notification Removed / Deleted: ${stats.removedNotifications}", col1, statY, paint)

            yPos += 135f

            // Section: Saved Media Inventory
            paint.color = Color.parseColor("#008069")
            paint.textSize = 15f
            paint.isFakeBoldText = true
            canvas.drawText("2. Saved Media Records (${savedMedia.size} items)", 36f, yPos, paint)

            yPos += 20f

            // Table Header
            paint.color = Color.parseColor("#E0E6E3")
            canvas.drawRect(36f, yPos, (pageWidth - 36).toFloat(), yPos + 22f, paint)
            paint.color = Color.parseColor("#111B21")
            paint.textSize = 10f
            paint.isFakeBoldText = true
            canvas.drawText("Type", 44f, yPos + 15f, paint)
            canvas.drawText("File Name", 95f, yPos + 15f, paint)
            canvas.drawText("Size", 360f, yPos + 15f, paint)
            canvas.drawText("Saved Date", 430f, yPos + 15f, paint)

            yPos += 25f
            paint.isFakeBoldText = false

            if (savedMedia.isEmpty()) {
                paint.color = Color.parseColor("#667781")
                canvas.drawText("No saved media records present in vault.", 44f, yPos + 16f, paint)
                yPos += 30f
            } else {
                for (item in savedMedia.take(12)) {
                    val sizeFormatted = formatFileSize(item.size)
                    val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.savedAt))

                    paint.color = if (item.isVideo) Color.parseColor("#E65100") else Color.parseColor("#2E7D32")
                    paint.isFakeBoldText = true
                    canvas.drawText(if (item.isVideo) "VIDEO" else "IMAGE", 44f, yPos + 14f, paint)

                    paint.color = Color.parseColor("#111B21")
                    paint.isFakeBoldText = false
                    val truncatedName = if (item.fileName.length > 38) item.fileName.take(35) + "..." else item.fileName
                    canvas.drawText(truncatedName, 95f, yPos + 14f, paint)
                    canvas.drawText(sizeFormatted, 360f, yPos + 14f, paint)
                    canvas.drawText(dateFormatted, 430f, yPos + 14f, paint)

                    paint.color = Color.parseColor("#EEEEEE")
                    canvas.drawLine(36f, yPos + 20f, (pageWidth - 36).toFloat(), yPos + 20f, paint)
                    yPos += 24f
                }
            }

            yPos += 15f

            // Section: Captured Statuses
            paint.color = Color.parseColor("#008069")
            paint.textSize = 15f
            paint.isFakeBoldText = true
            canvas.drawText("3. Available Status Media (${statuses.size} items detected)", 36f, yPos, paint)

            yPos += 20f
            paint.color = Color.parseColor("#E0E6E3")
            canvas.drawRect(36f, yPos, (pageWidth - 36).toFloat(), yPos + 22f, paint)
            paint.color = Color.parseColor("#111B21")
            paint.textSize = 10f
            paint.isFakeBoldText = true
            canvas.drawText("Type", 44f, yPos + 15f, paint)
            canvas.drawText("Status Identifier / File", 95f, yPos + 15f, paint)
            canvas.drawText("Source", 380f, yPos + 15f, paint)

            yPos += 25f
            paint.isFakeBoldText = false

            if (statuses.isEmpty()) {
                paint.color = Color.parseColor("#667781")
                canvas.drawText("No accessible WhatsApp status media found at scan time.", 44f, yPos + 16f, paint)
            } else {
                for (item in statuses.take(8)) {
                    paint.color = if (item.isVideo) Color.parseColor("#E65100") else Color.parseColor("#2E7D32")
                    paint.isFakeBoldText = true
                    canvas.drawText(if (item.isVideo) "VIDEO" else "IMAGE", 44f, yPos + 14f, paint)

                    paint.color = Color.parseColor("#111B21")
                    paint.isFakeBoldText = false
                    val truncatedName = if (item.name.length > 40) item.name.take(37) + "..." else item.name
                    canvas.drawText(truncatedName, 95f, yPos + 14f, paint)
                    val pkgName = if (item.packageSource.contains("w4b")) "WA Business" else "WhatsApp"
                    canvas.drawText(pkgName, 380f, yPos + 14f, paint)

                    paint.color = Color.parseColor("#EEEEEE")
                    canvas.drawLine(36f, yPos + 20f, (pageWidth - 36).toFloat(), yPos + 20f, paint)
                    yPos += 22f
                }
            }

            // Footer
            paint.color = Color.parseColor("#888888")
            paint.textSize = 9f
            canvas.drawText("Page $pageNumber  •  WhatsApp Status Vault  •  Developed by ShanPalia", 36f, (pageHeight - 20).toFloat(), paint)

            pdfDocument.finishPage(page)

            // --- PAGE 2: Notification History & Message Removals ---
            pageNumber = 2
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas

            // Page 2 Header Banner
            paint.color = Color.parseColor("#008069")
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 60f, paint)
            paint.color = Color.WHITE
            paint.textSize = 16f
            paint.isFakeBoldText = true
            canvas.drawText("WhatsApp Notification History & Message Logs", 36f, 38f, paint)

            yPos = 90f
            paint.color = Color.parseColor("#008069")
            paint.textSize = 14f
            canvas.drawText("4. Real Captured Notification Records (${notifications.size} total)", 36f, yPos, paint)

            yPos += 18f
            paint.color = Color.parseColor("#E0E6E3")
            canvas.drawRect(36f, yPos, (pageWidth - 36).toFloat(), yPos + 22f, paint)
            paint.color = Color.parseColor("#111B21")
            paint.textSize = 10f
            paint.isFakeBoldText = true
            canvas.drawText("Timestamp", 44f, yPos + 15f, paint)
            canvas.drawText("Sender / Group", 145f, yPos + 15f, paint)
            canvas.drawText("Message Snippet", 280f, yPos + 15f, paint)
            canvas.drawText("Status", 490f, yPos + 15f, paint)

            yPos += 26f
            paint.isFakeBoldText = false

            if (notifications.isEmpty()) {
                paint.color = Color.parseColor("#667781")
                canvas.drawText("No real WhatsApp notifications captured yet.", 44f, yPos + 16f, paint)
                canvas.drawText("(Ensure Notification Access is enabled in Settings to record notifications.)", 44f, yPos + 32f, paint)
            } else {
                for (notif in notifications.take(24)) {
                    val timeStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(notif.timestamp))

                    paint.color = Color.parseColor("#667781")
                    paint.textSize = 9f
                    canvas.drawText(timeStr, 44f, yPos + 12f, paint)

                    paint.color = Color.parseColor("#111B21")
                    paint.isFakeBoldText = true
                    val senderTruncated = if (notif.sender.length > 18) notif.sender.take(16) + ".." else notif.sender
                    canvas.drawText(senderTruncated, 145f, yPos + 12f, paint)

                    paint.isFakeBoldText = false
                    val msgTruncated = if (notif.messageText.length > 32) notif.messageText.take(29) + "..." else notif.messageText
                    canvas.drawText(msgTruncated, 280f, yPos + 12f, paint)

                    if (notif.isRemoved) {
                        paint.color = Color.parseColor("#D32F2F")
                        paint.isFakeBoldText = true
                        canvas.drawText("Removed", 490f, yPos + 12f, paint)
                    } else {
                        paint.color = Color.parseColor("#008069")
                        paint.isFakeBoldText = false
                        canvas.drawText("Active", 490f, yPos + 12f, paint)
                    }

                    paint.color = Color.parseColor("#EEEEEE")
                    canvas.drawLine(36f, yPos + 18f, (pageWidth - 36).toFloat(), yPos + 18f, paint)
                    yPos += 22f
                }
            }

            // Footer
            paint.color = Color.parseColor("#888888")
            paint.textSize = 9f
            canvas.drawText("Page $pageNumber  •  WhatsApp Status Vault  •  Developed by ShanPalia", 36f, (pageHeight - 20).toFloat(), paint)

            pdfDocument.finishPage(page)

            // Save PDF to file
            val reportsDir = File(context.getExternalFilesDir(null), "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()
            val reportFile = File(reportsDir, "Status_Vault_Report_${System.currentTimeMillis()}.pdf")

            FileOutputStream(reportFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            Result.success(reportFile)
        } catch (e: Exception) {
            pdfDocument.close()
            Result.failure(e)
        }
    }

    fun sharePdf(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "WhatsApp Status Vault Report - ShanPalia")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "Share PDF Report via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun openPdf(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(openIntent, "Open PDF Report with").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        return String.format(Locale.getDefault(), "%.1f %s", bytes / Math.pow(1024.0, index.toDouble()), units[index])
    }
}
