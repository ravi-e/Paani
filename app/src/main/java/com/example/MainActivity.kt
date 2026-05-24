package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import kotlin.math.min

class MainActivity : ComponentActivity() {
    private lateinit var database: WaterDatabase
    private lateinit var repository: WaterRepository
    private lateinit var viewModelFactory: HydrationViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = WaterDatabase.getDatabase(this)
        repository = WaterRepository(database.waterDao())
        viewModelFactory = HydrationViewModelFactory(repository, applicationContext)

        setContent {
            MyApplicationTheme {
                val viewModel: HydrationViewModel = ViewModelProvider(this, viewModelFactory)[HydrationViewModel::class.java]
                val settings by viewModel.settings.collectAsState()

                if (settings.userName.isBlank()) {
                    // First launch: collect the user's name before showing the app
                    OnboardingScreen(
                        onNameSubmit = { name ->
                            viewModel.updateUserName(name)
                        }
                    )
                } else {
                    // Keep alarms synchronised with active settings on launch
                    LaunchedEffect(Unit) {
                        viewModel.syncAlarms()
                    }
                    WaterReminderAppScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun WaterReminderAppScreen(
    viewModel: HydrationViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logsToday by viewModel.logsToday.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isPermGranted by viewModel.isNotificationPermissionGranted.collectAsState()

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
                            text = "💧 Paani piyo!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Hydration Companion",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSystemInDarkTheme()) Color(0xFFF8FAFC) else Color(0xFF1A3C34)
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
                                color = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFDEF0EB),
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

                // 1b. GREETING BANNER
                GreetingBanner(userName = settings.userName)

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
                        viewModel.logDrink()
                        
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
                                viewModel.undoLastDrink()
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
                        viewModel.snoozeReminder(waitMinutes)
                        Toast.makeText(context, "Delayed alert by ${waitMinutes.toInt()} min", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Settings Modal Overlaid Dialog
            if (showSettingsDialog) {
                SettingsDialog(
                    settings = settings,
                    onDismiss = { showSettingsDialog = false },
                    onSettingsSaved = { start, end, interval, target, voice, alarmUri ->
                        viewModel.updateSchedule(start, end, interval, target, voice, alarmUri)
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
    var dropSplashActive by remember { mutableStateOf(false) }

    val scaleSplash by animateFloatAsState(
        targetValue = if (splashState) 1.12f else 1.00f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        finishedListener = { splashState = false },
        label = "bounceScale"
    )

    // Drop splash expand/fade out
    var dropScaleTarget by remember { mutableStateOf(0f) }
    val dropScale by animateFloatAsState(
        targetValue = dropScaleTarget,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        finishedListener = { dropScaleTarget = 0f },
        label = "dropScale"
    )
    val dropAlpha by animateFloatAsState(
        targetValue = if (dropScaleTarget > 0f) 0f else if (dropScale > 0.05f) 0.55f else 0f,
        animationSpec = tween(600, easing = LinearEasing),
        label = "dropAlpha"
    )

    val handleClick = {
        splashState = true
        dropScaleTarget = 2.2f
        onGlassClick()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Counter Display (Big Number)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Text(
                text = "$glassesCount",
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0061A4),
                lineHeight = 58.sp,
                modifier = Modifier.testTag("glasses_counter")
            )
            Text(
                text = "Glasses Drunk Today (Goal: $target)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSystemInDarkTheme()) Color(0xFFF8FAFC) else Color(0xFF1A3C34)
            )
        }

        // BOTTLE + SEGMENTED RING HALO — stacked in a Box so the ring wraps the bottle
        Box(
            modifier = Modifier
                .scale(scaleSplash)
                .testTag("water_bottle_button")
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { handleClick() }
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Segmented arc ring drawn behind the bottle
            SegmentedProgressRing(
                progress = progress,
                segments = target,
                modifier = Modifier.size(268.dp)
            )

            // Soft background glow — perfect circular matching halo
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .background(
                        Color(0xFFD1E4FF).copy(alpha = if (isSystemInDarkTheme()) 0.08f else 0.22f),
                        CircleShape
                    )
            )

            // Custom water bottle drawing
            WaterBottleIndicator(
                progress = progress,
                glassesCount = glassesCount,
                target = target,
                modifier = Modifier.padding(10.dp)
            )

            // Water-drop splash overlay (expands and fades out on tap)
            if (dropScale > 0.01f) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(dropScale)
                        .background(
                            Color(0xFF29B6F6).copy(alpha = dropAlpha.coerceIn(0f, 1f)),
                            CircleShape
                        )
                )
            }
        }

        // Large primary CTA button
        Button(
            onClick = { handleClick() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(64.dp)
                .testTag("drink_button"),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSystemInDarkTheme()) Color(0xFF00695C) else Color(0xFF006B5B),
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Text(
                text = "💧  I Drank a Glass",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.3.sp
            )
        }

        // Completion celebration card
        if (glassesCount >= target) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFDEF0EB)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 2.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "🎉 Spectacular job! Daily hydration completed! 🎉",
                    color = if (isSystemInDarkTheme()) Color(0xFF22D3EE) else Color(0xFF006B5B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp)
                )
            }
        }

        // Next Reminder Indicator (pulsing dot)
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
// ─── ONBOARDING SCREEN ──────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(onNameSubmit: (String) -> Unit) {
    val isDark = isSystemInDarkTheme()
    val focusManager = LocalFocusManager.current
    var nameInput by remember { mutableStateOf("") }
    val isValid = nameInput.trim().length >= 2

    val bgGradient = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF0A1628), Color(0xFF0D2137)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFF0FAF7), Color(0xFFDEF0EB)))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Drop icon hero
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        if (isDark) Color(0xFF006B5B).copy(alpha = 0.3f) else Color(0xFF006B5B).copy(alpha = 0.12f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("💧", fontSize = 52.sp)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Welcome to Paani",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color(0xFFF0FAF7) else Color(0xFF006B5B),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Your personal hydration reminder.\nLet's get you set up!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFFB2DFDB) else Color(0xFF1A3C34),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            // Name input card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF112233) else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "What's your name?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFB2DFDB) else Color(0xFF1A3C34)
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = {
                            Text(
                                "e.g. Ravi",
                                color = if (isDark) Color(0xFF5E7C77) else Color(0xFF9ABCB4)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (isValid) onNameSubmit(nameInput)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF006B5B),
                            unfocusedBorderColor = if (isDark) Color(0xFF2A4040) else Color(0xFFB2DFDB),
                            focusedTextColor = if (isDark) Color(0xFFF0FAF7) else Color(0xFF1A3C34),
                            unfocusedTextColor = if (isDark) Color(0xFFF0FAF7) else Color(0xFF1A3C34),
                            cursorColor = Color(0xFF006B5B)
                        )
                    )
                }
            }

            // CTA button
            Button(
                onClick = { if (isValid) onNameSubmit(nameInput) },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF006B5B),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF006B5B).copy(alpha = 0.35f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text(
                    text = "Get Started 💧",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

// ─── GREETING BANNER ────────────────────────────────────────────────────────

@Composable
fun GreetingBanner(userName: String) {
    val isDark = isSystemInDarkTheme()
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val (emoji, greeting) = when {
        hour in 5..11 -> "🌅" to "Good morning"
        hour in 12..16 -> "☀️" to "Good afternoon"
        hour in 17..20 -> "🌇" to "Good evening"
        else -> "🌙" to "Good night"
    }
    val displayName = userName.trim().split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: userName

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF112233) else Color(0xFFDEF0EB)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(emoji, fontSize = 26.sp)
            Column {
                Text(
                    text = "$greeting, $displayName!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color(0xFFF0FAF7) else Color(0xFF006B5B)
                )
                Text(
                    text = "Stay hydrated and feel great today.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFFB2DFDB) else Color(0xFF1A3C34)
                )
            }
        }
    }
}

