package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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
