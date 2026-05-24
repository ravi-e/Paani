package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.WaterDatabase
import com.example.data.WaterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WaterAlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "WaterAlarmService created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "WaterAlarmService started.")

        // Start Foreground immediately with a placeholder notification to satisfy OS requirements
        // (on Android 8.0+, startForeground must be called within 5 seconds of startForegroundService)
        val channelId = createNotificationChannel()
        val notification = buildForegroundNotification(channelId, "Drink water! 💧", "Stay healthy and hydrated today.")
        startForeground(NOTIFICATION_ID, notification)

        // Asynchronously load the setting and play the selected/default sound
        CoroutineScope(Dispatchers.IO).launch {
            val db = WaterDatabase.getDatabase(applicationContext)
            val repository = WaterRepository(db.waterDao())
            val settings = repository.getSettingsDirect()
            
            // Build a more personalized notification based on user's name
            val name = settings.userName.trim().split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: ""
            val title = if (name.isNotBlank()) "Drink water, $name! 💧" else "Drink water! 💧"
            val text = "It's time to drink a glass of water."
            
            // Update notification
            val updatedNotification = buildForegroundNotification(channelId, title, text)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, updatedNotification)

            // Play sound
            playSound(settings.alarmSoundUri)
        }

        return START_NOT_STICKY
    }

    private fun playSound(uriString: String) {
        // Release existing media player if any
        stopSound()

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
            }

            var uriLoaded = false
            if (uriString.isNotBlank()) {
                try {
                    val parsedUri = Uri.parse(uriString)
                    player.setDataSource(applicationContext, parsedUri)
                    player.prepare()
                    uriLoaded = true
                    Log.d(TAG, "Successfully prepared MediaPlayer with custom Uri: $uriString")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load custom Uri $uriString, falling back to default", e)
                }
            }

            if (!uriLoaded) {
                val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                if (defaultUri != null) {
                    player.setDataSource(applicationContext, defaultUri)
                    player.prepare()
                    Log.d(TAG, "Successfully prepared MediaPlayer with fallback Uri: $defaultUri")
                } else {
                    Log.e(TAG, "No default alarm or ringtone Uri found!")
                    return
                }
            }

            mediaPlayer = player
            player.start()
            Log.d(TAG, "MediaPlayer started playing.")

        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound", e)
        }
    }

    private fun stopSound() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }
    }

    private fun buildForegroundNotification(channelId: String, title: String, text: String): android.app.Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val appPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val drankIntent = Intent(this, WaterActionReceiver::class.java).apply {
            action = "com.example.ACTION_RECORD_DRINK"
        }
        val drankPendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            drankIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, WaterActionReceiver::class.java).apply {
            action = "com.example.ACTION_SNOOZE_5"
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(appPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.checkbox_on_background, "✅ DRANK WATER NOW", drankPendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "⏲️ SNOOZE 5 MINS", snoozePendingIntent)
            .build()
    }

    private fun createNotificationChannel(): String {
        val channelId = "water_reminders_alarm_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Water Alarm Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Continuous looping friendly water reminders"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 300, 800)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        return channelId
    }

    override fun onDestroy() {
        Log.d(TAG, "WaterAlarmService destroyed, releasing resources.")
        stopSound()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "WaterAlarmService"
        const val NOTIFICATION_ID = 2002

        fun start(context: Context) {
            val intent = Intent(context, WaterAlarmService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WaterAlarmService::class.java)
            context.stopService(intent)
        }
    }
}