// ─── SEGMENTED PROGRESS RING ────────────────────────────────────────────────

@Composable
fun SegmentedProgressRing(
    progress: Float,
    segments: Int,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "ringProgress"
    )

    Canvas(modifier = modifier) {
        val strokeWidthPx = 22.dp.toPx()
        val radius = (min(size.width, size.height) / 2f) - strokeWidthPx / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val totalSweep = 270f   // 270° arc starting from bottom-left
        val startAngle = 135f   // start at bottom-left, sweep clockwise to bottom-right
        val safeSegments = segments.coerceAtLeast(1)
        val gapDeg = if (safeSegments <= 1) 0f else 3f
        val usableSweep = totalSweep - gapDeg * safeSegments
        val segmentSweep = usableSweep / safeSegments

        val filledCount = (animatedProgress * safeSegments).toInt()
        val partialFraction = (animatedProgress * safeSegments) - filledCount

        for (i in 0 until safeSegments) {
            val segStartAngle = startAngle + i * (segmentSweep + gapDeg)
            val isFilled = i < filledCount
            val isPartial = i == filledCount && partialFraction > 0f
            val sweepToUse = when {
                isFilled -> segmentSweep
                isPartial -> segmentSweep * partialFraction
                else -> 0f
            }

            // Track (unfilled background arc for this segment)
            drawArc(
                color = if (isDark) Color(0xFF1E3040) else Color(0xFFB2DFDB).copy(alpha = 0.45f),
                startAngle = segStartAngle,
                sweepAngle = segmentSweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // Filled arc for this segment
            if (sweepToUse > 0f) {
                val fillColor = when {
                    isFilled -> if (isDark) Color(0xFF00897B) else Color(0xFF006B5B)
                    else -> if (isDark) Color(0xFF4DB6AC) else Color(0xFF26A69A)
                }
                drawArc(
                    color = fillColor,
                    startAngle = segStartAngle,
                    sweepAngle = sweepToUse,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────

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
            val capWidth = width * 0.44f
            val capHeight = height * 0.08f
            
            val neckWidth = width * 0.30f
            val neckHeight = height * 0.08f
            
            val bodyWidth = width * 0.78f
            val bodyHeight = height * 0.78f
            
            val capLeft = (width - capWidth) / 2f
            val neckLeft = (width - neckWidth) / 2f
            val bodyLeft = (width - bodyWidth) / 2f
            val bodyTop = capHeight + neckHeight

            val capColor = if (isDark) Color(0xFF29B6F6) else Color(0xFF0061A4)
            val borderGlassColor = if (isDark) Color(0xFF5E8276) else Color(0xFF1A3C34) 
            val emptyWaterColor = if (isDark) Color(0xFF002220).copy(alpha = 0.25f) else Color(0xFFE0F7FA).copy(alpha = 0.45f)
            val centerX = width / 2f

            // 1. Draw CAP (Rounded vector cap with 3D gradient reflection)
            val capBrush = Brush.horizontalGradient(
                colors = listOf(
                    capColor,
                    capColor.copy(alpha = 0.85f),
                    Color.White.copy(alpha = 0.38f), 
                    capColor,
                    capColor.copy(alpha = 0.95f)
                ),
                startX = capLeft,
                endX = capLeft + capWidth
            )
            drawRoundRect(
                brush = capBrush,
                topLeft = Offset(capLeft, 2f),
                size = Size(capWidth, capHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )
            // Cap outline
            drawRoundRect(
                color = borderGlassColor,
                topLeft = Offset(capLeft, 2f),
                size = Size(capWidth, capHeight),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 6f)
            )
            // Vertical cap ridges
            val ridgeStep = capWidth / 6f
            for (i in 1..5) {
                val ridgeX = capLeft + i * ridgeStep
                drawLine(
                    color = borderGlassColor.copy(alpha = 0.4f),
                    start = Offset(ridgeX, 4f),
                    end = Offset(ridgeX, capHeight - 4f),
                    strokeWidth = 3f
                )
            }

            // 2. Draw COLLAR RING (PET-style plastic collar below cap)
            drawRoundRect(
                color = capColor.copy(alpha = 0.8f),
                topLeft = Offset(neckLeft - 4f, capHeight + 1f),
                size = Size(neckWidth + 8f, 7f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = borderGlassColor,
                topLeft = Offset(neckLeft - 4f, capHeight + 1f),
                size = Size(neckWidth + 8f, 7f),
                cornerRadius = CornerRadius(4f, 4f),
                style = Stroke(width = 5f)
            )

            // 3. BOTTLE BODY outline path: premium ergonomic curves
            val bottlePath = Path().apply {
                // Neck start left
                moveTo(neckLeft, capHeight)
                // Down to shoulder joint
                lineTo(neckLeft, bodyTop)
                
                // Left shoulder curve: smooth sweep out to body side
                quadraticTo(bodyLeft, bodyTop + 2f, bodyLeft, bodyTop + 24f)
                
                // Left side ergonomic grip curve: gentle inward curve for premium tapered waist
                val midY = bodyTop + 24f + (bodyHeight - 48f) / 2f
                val leftControlX = bodyLeft + 6f
                cubicTo(
                    bodyLeft, bodyTop + 60f,
                    leftControlX, midY,
                    bodyLeft, bodyTop + bodyHeight - 24f
                )
                
                // Bottom left corner
                quadraticTo(bodyLeft, bodyTop + bodyHeight, bodyLeft + 24f, bodyTop + bodyHeight)
                
                // Smooth base curve (slightly convex bottom for organic premium canteen style)
                quadraticTo(width / 2f, bodyTop + bodyHeight + 4f, bodyLeft + bodyWidth - 24f, bodyTop + bodyHeight)
                
                // Bottom right corner
                quadraticTo(bodyLeft + bodyWidth, bodyTop + bodyHeight, bodyLeft + bodyWidth, bodyTop + bodyHeight - 24f)
                
                // Right side ergonomic grip curve: gentle inward curve mirroring the left side
                val rightControlX = bodyLeft + bodyWidth - 6f
                cubicTo(
                    bodyLeft + bodyWidth, bodyTop + bodyHeight - 60f,
                    rightControlX, midY,
                    bodyLeft + bodyWidth, bodyTop + 24f
                )
                
                // Right shoulder curve: smooth sweep back to right neck joint
                quadraticTo(bodyLeft + bodyWidth, bodyTop + 2f, neckLeft + neckWidth, bodyTop)
                
                // Straight neck up
                lineTo(neckLeft + neckWidth, capHeight)
                
                close()
            }

            // Draw translucent empty bottle background fill
            drawPath(
                path = bottlePath,
                color = emptyWaterColor
            )

            // 4. Draw dynamic water brush gradient with double waving overlay
            clipPath(path = bottlePath) {
                val waveHeight = bodyHeight * animatedProgress
                val waveTopY = (bodyTop + bodyHeight) - waveHeight

                if (waveHeight > 0f) {
                    val activeWaterBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFB3E5FC), // Top surface sheeny light blue
                            Color(0xFF29B6F6), // Vibrant sky blue
                            Color(0xFF0288D1), // Premium solid blue
                            Color(0xFF01579B)  // Deep bottom blue
                        ),
                        startY = waveTopY,
                        endY = bodyTop + bodyHeight
                    )

                    // Draw main water body
                    drawRect(
                        brush = activeWaterBrush,
                        topLeft = Offset(0f, waveTopY),
                        size = Size(width, waveHeight + 60f)
                    )

                    // Dynamic wave layer overlay for visual depth
                    val wavePath1 = Path().apply {
                        val startY = waveTopY
                        moveTo(bodyLeft - 20f, startY)
                        val waveWidth = bodyWidth + 40f
                        cubicTo(
                            bodyLeft + waveWidth * 0.25f, startY - 8f,
                            bodyLeft + waveWidth * 0.5f, startY + 8f,
                            bodyLeft + waveWidth * 0.75f, startY - 6f,
                        )
                        cubicTo(
                            bodyLeft + waveWidth * 0.85f, startY - 10f,
                            bodyLeft + waveWidth, startY,
                            bodyLeft + waveWidth, startY
                        )
                        lineTo(bodyLeft + waveWidth, bodyTop + bodyHeight + 20f)
                        lineTo(bodyLeft - 20f, bodyTop + bodyHeight + 20f)
                        close()
                    }
                    drawPath(
                        path = wavePath1,
                        color = Color(0xFF0288D1).copy(alpha = 0.12f)
                    )
                    
                    // Wave ripple top highlight
                    drawOval(
                        color = Color.White.copy(alpha = 0.55f),
                        topLeft = Offset(bodyLeft - 10f, waveTopY - 6f),
                        size = Size(bodyWidth + 20f, 12f)
                    )

                    // Floating cute bubbles inside the water column
                    if (waveHeight > 30f) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.6f),
                            radius = 4.5f,
                            center = Offset(width * 0.35f, waveTopY + waveHeight * 0.55f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.7f),
                            radius = 3.5f,
                            center = Offset(width * 0.52f, waveTopY + waveHeight * 0.25f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.5f),
                            radius = 5.5f,
                            center = Offset(width * 0.68f, waveTopY + waveHeight * 0.7f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.65f),
                            radius = 4f,
                            center = Offset(width * 0.46f, waveTopY + waveHeight * 0.8f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.5f),
                            radius = 3f,
                            center = Offset(width * 0.28f, waveTopY + waveHeight * 0.35f)
                        )
                    }
                }
            }

            // 5. Draw horizontal ribbed glass groove accent lines inside the bottle body
            // This gives it a premium glass partition structure without jagged outer walls!
            val stepHeight = bodyHeight / 4f
            for (i in 1..3) {
                val grooveY = bodyTop + i * stepHeight
                drawLine(
                    color = borderGlassColor.copy(alpha = if (isDark) 0.18f else 0.12f),
                    start = Offset(bodyLeft + 16f, grooveY),
                    end = Offset(bodyLeft + bodyWidth - 16f, grooveY),
                    strokeWidth = 3f
                )
            }

            // 6. Draw prominent, beautiful water droplet brand logo in the center
            val dropletCenterY = bodyTop + bodyHeight * 0.38f
            val dropletWidth = 26f
            val dropletHeight = 36f
            val dropletPath = Path().apply {
                moveTo(centerX, dropletCenterY - dropletHeight / 2f)
                // Left curve
                cubicTo(
                    centerX - dropletWidth, dropletCenterY - dropletHeight * 0.05f,
                    centerX - dropletWidth, dropletCenterY + dropletHeight * 0.45f,
                    centerX, dropletCenterY + dropletHeight / 2f
                )
                // Right curve
                cubicTo(
                    centerX + dropletWidth, dropletCenterY + dropletHeight * 0.45f,
                    centerX + dropletWidth, dropletCenterY - dropletHeight * 0.05f,
                    centerX, dropletCenterY - dropletHeight / 2f
                )
                close()
            }
            // Fill brand droplet with premium high-sheen gradient
            drawPath(
                path = dropletPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF81D4FA), Color(0xFF0288D1)),
                    startY = dropletCenterY - dropletHeight / 2f,
                    endY = dropletCenterY + dropletHeight / 2f
                )
            )
            // Droplet outline
            drawPath(
                path = dropletPath,
                color = borderGlassColor,
                style = Stroke(width = 5.5f)
            )
            // Droplet white glossy reflection glare dot
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = 4f,
                center = Offset(centerX - 6f, dropletCenterY + 2f)
            )

            // 7. Draw customizable goal tick lines inside the bottle for progress marking
            if (target > 0) {
                val usableHeight = bodyHeight - 60f
                val paddingBottom = 30f
                for (i in 1..target) {
                    val ratio = i.toFloat() / target.toFloat()
                    val tickY = (bodyTop + bodyHeight) - (usableHeight * ratio) - paddingBottom
                    val isCompleted = glassesCount >= i
                    
                    val lineColor = if (isCompleted) {
                        Color(0xFFD1E4FF)
                    } else {
                        if (isDark) Color(0xFF3A4D45).copy(alpha = 0.4f) else Color(0xFFC6C8B9).copy(alpha = 0.6f)
                    }
                    val lineStrokeWidth = if (isCompleted) 5f else 3f
                    val lineLength = bodyWidth * 0.22f
                    
                    // Draw tick marks on the right side of the bottle
                    val tickStart = bodyLeft + bodyWidth - lineLength - 10f
                    drawLine(
                        color = lineColor,
                        start = Offset(tickStart, tickY),
                        end = Offset(tickStart + lineLength, tickY),
                        strokeWidth = lineStrokeWidth
                    )
                }
            }

            // 8. Draw outer bottle border outline (bold and beautiful!)
            drawPath(
                path = bottlePath,
                color = borderGlassColor,
                style = Stroke(width = 7.5f)
            )

            // 9. Glossy 3D sheen reflection highlights on the left side
            val sheenColor = Color.White.copy(alpha = 0.45f)
            val leftSheenPath = Path().apply {
                moveTo(neckLeft + 6f, capHeight + 10f)
                lineTo(neckLeft + 6f, bodyTop)
                quadraticTo(bodyLeft + 10f, bodyTop + 2f, bodyLeft + 10f, bodyTop + 24f)
                // Follow the smooth ergonomic grip curve on the left side inset by 10f
                val midY = bodyTop + 24f + (bodyHeight - 48f) / 2f
                val leftControlX = bodyLeft + 16f
                cubicTo(
                    bodyLeft + 10f, bodyTop + 60f,
                    leftControlX, midY,
                    bodyLeft + 10f, bodyTop + bodyHeight - 24f
                )
            }
            drawPath(
                path = leftSheenPath,
                color = sheenColor,
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )
            
            // Subtle right side highlight
            drawLine(
                color = Color.White.copy(alpha = 0.28f),
                start = Offset(bodyLeft + bodyWidth - 10f, bodyTop + 30f),
                end = Offset(bodyLeft + bodyWidth - 10f, bodyTop + bodyHeight - 30f),
                strokeWidth = 3.5f
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

        // 5m, 10m, 15m, and 30m options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val snoozeOptions = listOf(
                Pair("5 Min", 5.0),
                Pair("10 Min", 10.0),
                Pair("15 Min", 15.0),
                Pair("30 Min", 30.0)
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
private fun getFileNameFromUri(context: Context, uriString: String): String {
    if (uriString.isBlank()) return "System Default Chime"
    return try {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        uri.path?.substringAfterLast('/') ?: "Custom Sound"
    } catch (e: Exception) {
        "Custom Audio File"
    }
}

@Composable
fun SettingsDialog(
    settings: ReminderSettings,
    onDismiss: () -> Unit,
    onSettingsSaved: (Int, Int, Int, Int, Boolean, String) -> Unit,
    onClearLogs: () -> Unit
) {
    val context = LocalContext.current

    // Local mutable state for batched settings
    var currentStartHour by remember { mutableStateOf(settings.startTimeHour) }
    var currentEndHour by remember { mutableStateOf(settings.endTimeHour) }
    var currentInterval by remember { mutableStateOf(settings.intervalMinutes) }
    var currentTarget by remember { mutableStateOf(settings.targetGlasses) }
    var currentVoiceEnabled by remember { mutableStateOf(settings.voiceReminderEnabled) }
    var currentAlarmUri by remember { mutableStateOf(settings.alarmSoundUri) }

    val alarmSoundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                // Request persistable URI permission so service can play it in background
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                Log.e("SettingsDialog", "Failed to take persistable URI permission", e)
            }
            currentAlarmUri = uri.toString()
        }
    }

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
                    valueText = formatHour(currentStartHour),
                    onDecrease = { 
                        if (currentStartHour > 0) {
                            currentStartHour-- 
                            if (currentStartHour >= currentEndHour) {
                                currentEndHour = currentStartHour + 1
                            }
                        }
                    },
                    onIncrease = { 
                        if (currentStartHour < 23) {
                            currentStartHour++ 
                            if (currentStartHour >= currentEndHour) {
                                currentEndHour = currentStartHour + 1
                            }
                        }
                    }
                )

                HorizontalDivider(
                    color = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFC6C8B9),
                    thickness = 1.dp
                )

                // 2. END BEDTIME HOUR
                SettingAdjuster(
                    label = "Bedtime (End Time)",
                    valueText = formatHour(currentEndHour),
                    onDecrease = { 
                        if (currentEndHour > 1) {
                            currentEndHour-- 
                            if (currentStartHour >= currentEndHour) {
                                currentStartHour = currentEndHour - 1
                            }
                        }
                    },
                    onIncrease = { 
                        if (currentEndHour < 24) {
                            currentEndHour++ 
                            if (currentStartHour >= currentEndHour) {
                                currentStartHour = currentEndHour - 1
                            }
                        }
                    }
                )

                HorizontalDivider(
                    color = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFC6C8B9),
                    thickness = 1.dp
                )

                // 3. INTERVAL SELECTION
                val listIntervals = listOf(15, 30, 45, 60, 90, 120, 180, 240)
                val currentIdx = listIntervals.indexOf(currentInterval).coerceAtLeast(0)
                SettingAdjuster(
                    label = "Alert Interval",
                    valueText = formatInterval(currentInterval),
                    onDecrease = {
                        if (currentIdx > 0) {
                            currentInterval = listIntervals[currentIdx - 1]
                        }
                    },
                    onIncrease = {
                        if (currentIdx < listIntervals.lastIndex) {
                            currentInterval = listIntervals[currentIdx + 1]
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
                    valueText = "$currentTarget glasses",
                    onDecrease = { 
                        if (currentTarget > 1) currentTarget-- 
                    },
                    onIncrease = { 
                        if (currentTarget < 40) currentTarget++ 
                    }
                )

                HorizontalDivider(
                    color = if (isSystemInDarkTheme()) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFC6C8B9).copy(alpha = 0.5f),
                    thickness = 1.dp
                )

                // 5. VOICE REMINDER TOGGLE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔊 Voice reminder",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Speaks aloud when it's time to drink",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = currentVoiceEnabled,
                        onCheckedChange = { currentVoiceEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF006B5B),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFB2DFDB)
                        )
                    )
                }

                HorizontalDivider(
                    color = if (isSystemInDarkTheme()) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFC6C8B9).copy(alpha = 0.5f),
                    thickness = 1.dp
                )

                // 6. CUSTOM ALARM SOUND SELECTOR
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🎵 Alarm Sound",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = getFileNameFromUri(context, currentAlarmUri),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { alarmSoundPickerLauncher.launch("audio/*") },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSystemInDarkTheme()) Color(0xFF1A3038) else Color(0xFFDEF0EB),
                                contentColor = if (isSystemInDarkTheme()) Color.White else Color(0xFF006B5B)
                            )
                        ) {
                            Text("Choose File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(
                    color = if (isSystemInDarkTheme()) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFC6C8B9).copy(alpha = 0.5f),
                    thickness = 1.5.dp
                )

                // 7. SAVE BUTTON (Visual cue + Batched commit)
                Button(
                    onClick = {
                        triggerHapticFeedback(context)
                        onSettingsSaved(
                            currentStartHour,
                            currentEndHour,
                            currentInterval,
                            currentTarget,
                            currentVoiceEnabled,
                            currentAlarmUri
                        )
                        Toast.makeText(context, "Settings saved successfully! 💧", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSystemInDarkTheme()) Color(0xFF00695C) else Color(0xFF006B5B),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = "💾 Save Settings",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // 8. RESET DAILY COUNTER & REMINDER TIMERS
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

