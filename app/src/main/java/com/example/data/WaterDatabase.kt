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
    val snoozeTimeMillis: Long = 0L // 0 if not currently snoozed
)

@Dao
interface WaterDao {
    @Query("SELECT * FROM drink_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<DrinkLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DrinkLog)

    @Query("DELETE FROM drink_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM drink_logs WHERE id = (SELECT id FROM drink_logs ORDER BY timestamp DESC LIMIT 1)")
    suspend fun deleteLastLog()

    @Query("SELECT * FROM reminder_settings WHERE id = 1")
    suspend fun getSettings(): ReminderSettings?

    @Query("SELECT * FROM reminder_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<ReminderSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: ReminderSettings)
}

@Database(entities = [DrinkLog::class, ReminderSettings::class], version = 1, exportSchema = false)
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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class WaterRepository(private val waterDao: WaterDao) {
    val allLogs: Flow<List<DrinkLog>> = waterDao.getAllLogs()

    val logsToday: Flow<List<DrinkLog>> = waterDao.getAllLogs().map { logs ->
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        logs.filter { it.timestamp >= todayStart }
    }

    val settings: Flow<ReminderSettings> = waterDao.getSettingsFlow().map {
        it ?: ReminderSettings()
    }

    suspend fun getSettingsDirect(): ReminderSettings {
        return waterDao.getSettings() ?: ReminderSettings()
    }

    suspend fun insertLog(log: DrinkLog) = waterDao.insertLog(log)

    suspend fun clearLogs() = waterDao.clearAllLogs()

    suspend fun deleteLastLog() = waterDao.deleteLastLog()

    suspend fun updateSettings(newSettings: ReminderSettings) = waterDao.insertSettings(newSettings)
}
