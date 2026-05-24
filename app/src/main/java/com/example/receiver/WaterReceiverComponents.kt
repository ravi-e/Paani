package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.DrinkLog
import com.example.data.ReminderSettings
import com.example.data.WaterDatabase
import com.example.data.WaterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WaterReminderScheduler {
    private const val TAG = "WaterReminderScheduler"
    const val WORK_NAME = "WaterReminderWork"

    fun scheduleNextReminder(context: Context, settings: ReminderSettings) {
        val workManager = WorkManager.getInstance(context)

        // Cancel previous work request to avoid duplicates
        workManager.cancelUniqueWork(WORK_NAME)

        val triggerAtMillis = if (settings.snoozeTimeMillis > System.currentTimeMillis()) {
            settings.snoozeTimeMillis
        } else {
            calculateNextAlarmTime(settings)
        }

        val delayMillis = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)

        val workRequest = OneTimeWorkRequestBuilder<WaterReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag(WORK_NAME)
            .build()

        workManager.enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        Log.d(TAG, "Scheduled unique WorkManager worker with delay: ${delayMillis / 1000}s (Trigger at: ${java.util.Date(triggerAtMillis)})")
    }

    fun cancelReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun calculateNextAlarmTime(settings: ReminderSettings, fromTimeMillis: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = fromTimeMillis }
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMin = calendar.get(Calendar.MINUTE)

        val currentDayMins = currentHour * 60 + currentMin
        val startDayMins = settings.startTimeHour * 60 + settings.startTimeMinute
        val endDayMins = settings.endTimeHour * 60 + settings.endTimeMinute
        val intervalMins = settings.intervalMinutes

        val nextAlarmCalendar = Calendar.getInstance().apply { timeInMillis = fromTimeMillis }

        if (currentDayMins >= endDayMins) {
            // After today's active end time window. Schedule for tomorrow's start time.
            nextAlarmCalendar.add(Calendar.DAY_OF_YEAR, 1)
            nextAlarmCalendar.set(Calendar.HOUR_OF_DAY, settings.startTimeHour)
            nextAlarmCalendar.set(Calendar.MINUTE, settings.startTimeMinute)
            nextAlarmCalendar.set(Calendar.SECOND, 0)
            nextAlarmCalendar.set(Calendar.MILLISECOND, 0)
        } else if (currentDayMins < startDayMins) {
            // Before today's active start time window. Schedule for today's start time.
            nextAlarmCalendar.set(Calendar.HOUR_OF_DAY, settings.startTimeHour)
            nextAlarmCalendar.set(Calendar.MINUTE, settings.startTimeMinute)
            nextAlarmCalendar.set(Calendar.SECOND, 0)
            nextAlarmCalendar.set(Calendar.MILLISECOND, 0)
        } else {
            // Inside the active window. Schedule relative to current time + interval.
            val nextScheduledDayMins = currentDayMins + intervalMins
            if (nextScheduledDayMins >= endDayMins) {
                // If it pushes past today's end time, wrap to tomorrow's start time
                nextAlarmCalendar.add(Calendar.DAY_OF_YEAR, 1)
                nextAlarmCalendar.set(Calendar.HOUR_OF_DAY, settings.startTimeHour)
                nextAlarmCalendar.set(Calendar.MINUTE, settings.startTimeMinute)
                nextAlarmCalendar.set(Calendar.SECOND, 0)
                nextAlarmCalendar.set(Calendar.MILLISECOND, 0)
            } else {
                nextAlarmCalendar.add(Calendar.MINUTE, intervalMins)
                nextAlarmCalendar.set(Calendar.SECOND, 0)
                nextAlarmCalendar.set(Calendar.MILLISECOND, 0)
            }
        }
        return nextAlarmCalendar.timeInMillis
    }
}

class WaterReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "WaterReminderWorker triggered background task.")
        val db = WaterDatabase.getDatabase(applicationContext)
        val repository = WaterRepository(db.waterDao())

        val settings = repository.getSettingsDirect()

        if (isWithinActiveHours(settings)) {
            WaterAlarmService.start(applicationContext)

            // Optional voice cue — fires alongside the notification
            if (settings.voiceReminderEnabled) {
                val firstName = settings.userName.trim().split(" ").firstOrNull()
                    ?.replaceFirstChar { it.uppercase() } ?: ""
                val cue = if (firstName.isNotBlank()) {
                    "Time to drink some water, $firstName!"
                } else {
                    "Time to drink some water!"
                }
                WaterTtsHelper.speak(applicationContext, cue)
            }
        } else {
            Log.d(TAG, "Outside senior active window. Notification skipped.")
        }

        // Clear current snooze as alarm has triggered. Reset to next regular interval alarm.
        val updatedSettings = settings.copy(
            snoozeTimeMillis = 0L,
            nextReminderTimeMillis = WaterReminderScheduler.calculateNextAlarmTime(settings)
        )
        repository.updateSettings(updatedSettings)

        // Schedule the subsequent regular reminder
        WaterReminderScheduler.scheduleNextReminder(applicationContext, updatedSettings)

        return Result.success()
    }

    private fun isWithinActiveHours(settings: ReminderSettings): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMin = calendar.get(Calendar.MINUTE)

        val currentMins = currentHour * 60 + currentMin
        val startMins = settings.startTimeHour * 60 + settings.startTimeMinute
        val endMins = settings.endTimeHour * 60 + settings.endTimeMinute

        return currentMins in startMins..endMins
    }

    private fun showReminderNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "water_reminders_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Water Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent friendly hydration reminders for seniors"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 300, 800)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val drankIntent = Intent(context, WaterActionReceiver::class.java).apply {
            action = "com.example.ACTION_RECORD_DRINK"
        }
        val drankPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            drankIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, WaterActionReceiver::class.java).apply {
            action = "com.example.ACTION_SNOOZE_5"
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Drink Glass of Water! \uD83D\uDCA7")
            .setContentText("Stay healthy and hydrated today. Clear screen simple tap!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 800, 300, 800))
            .setContentIntent(appPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.checkbox_on_background, "✅ DRANK WATER NOW", drankPendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "⏲️ SNOOZE 5 MINS", snoozePendingIntent)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    companion object {
        private const val TAG = "WaterReminderWorker"
        const val NOTIFICATION_ID = 2001
    }
}

class WaterActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("WaterActionReceiver", "Direct action triggered: $action")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(WaterReminderWorker.NOTIFICATION_ID)
        notificationManager.cancel(WaterAlarmService.NOTIFICATION_ID)
        WaterAlarmService.stop(context)

        val db = WaterDatabase.getDatabase(context)
        val repository = WaterRepository(db.waterDao())

        CoroutineScope(Dispatchers.IO).launch {
            val settings = repository.getSettingsDirect()

            when (action) {
                "com.example.ACTION_RECORD_DRINK" -> {
                    repository.insertLog(DrinkLog())
                    val updatedSettings = settings.copy(
                        snoozeTimeMillis = 0L,
                        nextReminderTimeMillis = WaterReminderScheduler.calculateNextAlarmTime(settings)
                    )
                    repository.updateSettings(updatedSettings)
                    WaterReminderScheduler.scheduleNextReminder(context, updatedSettings)
                    Log.d("WaterActionReceiver", "Direct Water log saved.")
                }
                "com.example.ACTION_SNOOZE_5" -> {
                    val snoozeMillis = System.currentTimeMillis() + (5 * 60 * 1000)
                    val updatedSettings = settings.copy(
                        snoozeTimeMillis = snoozeMillis,
                        nextReminderTimeMillis = snoozeMillis
                    )
                    repository.updateSettings(updatedSettings)
                    WaterReminderScheduler.scheduleNextReminder(context, updatedSettings)
                    Log.d("WaterActionReceiver", "Snoozed 5 mins registered.")
                }
            }
        }
    }
}
