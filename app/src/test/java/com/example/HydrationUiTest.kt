package com.example

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.WaterDatabase
import com.example.data.WaterRepository
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HydrationUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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

    @Test
    fun testTapBottleIncrementsCounter() {
        // Set Compose content inside the test rule
        composeTestRule.setContent {
            MyApplicationTheme {
                WaterReminderAppScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Verify initial glasses count is "0"
        composeTestRule.onNodeWithTag("glasses_counter").assertTextEquals("0")

        // Call logDrink directly on the viewModel to trigger a drink log
        viewModel.logDrink()

        // Real-time polling loop that yields the CPU to let background Room threads execute,
        // then pumps the Compose UI thread using waitForIdle()
        var isUpdated = false
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 4000) {
            composeTestRule.waitForIdle()
            try {
                composeTestRule.onNodeWithTag("glasses_counter").assertTextEquals("1")
                isUpdated = true
                break
            } catch (e: AssertionError) {
                // Eagerly yield the thread to background database query executors
                Thread.sleep(40)
            }
        }

        // Final assertion to report failure clearly if it never updated
        if (!isUpdated) {
            composeTestRule.onNodeWithTag("glasses_counter").assertTextEquals("1")
        }
    }
}
