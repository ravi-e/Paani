package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

@Entity(tableName = "drink_logs")
data class DrinkLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminder_settings")
data class ReminderSettings(
    @PrimaryKey val id: Int = 1,
    val startTimeHour: Int = 8,
    val startTimeMinute: Int = 0,
    val endTimeHour: Int = 20,
    val endTimeMinute: Int = 0,
    val intervalMinutes: Int = 60, // default 1 hour
    val targetGlasses: Int = 8,
    val nextReminderTimeMillis: Long = 0L,
    val snoozeTimeMillis: Long = 0L, // 0 if not currently snoozed
    val userName: String = "", // set during first-launch onboarding
    val voiceReminderEnabled: Boolean = true, // speak aloud when reminder fires
    val alarmSoundUri: String = "" // custom alarm sound file Uri string
)

@Database(entities = [DrinkLog::class, ReminderSettings::class], version = 4, exportSchema = false)
abstract class WaterDatabase : RoomDatabase() {
    abstract fun waterDao(): WaterDao

    companion object {
        @Volatile
        private var INSTANCE: WaterDatabase? = null

        fun getDatabase(context: Context): WaterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WaterDatabase::class.java,
                    "water_reminder_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
