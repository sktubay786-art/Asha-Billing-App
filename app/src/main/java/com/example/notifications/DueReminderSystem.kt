package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.formatInr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Serializable
data class BillDueReminder(
    val billId: String,
    val billNo: String,
    val customerName: String,
    val customerPhone: String,
    val dueAmount: Double,
    val dueDateIso: String,
    val hour: Int = 9,
    val minute: Int = 0,
    val triggerAtMillis: Long = 0L,
    val enabled: Boolean = true
)

class DueReminderStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _reminders = MutableStateFlow(loadAll())
    val reminders: StateFlow<List<BillDueReminder>> = _reminders.asStateFlow()

    fun loadAll(): List<BillDueReminder> {
        val raw = prefs.getString(KEY_REMINDERS, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<BillDueReminder>>(raw) }.getOrDefault(emptyList())
    }

    fun getByBillId(billId: String): BillDueReminder? =
        _reminders.value.firstOrNull { it.billId == billId }

    fun upsert(reminder: BillDueReminder) {
        val updated = _reminders.value.filterNot { it.billId == reminder.billId } + reminder
        save(updated)
    }

    fun remove(billId: String) {
        val updated = _reminders.value.filterNot { it.billId == billId }
        save(updated)
    }

    private fun save(list: List<BillDueReminder>) {
        _reminders.value = list
        prefs.edit().putString(KEY_REMINDERS, json.encodeToString(list)).apply()
    }

    companion object {
        private const val PREFS_NAME = "asha_due_reminders"
        private const val KEY_REMINDERS = "reminders_json"
    }
}

object DueReminderScheduler {
    const val CHANNEL_ID = "asha_due_reminders"
    const val ACTION_TRIGGER_DUE_REMINDER = "com.example.notifications.TRIGGER_DUE_REMINDER"
    const val ACTION_DISMISS_REMINDER = "com.example.notifications.DISMISS_REMINDER"
    const val EXTRA_BILL_ID = "extra_bill_id"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bill Due Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Persistent notifications for scheduled bill due dates"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun computeTriggerMillis(dueDateIso: String, hour: Int = 9, minute: Int = 0): Long {
        val date = runCatching { LocalDate.parse(dueDateIso) }.getOrElse { LocalDate.now() }
        val scheduled = LocalDateTime.of(date, LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val now = System.currentTimeMillis()
        // If user selected today and time has passed, fire shortly (4 seconds) so persistent notification shows reliably.
        return if (scheduled <= now) now + 4_000L else scheduled
    }

    fun scheduleReminder(context: Context, reminder: BillDueReminder) {
        val appContext = context.applicationContext
        ensureChannel(appContext)
        val triggerMillis = if (reminder.triggerAtMillis > 0L) {
            reminder.triggerAtMillis
        } else {
            computeTriggerMillis(reminder.dueDateIso, reminder.hour, reminder.minute)
        }
        val finalized = reminder.copy(triggerAtMillis = triggerMillis, enabled = true)
        DueReminderStore(appContext).upsert(finalized)

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = triggerPendingIntent(appContext, finalized.billId)

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
        }
    }

    fun cancelReminder(context: Context, billId: String) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.cancel(triggerPendingIntent(appContext, billId))
        NotificationManagerCompat.from(appContext).cancel(notificationIdFor(billId))
        DueReminderStore(appContext).remove(billId)
    }

    fun rescheduleAll(context: Context) {
        val appContext = context.applicationContext
        val store = DueReminderStore(appContext)
        val now = System.currentTimeMillis()
        store.loadAll().filter { it.enabled }.forEach { reminder ->
            val adjusted = if (reminder.triggerAtMillis <= now) {
                reminder.copy(triggerAtMillis = now + 3_000L)
            } else {
                reminder
            }
            scheduleReminder(appContext, adjusted)
        }
    }

    fun showPersistentNotification(context: Context, reminder: BillDueReminder) {
        val appContext = context.applicationContext
        ensureChannel(appContext)
        if (!hasNotificationPermission(appContext)) return

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_BILL_ID, reminder.billId)
        }
        val openPi = PendingIntent.getActivity(
            appContext,
            notificationIdFor(reminder.billId),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(appContext, DueReminderReceiver::class.java).apply {
            action = ACTION_DISMISS_REMINDER
            putExtra(EXTRA_BILL_ID, reminder.billId)
        }
        val dismissPi = PendingIntent.getBroadcast(
            appContext,
            notificationIdFor(reminder.billId) + 10_000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val reminderMessage = "Hello ${reminder.customerName}, gentle payment reminder for Bill ${reminder.billNo}: ${formatInr(reminder.dueAmount)} due (${reminder.dueDateIso})."
        val shareIntent = buildReminderActionIntent(reminder.customerPhone, reminderMessage)
        val sharePi = PendingIntent.getActivity(
            appContext,
            notificationIdFor(reminder.billId) + 20_000,
            shareIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = runCatching {
            BitmapFactory.decodeResource(appContext.resources, R.drawable.ic_asha_logo)
        }.getOrNull()

        val title = "Bill Due: ${reminder.customerName}"
        val body = "${reminder.billNo} · ${formatInr(reminder.dueAmount)} due on ${reminder.dueDateIso}"

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bill)
            .apply { if (largeIcon != null) setLargeIcon(largeIcon) }
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body\nTap to view invoice or send reminder."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openPi)
            .addAction(0, "Send Reminder", sharePi)
            .addAction(0, "Dismiss", dismissPi)
            .build()

        runCatching {
            NotificationManagerCompat.from(appContext).notify(notificationIdFor(reminder.billId), notification)
        }
    }

    fun isDueTodayOrEarlier(dueDateIso: String): Boolean {
        val date = runCatching { LocalDate.parse(dueDateIso) }.getOrNull() ?: return false
        return !date.isAfter(LocalDate.now())
    }

    private fun buildReminderActionIntent(phoneRaw: String, message: String): Intent {
        val digits = phoneRaw.replace(Regex("""\D"""), "")
        return if (digits.isNotBlank()) {
            val encoded = runCatching { URLEncoder.encode(message, "UTF-8") }.getOrDefault(message)
            Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits?text=$encoded")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                },
                "Send Bill Reminder"
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    private fun triggerPendingIntent(context: Context, billId: String): PendingIntent {
        val intent = Intent(context, DueReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_DUE_REMINDER
            putExtra(EXTRA_BILL_ID, billId)
        }
        return PendingIntent.getBroadcast(
            context,
            notificationIdFor(billId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun notificationIdFor(billId: String): Int = (billId.hashCode() and 0x0FFF_FFFF).coerceAtLeast(101)
}

class DueReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val billId = intent.getStringExtra(DueReminderScheduler.EXTRA_BILL_ID) ?: return
        when (intent.action) {
            DueReminderScheduler.ACTION_DISMISS_REMINDER -> {
                NotificationManagerCompat.from(context.applicationContext)
                    .cancel(DueReminderScheduler.notificationIdFor(billId))
            }
            DueReminderScheduler.ACTION_TRIGGER_DUE_REMINDER -> {
                val store = DueReminderStore(context.applicationContext)
                val reminder = store.getByBillId(billId) ?: return
                DueReminderScheduler.showPersistentNotification(context.applicationContext, reminder)
            }
        }
    }
}

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            DueReminderScheduler.rescheduleAll(context.applicationContext)
        }
    }
}
