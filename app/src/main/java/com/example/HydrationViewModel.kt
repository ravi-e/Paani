package com.example

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DrinkLog
import com.example.data.ReminderSettings
import com.example.data.WaterRepository
import com.example.receiver.WaterReminderScheduler
import com.example.receiver.WaterAlarmService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Custom ViewModel Factory supporting Room repository parameters
class HydrationViewModelFactory(
    private val repository: WaterRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HydrationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HydrationViewModel(repository, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class HydrationViewModel(
    private val repository: WaterRepository,
    private val appContext: Context
) : ViewModel() {
    
    // Track logs and settings
    val logsToday: StateFlow<List<DrinkLog>> = repository.logsToday
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val settings: StateFlow<ReminderSettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReminderSettings()
        )

    private val _isNotificationPermissionGranted = MutableStateFlow(true)
    val isNotificationPermissionGranted = _isNotificationPermissionGranted.asStateFlow()

    fun updatePermissionStatus(isGranted: Boolean) {
        _isNotificationPermissionGranted.value = isGranted
    }

    // Initialize or verify alarms
    fun syncAlarms() {
        viewModelScope.launch {
            val currentSettings = repository.getSettingsDirect()
            WaterReminderScheduler.scheduleNextReminder(appContext, currentSettings)
        }
    }

    // Add today's drinking record
    fun logDrink() {
        viewModelScope.launch {
            try {
                repository.insertLog(DrinkLog())
                val currentSettings = repository.getSettingsDirect()
                // Reset snooze hours back to normal state as senior successfully drank
                val updated = currentSettings.copy(
                    snoozeTimeMillis = 0L,
                    nextReminderTimeMillis = WaterReminderScheduler.calculateNextAlarmTime(currentSettings)
                )
                repository.updateSettings(updated)
                WaterReminderScheduler.scheduleNextReminder(appContext, updated)
                
                // Stop continuous looping alarm
                try {
                    WaterAlarmService.stop(appContext)
                } catch (e: Exception) {
                    System.err.println("TEST_ERROR: Could not stop alarm service: " + e.message)
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                System.err.println("TEST_ERROR: Error logging drink: " + e.message)
                e.printStackTrace()
            }
        }
    }

    // Undo the last logged drink (e.g. if senior clicked the glass accidentally)
    fun undoLastDrink() {
        viewModelScope.launch {
            try {
                repository.deleteLastLog()
                val currentSettings = repository.getSettingsDirect()
                val updated = currentSettings.copy(
                    snoozeTimeMillis = 0L,
                    nextReminderTimeMillis = WaterReminderScheduler.calculateNextAlarmTime(currentSettings)
                )
                repository.updateSettings(updated)
                WaterReminderScheduler.scheduleNextReminder(appContext, updated)
                
                // Stop continuous looping alarm
                try {
                    WaterAlarmService.stop(appContext)
                } catch (e: Exception) {
                    Log.w("HydrationViewModel", "Could not stop alarm service in test/bg environment", e)
                }
            } catch (e: Exception) {
                Log.e("HydrationViewModel", "Error undoing drink", e)
            }
        }
    }

    // Handle standard daily progress wipe (for clean testing or manual correction)
    fun clearTodayLogs() {
        viewModelScope.launch {
            try {
                repository.clearLogs()
            } catch (e: Exception) {
                Log.e("HydrationViewModel", "Error clearing logs", e)
            }
        }
    }

    // Fast-snooze current active reminder alert
    fun snoozeReminder(minutes: Double) {
        viewModelScope.launch {
            try {
                val snoozeMillis = System.currentTimeMillis() + (minutes * 60 * 1000).toLong()
                val currentSettings = repository.getSettingsDirect()
                val updated = currentSettings.copy(
                    snoozeTimeMillis = snoozeMillis,
                    nextReminderTimeMillis = snoozeMillis
                )
                repository.updateSettings(updated)
                WaterReminderScheduler.scheduleNextReminder(appContext, updated)
                
                // Stop continuous looping alarm
                try {
                    WaterAlarmService.stop(appContext)
                } catch (e: Exception) {
                    Log.w("HydrationViewModel", "Could not stop alarm service in test/bg environment", e)
                }
            } catch (e: Exception) {
                Log.e("HydrationViewModel", "Error snoozing reminder", e)
            }
        }
    }

    // Persist the user's name from the onboarding screen
    fun updateUserName(name: String) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect()
            repository.updateSettings(current.copy(userName = name.trim()))
        }
    }

    // Toggle the optional voice/TTS reminder cue
    fun updateVoiceReminder(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect()
            repository.updateSettings(current.copy(voiceReminderEnabled = enabled))
        }
    }

    // Settings modifiers with safe bound checks
    fun updateSchedule(
        startHour: Int? = null,
        endHour: Int? = null,
        intervalMinutes: Int? = null,
        targetGlasses: Int? = null,
        voiceReminderEnabled: Boolean? = null,
        alarmSoundUri: String? = null
    ) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect()
            var nextStartHour = startHour ?: current.startTimeHour
            var nextEndHour = endHour ?: current.endTimeHour
            var nextInterval = intervalMinutes ?: current.intervalMinutes
            var nextTarget = targetGlasses ?: current.targetGlasses
            var nextVoice = voiceReminderEnabled ?: current.voiceReminderEnabled
            var nextAlarmUri = alarmSoundUri ?: current.alarmSoundUri

            // Clamp check bounds
            if (nextStartHour >= nextEndHour) {
                // Keep minimum of 1-hour span
                if (startHour != null) nextStartHour = nextEndHour - 1
                else if (endHour != null) nextEndHour = nextStartHour + 1
            }
            if (nextStartHour < 0) nextStartHour = 0
            if (nextEndHour > 24) nextEndHour = 24
            if (nextInterval < 1) nextInterval = 1
            if (nextTarget < 1) nextTarget = 1

            val updated = current.copy(
                startTimeHour = nextStartHour,
                endTimeHour = nextEndHour,
                intervalMinutes = nextInterval,
                targetGlasses = nextTarget,
                voiceReminderEnabled = nextVoice,
                alarmSoundUri = nextAlarmUri,
                snoozeTimeMillis = 0L // reset any active snoozes on interval edit
            )
            repository.updateSettings(updated)
            WaterReminderScheduler.scheduleNextReminder(appContext, updated)
        }
    }
}
