package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

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
