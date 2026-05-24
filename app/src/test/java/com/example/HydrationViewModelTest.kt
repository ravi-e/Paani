package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.DrinkLog
import com.example.data.ReminderSettings
import com.example.data.WaterDatabase
import com.example.data.WaterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HydrationViewModelTest {

    private lateinit var context: Context
    private lateinit var database: WaterDatabase
    private lateinit var repository: WaterRepository
    private lateinit var viewModel: HydrationViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()

        // Initialize WorkManager for testing
        val config = androidx.work.Configuration.Builder()
            .setExecutor(androidx.work.testing.SynchronousExecutor())
            .build()
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        database = Room.inMemoryDatabaseBuilder(context, WaterDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(Runnable::run)
            .setTransactionExecutor(Runnable::run)
            .build()
        repository = WaterRepository(database.waterDao())
        viewModel = HydrationViewModel(repository, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    // Helper function to actively collect and await asynchronous updates to a StateFlow
    private suspend fun <T> StateFlow<T>.awaitValue(
        timeoutMillis: Long = 2000,
        condition: (T) -> Boolean
    ): T {
        var result: T = value
        kotlinx.coroutines.coroutineScope {
            // Subscribe to the StateFlow to trigger SharingStarted.WhileSubscribed
            val job = launch {
                collect { newValue ->
                    result = newValue
                }
            }
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < timeoutMillis) {
                if (condition(result)) {
                    break
                }
                kotlinx.coroutines.delay(20)
            }
            job.cancel() // Cleanly unsubscribe
        }
        return result
    }

    @Test
    fun testLogDrink() = runBlocking {
        // Initially, no logs
        var logs = viewModel.logsToday.awaitValue { it.isEmpty() }
        assertEquals(0, logs.size)

        // Log a drink
        viewModel.logDrink()
        
        // Wait for the background DB write and Flow emission
        logs = viewModel.logsToday.awaitValue { it.size == 1 }
        assertEquals(1, logs.size)

        // Check settings are updated (snooze reset, next alarm scheduled)
        val settings = viewModel.settings.awaitValue { it.nextReminderTimeMillis > 0L }
        assertEquals(0L, settings.snoozeTimeMillis)
        assertTrue(settings.nextReminderTimeMillis > 0L)
    }

    @Test
    fun testUndoLastDrink() = runBlocking {
        // Log two drinks
        viewModel.logDrink()
        viewModel.logsToday.awaitValue { it.size == 1 }
        
        viewModel.logDrink()
        var logs = viewModel.logsToday.awaitValue { it.size == 2 }
        assertEquals(2, logs.size)

        // Undo last drink
        viewModel.undoLastDrink()

        logs = viewModel.logsToday.awaitValue { it.size == 1 }
        assertEquals(1, logs.size)
    }

    @Test
    fun testClearTodayLogs() = runBlocking {
        // Log a drink
        viewModel.logDrink()
        var logs = viewModel.logsToday.awaitValue { it.size == 1 }
        assertEquals(1, logs.size)

        // Clear logs
        viewModel.clearTodayLogs()

        logs = viewModel.logsToday.awaitValue { it.isEmpty() }
        assertEquals(0, logs.size)
    }

    @Test
    fun testSnoozeReminder() = runBlocking {
        // Snooze for 5 minutes
        viewModel.snoozeReminder(5.0)

        val settings = viewModel.settings.awaitValue { it.snoozeTimeMillis > 0L }
        assertTrue(settings.snoozeTimeMillis > System.currentTimeMillis())
        assertEquals(settings.snoozeTimeMillis, settings.nextReminderTimeMillis)
    }

    @Test
    fun testUpdateSchedule() = runBlocking {
        // Update to custom settings
        viewModel.updateSchedule(
            startHour = 9,
            endHour = 21,
            intervalMinutes = 45,
            targetGlasses = 10
        )

        val settings = viewModel.settings.awaitValue { it.targetGlasses == 10 }
        assertEquals(9, settings.startTimeHour)
        assertEquals(21, settings.endTimeHour)
        assertEquals(45, settings.intervalMinutes)
        assertEquals(10, settings.targetGlasses)
        assertEquals(0L, settings.snoozeTimeMillis)
    }

    @Test
    fun testUpdateScheduleClamping() = runBlocking {
        // Attempt update with startHour >= endHour (e.g. startHour=10, endHour=9)
        // Clamping logic: for startHour=10, endHour=9: nextStartHour = nextEndHour - 1 = 9 - 1 = 8
        viewModel.updateSchedule(
            startHour = 10,
            endHour = 9
        )

        val settings = viewModel.settings.awaitValue { it.startTimeHour == 8 && it.endTimeHour == 9 }
        assertEquals(8, settings.startTimeHour)
        assertEquals(9, settings.endTimeHour)
    }
}
