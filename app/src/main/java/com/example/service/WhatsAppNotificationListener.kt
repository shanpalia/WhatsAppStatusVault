package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.example.data.db.AppDatabase
import com.example.data.db.NotificationHistoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WhatsAppNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        if (packageName != PACKAGE_WHATSAPP && packageName != PACKAGE_WHATSAPP_BUSINESS) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Extract the actual notification content. WhatsApp often uses
        // Notification.MessagingStyle, where EXTRA_TEXT only contains a
        // summary such as "3 new messages". Prefer the individual messages.
        val titleCharSequence = extras.getCharSequence(Notification.EXTRA_TITLE)
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
        val textCharSequence = extras.getCharSequence(Notification.EXTRA_TEXT)
        val bigTextCharSequence = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
        val subTextCharSequence = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)

        val messagingLines = mutableListOf<String>()

        try {
            val messageBundles = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (messageBundles != null) {
                val messages = Notification.MessagingStyle.Message.getMessagesFromBundleArray(messageBundles)
                for (message in messages) {
                    val text = message.text?.toString()?.trim().orEmpty()
                    if (text.isNotBlank()) {
                        messagingLines += text
                    }
                }
            }
        } catch (_: Throwable) {
            // Some Android/WhatsApp notification formats do not expose MessagingStyle.
        }

        // Some WhatsApp builds expose individual lines instead of EXTRA_MESSAGES.
        if (messagingLines.isEmpty()) {
            try {
                val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                if (lines != null) {
                    lines.mapNotNull { it?.toString()?.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .forEach { messagingLines += it }
                }
            } catch (_: Throwable) {
                // Ignore unsupported notification formats.
            }
        }

        val sender = (
            conversationTitle?.toString()?.trim()
                ?: titleCharSequence?.toString()?.trim()
                ?: ""
            )

        val fallbackText = (
            bigTextCharSequence
                ?: textCharSequence
                ?: subTextCharSequence
            )?.toString()?.trim().orEmpty()

        val messageText = if (messagingLines.isNotEmpty()) {
            messagingLines.joinToString("\n")
        } else {
            fallbackText
        }

        // WhatsApp can post a deletion/retraction notification after a message
        // was captured. In that case, mark the most recently captured message
        // from the same conversation as removed. We never invent the deleted
        // message text; only previously captured text is shown.
        val deletionText = messageText.trim()
        val looksLikeDeletedMessage =
            deletionText.contains("message was deleted", ignoreCase = true) ||
            deletionText.contains("this message was deleted", ignoreCase = true) ||
            deletionText.contains("deleted this message", ignoreCase = true) ||
            deletionText.contains("message deleted", ignoreCase = true) ||
            deletionText.contains("msg deleted", ignoreCase = true)

        val senderForDeletion = (
            conversationTitle?.toString()?.trim()
                ?: titleCharSequence?.toString()?.trim()
                ?: ""
            )

        if (looksLikeDeletedMessage && senderForDeletion.isNotBlank()) {
            serviceScope.launch {
                try {
                    val db = AppDatabase.getDatabase(applicationContext)
                    db.notificationDao().markLatestFromSenderRemoved(
                        packageSource = packageName,
                        sender = senderForDeletion
                    )
                } catch (_: Exception) {
                    // Ignore deletion-marker update failures.
                }
            }
            // Do not create a fake notification entry for the deletion marker.
            return
        }

        // Skip internal/empty or service notifications like "WhatsApp Web is active" or backup notifications.
        if (sender.isBlank() && messageText.isBlank()) return
        if (sender.equals("WhatsApp", ignoreCase = true) && messageText.contains("Web", ignoreCase = true)) return
        if (messageText.contains("Checking for new messages", ignoreCase = true)) return
        if (messageText.contains("Backup in progress", ignoreCase = true)) return

        val isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false) || sender.contains(":")

        val entity = NotificationHistoryEntity(
            notificationKey = sbn.key ?: "${packageName}_${sbn.id}_${sbn.postTime}",
            packageSource = packageName,
            sender = if (sender.isNotBlank()) sender else "WhatsApp Contact",
            messageText = if (messageText.isNotBlank()) messageText else "Original message was not available in the notification.",
            timestamp = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis(),
            isRemoved = false,
            isGroup = isGroup,
            tag = sbn.tag
        )

        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                db.notificationDao().insert(entity)
            } catch (e: Exception) {
                // Database insertion exception handled safely
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        if (packageName != PACKAGE_WHATSAPP && packageName != PACKAGE_WHATSAPP_BUSINESS) {
            return
        }

        val key = sbn.key ?: return

        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                db.notificationDao().markRemoved(key, System.currentTimeMillis())
            } catch (e: Exception) {
                // Ignore removal update failure
            }
        }
    }

    companion object {
        const val PACKAGE_WHATSAPP = "com.whatsapp"
        const val PACKAGE_WHATSAPP_BUSINESS = "com.whatsapp.w4b"

        fun isNotificationAccessGranted(context: Context): Boolean {
            val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
            if (enabledListeners.contains(context.packageName)) {
                return true
            }
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            if (!flat.isNullOrEmpty()) {
                val names = flat.split(":").map { it.trim() }
                for (name in names) {
                    val cn = ComponentName.unflattenFromString(name)
                    if (cn != null && cn.packageName == context.packageName) {
                        return true
                    }
                }
            }
            return false
        }

        fun openNotificationAccessSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            }
        }
    }
}
