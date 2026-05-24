package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.DrinkLog
import com.example.data.ReminderSettings
import com.example.data.WaterDatabase
import com.example.data.WaterDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WaterDaoTest {

    private lateinit var database: WaterDatabase
    private lateinit var dao: WaterDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WaterDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.waterDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndGetLogs() = runBlocking {
        var logs = dao.getAllLogs().first()
        assertEquals(0, logs.size)

        val log1 = DrinkLog(id = 1, timestamp = 1000L)
        val log2 = DrinkLog(id = 2, timestamp = 2000L)
        
        dao.insertLog(log1)
        dao.insertLog(log2)

        logs = dao.getAllLogs().first()
        assertEquals(2, logs.size)
        assertEquals(2, logs[0].id) // Ordered DESC by timestamp
        assertEquals(1, logs[1].id)
    }

    @Test
    fun testDeleteLastLog() = runBlocking {
        val log1 = DrinkLog(id = 1, timestamp = 1000L)
        val log2 = DrinkLog(id = 2, timestamp = 2000L)
        
        dao.insertLog(log1)
        dao.insertLog(log2)

        dao.deleteLastLog()

        val logs = dao.getAllLogs().first()
        assertEquals(1, logs.size)
        assertEquals(1, logs[0].id)
    }

    @Test
    fun testClearAllLogs() = runBlocking {
        dao.insertLog(DrinkLog(id = 1, timestamp = 1000L))
        dao.insertLog(DrinkLog(id = 2, timestamp = 2000L))

        dao.clearAllLogs()

        val logs = dao.getAllLogs().first()
        assertEquals(0, logs.size)
    }

    @Test
    fun testInsertAndGetSettings() = runBlocking {
        var settings = dao.getSettings()
        assertNull(settings)

        val newSettings = ReminderSettings(
            id = 1,
            startTimeHour = 7,
            endTimeHour = 22,
            intervalMinutes = 30,
            targetGlasses = 12
        )
        dao.insertSettings(newSettings)

        settings = dao.getSettings()
        assertNotNull(settings)
        assertEquals(7, settings?.startTimeHour)
        assertEquals(22, settings?.endTimeHour)
        assertEquals(30, settings?.intervalMinutes)
        assertEquals(12, settings?.targetGlasses)
    }
}
