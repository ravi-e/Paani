package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.data.DrinkLog
import com.example.data.ReminderSettings
import com.example.data.WaterDatabase
import com.example.data.WaterRepository
import com.example.receiver.WaterReminderScheduler
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var database: WaterDatabase
    private lateinit var repository: WaterRepository
    private lateinit var viewModelFactory: WaterViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = WaterDatabase.getDatabase(this)
        repository = WaterRepository(database.waterDao())
        viewModelFactory = WaterViewModelFactory(repository, applicationContext)

        setContent {
            MyApplicationTheme {
                val viewModel: WaterViewModel = ViewModelProvider(this, viewModelFactory)[WaterViewModel::class.java]
                
                // Keep alarms synchronized with active settings on launch
                LaunchedEffect(Unit) {
                    viewModel.syncAlarms(applicationContext)
                }

                WaterReminderAppScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// Custom ViewModel Factory supporting Room repository parameters
class WaterViewModelFactory(
    private val repository: WaterRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WaterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WaterViewModel(repository, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class WaterViewModel(
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
    fun syncAlarms(context: Context) {
        viewModelScope.launch {
            val currentSettings = repository.getSettingsDirect()
            WaterReminderScheduler.scheduleNextReminder(context, currentSettings)
        }
    }

    // Add today's drinking record
    fun logDrink(context: Context) {
        viewModelScope.launch {
            repository.insertLog(DrinkLog())
            val currentSettings = repository.getSettingsDirect()
            // Reset snooze hours back to normal state as senior successfully drank
            val updated = currentSettings.copy(
                snoozeTimeMillis = 0L,
                nextReminderTimeMillis = WaterReminderScheduler.calculateNextAlarmTime(currentSettings)
            )
            repository.updateSettings(updated)
            WaterReminderScheduler.scheduleNextReminder(context, updated)
        }
    }

    // Undo the last logged drink (e.g. if senior clicked the glass accidentally)
    fun undoLastDrink(context: Context) {
        viewModelScope.launch {
            repository.deleteLastLog()
            val currentSettings = repository.getSettingsDirect()
            val updated = currentSettings.copy(
                snoozeTimeMillis = 0L,
                nextReminderTimeMillis = WaterReminderScheduler.calculateNextAlarmTime(currentSettings)
            )
            repository.updateSettings(updated)
            WaterReminderScheduler.scheduleNextReminder(context, updated)
        }
    }

    // Handle standard daily progress wipe (for clean testing or manual correction)
    fun clearTodayLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    // Fast-snooze current active reminder alert
    fun snoozeReminder(context: Context, minutes: Double) {
        viewModelScope.launch {
            val snoozeMillis = System.currentTimeMillis() + (minutes * 60 * 1000).toLong()
            val currentSettings = repository.getSettingsDirect()
            val updated = currentSettings.copy(
                snoozeTimeMillis = snoozeMillis,
                nextReminderTimeMillis = snoozeMillis
            )
            repository.updateSettings(updated)
            WaterReminderScheduler.scheduleNextReminder(context, updated)
        }
    }

    // Settings modifiers with safe bound checks
    fun updateSchedule(
        context: Context,
        startHour: Int? = null,
        endHour: Int? = null,
        intervalMinutes: Int? = null,
        targetGlasses: Int? = null
    ) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect()
            var nextStartHour = startHour ?: current.startTimeHour
            var nextEndHour = endHour ?: current.endTimeHour
            var nextInterval = intervalMinutes ?: current.intervalMinutes
            var nextTarget = targetGlasses ?: current.targetGlasses

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
                snoozeTimeMillis = 0L // reset any active snoozes on interval edit
            )
            repository.updateSettings(updated)
            WaterReminderScheduler.scheduleNextReminder(context, updated)
        }
    }
}

@Composable
fun WaterReminderAppScreen(
    viewModel: WaterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logsToday by viewModel.logsToday.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isPermGranted by viewModel.isNotificationPermissionGranted.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showSettingsDialog by remember { mutableStateOf(false) }

    val glassesDrunk = logsToday.size
    val dailyProgress = if (settings.targetGlasses > 0) {
        (glassesDrunk.toFloat() / settings.targetGlasses.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    // Permission launcher for high legibility warning card
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.updatePermissionStatus(isGranted)
            if (!isGranted) {
                Toast.makeText(context, "Please enable notification permissions to hear reminders on-time!", Toast.LENGTH_LONG).show()
            }
        }
    )

    // Verify notification alert rights
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            viewModel.updatePermissionStatus(status)
        } else {
            viewModel.updatePermissionStatus(true)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Main non-scrolled container
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                
                // 1. HEADER ROW (Title & Settings Gear Icon)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "💧 HYDROCOMPANION",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Senior Hydration Companion",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSystemInDarkTheme()) Color(0xFFF8FAFC) else Color(0xFF43493E)
                        )
                    }

                    // Settings Cog Icon Button
                    IconButton(
                        onClick = {
                            triggerHapticFeedback(context)
                            showSettingsDialog = true
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFECEFE9),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuration Menu",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 2. SILENT ALERT BANNER (Only displayed if notifications disabled)
                if (!isPermGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3CD),
                            contentColor = Color(0xFF856404)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                            .padding(bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⚠️", fontSize = 24.sp)
                            Column {
                                Text("Reminders are Silent", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Tap here to allow system alert sounds.", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // 3. MAIN CENTRAL BOTTLE DISPLAY CARD
                WaterProgressCard(
                    progress = dailyProgress,
                    glassesCount = glassesDrunk,
                    target = settings.targetGlasses,
                    nextReminderMillis = settings.nextReminderTimeMillis,
                    snoozeTimeMillis = settings.snoozeTimeMillis,
                    onGlassClick = {
                        triggerStrongHapticFeedback(context)
                        viewModel.logDrink(context)
                        
                        // Handle 5-second Undo with Snackbar action
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            val snackbarResult = snackbarHostState.showSnackbar(
                                message = "Registered 1 glass of water! 💧",
                                actionLabel = "UNDO",
                                duration = SnackbarDuration.Short
                            )
                            if (snackbarResult == SnackbarResult.ActionPerformed) {
                                triggerHapticFeedback(context)
                                viewModel.undoLastDrink(context)
                                Toast.makeText(context, "Last drink undone", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // 4. SNOOZE ROW (1 min, 2 min, & 5 min snoozes)
                SnoozeController(
                    onSnoozeSelected = { waitMinutes ->
                        triggerHapticFeedback(context)
                        viewModel.snoozeReminder(context, waitMinutes)
                        Toast.makeText(context, "Delayed alert by ${waitMinutes.toInt()} min", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Settings Modal Overlaid Dialog
            if (showSettingsDialog) {
                SettingsDialog(
                    settings = settings,
                    onDismiss = { showSettingsDialog = false },
                    onSettingsChanged = { start, end, interval, target ->
                        viewModel.updateSchedule(context, start, end, interval, target)
                    },
                    onClearLogs = {
                        triggerStrongHapticFeedback(context)
                        viewModel.clearTodayLogs()
                        Toast.makeText(context, "Hydration records cleared", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun WaterProgressCard(
    progress: Float,
    glassesCount: Int,
    target: Int,
    nextReminderMillis: Long,
    snoozeTimeMillis: Long,
    onGlassClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var splashState by remember { mutableStateOf(false) }

    val scaleSplash by animateFloatAsState(
        targetValue = if (splashState) 1.12f else 1.00f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        finishedListener = { splashState = false },
        label = "bounceScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Counter Display (Big Number, text-xl font-bold)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Text(
                text = "$glassesCount",
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0061A4),
                lineHeight = 58.sp
            )
            Text(
                text = "Glasses Drunk Today (Goal: $target)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSystemInDarkTheme()) Color(0xFFF8FAFC) else Color(0xFF43493E)
            )
        }

        // MASSIVE WATER BOTTLE BUTTON (Interactive Progress Indicator - scaled to fit without scrolling)
        Box(
            modifier = Modifier
                .scale(scaleSplash)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    splashState = true
                    onGlassClick()
                }
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Soft background glow
            Box(
                modifier = Modifier
                    .size(width = 175.dp, height = 245.dp)
                    .background(Color(0xFFD1E4FF).copy(alpha = if (isSystemInDarkTheme()) 0.12f else 0.35f), RoundedCornerShape(32.dp))
            )

            // Custom filling water bottle drawing with tick marks & high-end gloss sheen
            WaterBottleIndicator(
                progress = progress,
                glassesCount = glassesCount,
                target = target,
                modifier = Modifier.padding(10.dp)
            )
        }

        // Action helper tap text
        Text(
            text = "👉 TAP THE BOTTLE TO DRINK 👈",
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = if (isSystemInDarkTheme()) Color(0xFF80DEEA) else Color(0xFF0061A4),
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(vertical = 2.dp)
        )

        // Customizable Goal Linear Progress Bar Component
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Goal Progress",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemInDarkTheme()) Color(0xFFF8FAFC) else Color(0xFF43493E)
                )
                Text(
                    text = "${(progress * 100).toInt()}% Done",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSystemInDarkTheme()) Color(0xFF22D3EE) else Color(0xFF386A20)
                )
            }

            // High-contrast, beautiful rounded Linear Progress indicator
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = Color(0xFF0061A4),
                trackColor = if (isSystemInDarkTheme()) Color(0xFF334155).copy(alpha = 0.3f) else Color(0xFFC6C8B9).copy(alpha = 0.3f)
            )

            // Dynamic encouraging state text inside non-overflow box
            if (glassesCount >= target) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFE7E9D9)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "🎉 Spectacular job! Daily hydration completed! 🎉",
                        color = if (isSystemInDarkTheme()) Color(0xFF22D3EE) else Color(0xFF386A20),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp)
                    )
                }
            }
        }

        // Next Reminder Indicator (White pulsed dot, blue card, white text)
        val reminderText = if (snoozeTimeMillis > System.currentTimeMillis()) {
            val remTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(snoozeTimeMillis))
            "Snoozed until: $remTime"
        } else if (nextReminderMillis > System.currentTimeMillis()) {
            val remTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(nextReminderMillis))
            "Next Reminder: $remTime"
        } else {
            "Reminders Active"
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0061A4)),
            shape = RoundedCornerShape(50),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseOpacity by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dotPulse"
                )

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.White.copy(alpha = pulseOpacity), CircleShape)
                )
                Text(
                    text = reminderText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun WaterBottleIndicator(
    progress: Float,
    glassesCount: Int,
    target: Int,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "waterFill"
    )

    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .width(155.dp)
            .height(225.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Dimensions of the bottle structure
            val capWidth = width * 0.38f
            val capHeight = height * 0.08f
            
            val neckWidth = width * 0.28f
            val neckHeight = height * 0.12f
            
            val bodyWidth = width * 0.85f
            val bodyHeight = height * 0.76f
            
            val capLeft = (width - capWidth) / 2f
            val neckLeft = (width - neckWidth) / 2f
            val bodyLeft = (width - bodyWidth) / 2f
            val bodyTop = capHeight + neckHeight

            val capColor = if (isDark) Color(0xFF81C784) else Color(0xFF386A20) 
            val borderGlassColor = if (isDark) Color(0xFF5C6354) else Color(0xFFC6C8B9) 
            val emptyWaterColor = if (isDark) Color(0xFF232620) else Color.White.copy(alpha = 0.85f)

            // 1. Draw CAP
            drawRoundRect(
                color = capColor,
                topLeft = Offset(capLeft, 0f),
                size = Size(capWidth, capHeight),
                cornerRadius = CornerRadius(12f, 12f)
            )
            
            // Cap accent line
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(capLeft + 8f, capHeight / 2f),
                end = Offset(capLeft + capWidth - 8f, capHeight / 2f),
                strokeWidth = 4f
            )

            // 2. Draw NECK
            drawRect(
                color = emptyWaterColor,
                topLeft = Offset(neckLeft, capHeight),
                size = Size(neckWidth, neckHeight)
            )
            drawLine(
                color = borderGlassColor,
                start = Offset(neckLeft, capHeight),
                end = Offset(neckLeft, capHeight + neckHeight),
                strokeWidth = 6f
            )
            drawLine(
                color = borderGlassColor,
                start = Offset(neckLeft + neckWidth, capHeight),
                end = Offset(neckLeft + neckWidth, capHeight + neckHeight),
                strokeWidth = 6f
            )

            // 3. Draw BOTTLE BODY outline path
            val bottlePath = Path().apply {
                moveTo(neckLeft, capHeight + neckHeight)
                quadraticTo(
                    bodyLeft, capHeight + neckHeight + 15f,
                    bodyLeft, bodyTop + 25f
                )
                lineTo(bodyLeft, bodyTop + bodyHeight - 35f)
                quadraticTo(
                    bodyLeft, bodyTop + bodyHeight,
                    bodyLeft + 35f, bodyTop + bodyHeight
                )
                lineTo(bodyLeft + bodyWidth - 35f, bodyTop + bodyHeight)
                quadraticTo(
                    bodyLeft + bodyWidth, bodyTop + bodyHeight,
                    bodyLeft + bodyWidth, bodyTop + bodyHeight - 35f
                )
                lineTo(bodyLeft + bodyWidth, bodyTop + 25f)
                quadraticTo(
                    bodyLeft + bodyWidth, capHeight + neckHeight + 15f,
                    neckLeft + neckWidth, capHeight + neckHeight
                )
                close()
            }

            drawPath(
                path = bottlePath,
                color = emptyWaterColor
            )

            // 4. Draw dynamic water brush gradient
            clipPath(path = bottlePath) {
                val waveHeight = bodyHeight * animatedProgress
                val waveTopY = (bodyTop + bodyHeight) - waveHeight

                if (waveHeight > 0f) {
                    val activeWaterBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF29B6F6), // Top beautiful sheen sky blue
                            Color(0xFF0061A4), // Secondary water blue
                            Color(0xFF0D47A1)  // Bottom deep ocean water
                        ),
                        startY = waveTopY,
                        endY = bodyTop + bodyHeight
                    )

                    drawRect(
                        brush = activeWaterBrush,
                        topLeft = Offset(0f, waveTopY),
                        size = Size(width, waveHeight + 50f)
                    )
                    
                    // Wave ripple top overlay highlight
                    drawOval(
                        color = if (isDark) Color(0xFFE0F7FA).copy(alpha = 0.4f) else Color(0xFFD1E4FF).copy(alpha = 0.6f),
                        topLeft = Offset(bodyLeft - 10f, waveTopY - 8f),
                        size = Size(bodyWidth + 20f, 16f)
                    )

                    // Float bubbles inside filled water column
                    if (waveHeight > 30f) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.4f),
                            radius = 5f,
                            center = Offset(width * 0.35f, waveTopY + waveHeight * 0.6f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.5f),
                            radius = 3.5f,
                            center = Offset(width * 0.5f, waveTopY + waveHeight * 0.25f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.35f),
                            radius = 6f,
                            center = Offset(width * 0.68f, waveTopY + waveHeight * 0.75f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.45f),
                            radius = 4f,
                            center = Offset(width * 0.48f, waveTopY + waveHeight * 0.82f)
                        )
                    }
                }
            }

            // 5. Draw customizable goal tick lines inside the bottle for progress marking
            if (target > 0) {
                val usableHeight = bodyHeight - 60f
                val paddingBottom = 30f
                for (i in 1..target) {
                    val ratio = i.toFloat() / target.toFloat()
                    val tickY = (bodyTop + bodyHeight) - (usableHeight * ratio) - paddingBottom
                    val isCompleted = glassesCount >= i
                    
                    val lineColor = if (isCompleted) {
                        if (isDark) Color(0xFFE0F7FA) else Color(0xFFD1E4FF)
                    } else {
                        if (isDark) Color(0xFF43493E).copy(alpha = 0.3f) else Color(0xFFC6C8B9).copy(alpha = 0.4f)
                    }
                    val lineStrokeWidth = if (isCompleted) 5f else 3f
                    val lineLength = bodyWidth * 0.45f
                    val lineStart = (width - lineLength) / 2f
                    
                    drawLine(
                        color = lineColor,
                        start = Offset(lineStart, tickY),
                        end = Offset(lineStart + lineLength, tickY),
                        strokeWidth = lineStrokeWidth
                    )
                }
            }

            // 6. Draw outer glass border outline
            drawPath(
                path = bottlePath,
                color = borderGlassColor,
                style = Stroke(width = 8f)
            )

            // 7. Dynamic glossy sheen reflection overlay line
            val sheenColor = Color.White.copy(alpha = if (isDark) 0.15f else 0.33f)
            val leftSheenPath = Path().apply {
                moveTo(neckLeft + 8f, capHeight + neckHeight + 8f)
                quadraticTo(
                    bodyLeft + 12f, capHeight + neckHeight + 12f,
                    bodyLeft + 12f, bodyTop + 55f
                )
            }
            drawPath(
                path = leftSheenPath,
                color = sheenColor,
                style = Stroke(width = 6f)
            )
            // Right edge sheen
            drawLine(
                color = sheenColor,
                start = Offset(bodyLeft + bodyWidth - 12f, bodyTop + 50f),
                end = Offset(bodyLeft + bodyWidth - 12f, bodyTop + bodyHeight - 50f),
                strokeWidth = 4f
            )
        }
    }
}

@Composable
fun SnoozeController(
    onSnoozeSelected: (Double) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFC6C8B9),
                thickness = 1.5.dp
            )
            Text(
                text = "SNOOZE FOR LATER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (isSystemInDarkTheme()) Color(0xFFF8FAFC).copy(alpha = 0.8f) else Color(0xFF43493E).copy(alpha = 0.8f),
                letterSpacing = 1.2.sp
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFC6C8B9),
                thickness = 1.5.dp
            )
        }

        // Exactly exactly three options: 1m, 2m, and 5m (no 10m snooze or rest)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val snoozeOptions = listOf(
                Pair("1 Min", 1.0),
                Pair("2 Min", 2.0),
                Pair("5 Min", 5.0)
            )
            
            snoozeOptions.forEach { option ->
                TactileSnoozeButton(
                    label = option.first.substringAfter(" "),
                    value = option.first.substringBefore(" "),
                    modifier = Modifier.weight(1f),
                    onClick = { onSnoozeSelected(option.second) }
                )
            }
        }
    }
}

@Composable
fun TactileSnoozeButton(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .border(
                width = 2.dp,
                color = if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFD1E4FF),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color.White,
            contentColor = Color(0xFF0061A4)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = if (isSystemInDarkTheme()) Color(0xFF29B6F6) else Color(0xFF0061A4)
            )
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = if (isSystemInDarkTheme()) Color(0xFFF8FAFC).copy(alpha = 0.6f) else Color(0xFF43493E).copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun SettingsDialog(
    settings: ReminderSettings,
    onDismiss: () -> Unit,
    onSettingsChanged: (Int?, Int?, Int?, Int?) -> Unit,
    onClearLogs: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ Safe Options",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider(
                    color = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFC6C8B9),
                    thickness = 1.dp
                )

                // 1. START WAKEUP HOUR
                SettingAdjuster(
                    label = "Wakeup (Start Time)",
                    valueText = formatHour(settings.startTimeHour),
                    onDecrease = { onSettingsChanged(settings.startTimeHour - 1, null, null, null) },
                    onIncrease = { onSettingsChanged(settings.startTimeHour + 1, null, null, null) }
                )

                HorizontalDivider(
                    color = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFC6C8B9),
                    thickness = 1.dp
                )

                // 2. END BEDTIME HOUR
                SettingAdjuster(
                    label = "Bedtime (End Time)",
                    valueText = formatHour(settings.endTimeHour),
                    onDecrease = { onSettingsChanged(null, settings.endTimeHour - 1, null, null) },
                    onIncrease = { onSettingsChanged(null, settings.endTimeHour + 1, null, null) }
                )

                HorizontalDivider(
                    color = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFC6C8B9),
                    thickness = 1.dp
                )

                // 3. INTERVAL SELECTION
                val listIntervals = listOf(15, 30, 45, 60, 90, 120, 180, 240)
                val currentIdx = listIntervals.indexOf(settings.intervalMinutes).coerceAtLeast(0)
                SettingAdjuster(
                    label = "Alert Interval",
                    valueText = formatInterval(settings.intervalMinutes),
                    onDecrease = {
                        if (currentIdx > 0) {
                            onSettingsChanged(null, null, listIntervals[currentIdx - 1], null)
                        }
                    },
                    onIncrease = {
                        if (currentIdx < listIntervals.lastIndex) {
                            onSettingsChanged(null, null, listIntervals[currentIdx + 1], null)
                        }
                    }
                )

                HorizontalDivider(
                    color = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFC6C8B9),
                    thickness = 1.dp
                )

                // 4. DAILY TARGET GLASSES
                SettingAdjuster(
                    label = "Daily Glasses Goal",
                    valueText = "${settings.targetGlasses} glasses",
                    onDecrease = { onSettingsChanged(null, null, null, settings.targetGlasses - 1) },
                    onIncrease = { onSettingsChanged(null, null, null, settings.targetGlasses + 1) }
                )

                HorizontalDivider(
                    color = if (isSystemInDarkTheme()) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFC6C8B9).copy(alpha = 0.5f),
                    thickness = 1.dp
                )

                // 5. RESET DAILY COUNTER & REMINDER TIMERS
                Button(
                    onClick = onClearLogs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart Counters",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Restart Counters & Clear Logs",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SettingAdjuster(
    label: String,
    valueText: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onDecrease,
                modifier = Modifier
                    .size(width = 60.dp, height = 48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSystemInDarkTheme()) Color(0xFF0F172A) else Color(0xFFE7E9D9),
                    contentColor = if (isSystemInDarkTheme()) Color.White else Color(0xFF386A20)
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("−", fontSize = 24.sp, fontWeight = FontWeight.Black)
            }

            Text(
                text = valueText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onIncrease,
                modifier = Modifier
                    .size(width = 60.dp, height = 48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSystemInDarkTheme()) Color(0xFF0F172A) else Color(0xFFE7E9D9),
                    contentColor = if (isSystemInDarkTheme()) Color.White else Color(0xFF386A20)
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

private fun formatHour(hour: Int): String {
    return when {
        hour == 0 -> "12 AM"
        hour == 12 -> "12 PM"
        hour > 12 -> "${hour - 12} PM"
        else -> "$hour AM"
    }
}

private fun formatInterval(minutes: Int): String {
    return when {
        minutes < 60 -> "Every $minutes mins"
        minutes == 60 -> "Every 1 hour"
        minutes % 60 == 0 -> "Every ${minutes / 60} hours"
        else -> "Every ${minutes / 60}h ${minutes % 60}m"
    }
}

private fun triggerStrongHapticFeedback(context: Context) {
    try {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        val pattern = longArrayOf(0, 160, 80, 160) // Start immediately, vibrate 160ms, rest 80ms, vibrate 160ms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, -1)
        }
    } catch (e: Exception) {
        triggerHapticFeedback(context)
    }
}

private fun triggerHapticFeedback(context: Context) {
    try {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(80)
        }
    } catch (e: Exception) {
        Log.e("HapticFeedback", "Could not trigger haptics", e)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

