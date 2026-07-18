package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.DecryptedCallLog
import com.example.data.DecryptedFaq
import com.example.data.DecryptedPriorityContact
import com.example.service.CallState
import com.example.ui.AppViewModel
import com.example.ui.AppScreen
import kotlinx.coroutines.delay
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.BeigePrimary as RawBeigePrimary
import com.example.ui.theme.BeigeSecondary as RawBeigeSecondary
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.*
import androidx.compose.animation.core.spring

import java.text.SimpleDateFormat
import java.util.*

val BeigePrimary: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF0DDE9F) else Color(0xFF2563EB) // Modern Mint Green vs Vibrant Sapphire Blue

val BeigeSecondary: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF38BDF8) else Color(0xFF0F172A) // Sky Blue vs Midnight Deep Slate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AppViewModel = viewModel()
            val highContrastMode by viewModel.isHighContrastMode.collectAsState()

            MyApplicationTheme(highContrastMode = highContrastMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val currentCallState by viewModel.currentCallState.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val outgoingCallActive by viewModel.outgoingCallActive.collectAsState()
    
    // Permission request logic
    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Microphone access is recommended for real AI Call answering simulation.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasRecordPermission) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            currentCallState != CallState.Idle -> {
                // Full Screen Call Screen Overlay
                CallScreenOverlay(
                    callState = currentCallState,
                    onEndCall = { viewModel.stopCallSimulation() }
                )
            }
            outgoingCallActive -> {
                // Immersive Outgoing Call Screen
                OutgoingCallOverlay(
                    viewModel = viewModel,
                    number = viewModel.outgoingCallNumber.collectAsState().value,
                    name = viewModel.outgoingCallName.collectAsState().value,
                    onEndCall = { viewModel.endOutgoingCall() }
                )
            }
            else -> {
                // Normal App Screen Switcher with Gorgeous Physics-based Screen Animations
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            (slideInHorizontally(animationSpec = spring(stiffness = 380f)) { it } + fadeIn()).togetherWith(
                                slideOutHorizontally(animationSpec = spring(stiffness = 380f)) { -it } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally(animationSpec = spring(stiffness = 380f)) { -it } + fadeIn()).togetherWith(
                                slideOutHorizontally(animationSpec = spring(stiffness = 380f)) { it } + fadeOut()
                            )
                        }
                    },
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        AppScreen.SPLASH -> SplashScreen(viewModel = viewModel)
                        AppScreen.ONBOARDING -> OnboardingScreen(viewModel = viewModel)
                        AppScreen.SIGN_UP -> SignUpScreen(viewModel = viewModel)
                        AppScreen.SIGN_IN -> SignInScreen(viewModel = viewModel)
                        AppScreen.SIGN_IN_OTP -> OtpScreen(viewModel = viewModel)
                        AppScreen.FORGOT_PASSWORD -> ForgotPasswordScreen(viewModel = viewModel)
                        AppScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel, hasRecordPermission = hasRecordPermission)
                    }
                }
            }
        }
    }
}

// --- FULL SCREEN CALL SCREEN OVERLAY ---

@Composable
fun CallScreenOverlay(
    callState: CallState,
    onEndCall: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Ensure the interface supports dark mode/night visibility
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712)) // Deep night shift obsidian background
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Decorative pulsing radial background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFF0D9488).copy(alpha = 0.08f),
                radius = size.minDimension / 1.5f * pulseScale
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                val callType = when (callState) {
                    is CallState.Incoming -> callState.callType
                    else -> "MB Connect AI"
                }
                
                Surface(
                    color = if (callType == "WhatsApp") Color(0xFF25D366).copy(alpha = 0.15f) else Color(0xFF0D9488).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (callType == "WhatsApp") Icons.Default.Chat else Icons.Default.Phone,
                            contentDescription = null,
                            tint = if (callType == "WhatsApp") Color(0xFF25D366) else Color(0xFF0D9488),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "INCOMING $callType CALL",
                            color = if (callType == "WhatsApp") Color(0xFF25D366) else Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                val callerName = when (callState) {
                    is CallState.Incoming -> callState.contactName
                    is CallState.Answering -> callState.contactName
                    is CallState.RecordingCaller -> "Active Call"
                    else -> "AI Receptionist"
                }

                val callerNumber = when (callState) {
                    is CallState.Incoming -> callState.phoneNumber
                    is CallState.Answering -> callState.phoneNumber
                    is CallState.RecordingCaller -> callState.phoneNumber
                    else -> ""
                }

                Text(
                    text = callerName.ifEmpty { "Unknown Caller" },
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (callerNumber.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = callerNumber,
                        color = Color.Gray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Central Avatar & Call Progress Visual
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Avatar circle with pulsing border
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale)
                        .border(2.dp, Color(0xFF0D9488).copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {}

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F2937)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (callState) {
                            is CallState.RecordingCaller -> Icons.Default.Mic
                            is CallState.SpeakingResponse -> Icons.Default.VolumeUp
                            else -> Icons.Default.Person
                        },
                        contentDescription = "Caller Status Icon",
                        tint = when (callState) {
                            is CallState.RecordingCaller -> Color(0xFFEF4444) // Red mic for active record
                            is CallState.SpeakingResponse -> Color(0xFF34D399) // Green speaker for speech
                            else -> Color.White
                        },
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            // Real-time Action status block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val stateText = when (callState) {
                    is CallState.Incoming -> "Ringing..."
                    is CallState.Answering -> callState.status
                    is CallState.RecordingCaller -> "Recording Caller's Voice... ${callState.durationLeft}s"
                    is CallState.SpeakingResponse -> "AI Telugu Voice Answering..."
                    is CallState.Completed -> "Call Log Securely Saved"
                    else -> ""
                }

                Text(
                    text = stateText,
                    color = Color(0xFF38BDF8),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // If recording or speaking, show real progress simulation
                if (callState is CallState.RecordingCaller) {
                    LinearProgressIndicator(
                        progress = { callState.durationLeft / 6f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(100.dp)),
                        color = Color(0xFF0D9488),
                        trackColor = Color(0xFF374151),
                    )
                }

                // Show spoken text if speaking Telugu
                if (callState is CallState.SpeakingResponse) {
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = callState.text,
                            color = Color(0xFF34D399),
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Default,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Show summary if completed
                if (callState is CallState.Completed) {
                    Surface(
                        color = Color(0xFF111827),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "E2E Encrypted Call Summary:",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = callState.summary,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Decline / End Simulation Button
            Button(
                onClick = onEndCall,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = CircleShape,
                modifier = Modifier
                    .size(64.dp)
                    .testTag("end_simulation_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}


// --- MAIN DASHBOARD SCREEN ---

@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    hasRecordPermission: Boolean
) {
    val callLogs by viewModel.callLogs.collectAsState()
    val faqs by viewModel.faqs.collectAsState()
    val priorityContacts by viewModel.priorityContacts.collectAsState()
    val highContrastMode by viewModel.isHighContrastMode.collectAsState()
    val isAiPaused by viewModel.isAiPaused.collectAsState()

    // Upgraded States
    val currentUserProfile by viewModel.currentUserProfile.collectAsState()
    val contactsList by viewModel.sortedFilteredContacts.collectAsState()
    val contactsSearchQuery by viewModel.contactsSearchQuery.collectAsState()
    val contactsSortOrder by viewModel.contactsSortOrder.collectAsState()
    val dialerInput by viewModel.dialerInput.collectAsState()
    val missedCallsAlerts by viewModel.missedCallsAlerts.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Keypad, 1: History, 2: Contacts, 3: Bot, 4: Settings

    // Pulsing LED status dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_led")
    val ledAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ledAlpha"
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = if (highContrastMode) Color.Black else MaterialTheme.colorScheme.surface,
                contentColor = BeigePrimary,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Dialpad, contentDescription = "Keypad") },
                    label = { Text("Keypad", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BeigePrimary,
                        selectedTextColor = BeigePrimary,
                        indicatorColor = BeigePrimary.copy(alpha = 0.18f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = {
                        Box {
                            Icon(Icons.Default.History, contentDescription = "Logs")
                            if (missedCallsAlerts.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color.Red, CircleShape)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    },
                    label = { Text("History", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BeigePrimary,
                        selectedTextColor = BeigePrimary,
                        indicatorColor = BeigePrimary.copy(alpha = 0.18f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Contacts") },
                    label = { Text("Contacts", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BeigePrimary,
                        selectedTextColor = BeigePrimary,
                        indicatorColor = BeigePrimary.copy(alpha = 0.18f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Bot") },
                    label = { Text("Bot Rules", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BeigePrimary,
                        selectedTextColor = BeigePrimary,
                        indicatorColor = BeigePrimary.copy(alpha = 0.18f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 4,
                    onClick = { activeTab = 4 },
                    icon = { Icon(Icons.Default.SettingsApplications, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BeigePrimary,
                        selectedTextColor = BeigePrimary,
                        indicatorColor = BeigePrimary.copy(alpha = 0.18f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (highContrastMode) Color.Black else MaterialTheme.colorScheme.background)
        ) {
            // Header Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(if (highContrastMode) Color.Black else BeigePrimary)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "MB Connect",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .alpha(ledAlpha)
                                    .background(Color(0xFF00E676), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Secure E2E Dialer Core",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD4B483)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // High Contrast Toggle
                        IconButton(
                            onClick = { viewModel.toggleHighContrastMode() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (highContrastMode) Icons.Default.BrightnessHigh else Icons.Default.Contrast,
                                contentDescription = "Contrast Toggle",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Status pause/resume
                        IconButton(
                            onClick = { viewModel.toggleAiPause() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isAiPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = "Pause Bot",
                                tint = if (isAiPaused) Color.Red else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Core Layout Body based on tab selection
            when (activeTab) {
                0 -> {
                    // TAB 0: KEYPAD / T9 DIALER
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 1. Live Number Display
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dialerInput.ifEmpty { "Enter Number" },
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dialerInput.isEmpty()) Color.Gray else if (highContrastMode) Color.White else BeigePrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )

                            // Quick contact save shortcut if number typed and doesn't exist
                            if (dialerInput.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        viewModel.addContact("New Contact", dialerInput, "Added from Keypad")
                                        activeTab = 2 // jump to contacts
                                    }
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add to Contacts", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BeigePrimary)
                                }
                            }
                        }

                        // 2. Real-time dialer search match suggestions list
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        ) {
                            if (dialerInput.isNotEmpty()) {
                                val matches = contactsList.filter {
                                    it.phoneNumber.contains(dialerInput) || it.name.contains(dialerInput)
                                }
                                if (matches.isNotEmpty()) {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(matches.take(2)) { contact ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        viewModel.startOutgoingCall(contact.phoneNumber, contact.name)
                                                    }
                                                    .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Person, null, tint = BeigePrimary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(contact.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BeigePrimary)
                                                }
                                                Text(contact.phoneNumber, fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                } else {
                                    Text("No matching contacts found", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Center))
                                }
                            }
                        }

                        // 3. Grid Keys Panel
                        val keys = listOf(
                            Pair("1", ""), Pair("2", "ABC"), Pair("3", "DEF"),
                            Pair("4", "GHI"), Pair("5", "JKL"), Pair("6", "MNO"),
                            Pair("7", "PQRS"), Pair("8", "TUV"), Pair("9", "WXYZ"),
                            Pair("*", ""), Pair("0", "+"), Pair("#", "")
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (row in 0..3) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    for (col in 0..2) {
                                        val key = keys[row * 3 + col]
                                        Surface(
                                            onClick = { viewModel.onDialKey(key.first) },
                                            color = if (highContrastMode) Color.Black else Color(0xFFFCFAF5),
                                            border = BorderStroke(1.dp, if (highContrastMode) Color.White else Color(0xFFD4B483).copy(alpha = 0.5f)),
                                            shape = CircleShape,
                                            modifier = Modifier.size(64.dp)
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Text(
                                                    text = key.first,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (highContrastMode) Color.White else BeigePrimary
                                                )
                                                if (key.second.isNotEmpty()) {
                                                    Text(
                                                        text = key.second,
                                                        fontSize = 9.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Action Bar (Start Call & Backspace)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Clear All Button
                            IconButton(
                                onClick = { viewModel.dialerInput.value = "" },
                                enabled = dialerInput.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear All",
                                    tint = if (dialerInput.isNotEmpty()) Color.Red else Color.LightGray
                                )
                            }

                            // Green Ring Call trigger
                            IconButton(
                                onClick = { 
                                    if (dialerInput.isNotEmpty()) {
                                        viewModel.startOutgoingCall(dialerInput)
                                    }
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFF25D366), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Start Call",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Backspace Button
                            IconButton(
                                onClick = { viewModel.onDialBackspace() },
                                enabled = dialerInput.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Backspace",
                                    tint = if (dialerInput.isNotEmpty()) if (highContrastMode) Color.White else BeigePrimary else Color.LightGray
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 1: SECURE CALL LOGS / HISTORY
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // A. Missed Call Notifications & Alerts Strip
                        if (missedCallsAlerts.isNotEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFCE8E6)),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Notifications, "Missed Call Alert", tint = Color(0xFFC5221F))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "Missed Call Notification Alert!",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFFC5221F)
                                                )
                                            }
                                            TextButton(onClick = { viewModel.clearAllMissedCalls() }) {
                                                Text("Clear All", fontSize = 11.sp, color = Color(0xFFC5221F))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        missedCallsAlerts.forEach { alert ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text(alert.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text(alert.phoneNumber, fontSize = 10.sp, color = Color.Gray)
                                                }
                                                Row {
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.startOutgoingCall(alert.phoneNumber, alert.name)
                                                            viewModel.clearMissedCallAlert(alert.id)
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.Phone, "Call Back", tint = Color(0xFF137333), modifier = Modifier.size(16.dp))
                                                    }
                                                    IconButton(
                                                        onClick = { viewModel.clearMissedCallAlert(alert.id) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.Close, "Dismiss", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // B. Shield E2EE Dashboard Header
                        item {
                            LogDashboardHeader(logsCount = callLogs.size, highContrastMode = highContrastMode)
                        }

                        // NEW: Visual Call Analytics Dashboard Card
                        item {
                            CallLogsAnalyticsPanel(callLogs = callLogs, highContrastMode = highContrastMode)
                        }

                        // C. Logs List View
                        if (callLogs.isEmpty()) {
                            item {
                                EmptyStateBlock(
                                    icon = Icons.Default.PhoneCallback,
                                    title = "No Sealed Local Logs",
                                    subtitle = "All your VoIP, SIM, and dialed calls are secured using 256-bit AES offline files."
                                )
                            }
                        } else {
                            items(callLogs) { log ->
                                CallLogCard(
                                    log = log,
                                    onDelete = { viewModel.simulateCall(log.phoneNumber, log.contactName, log.callType) },
                                    onDeleteLog = { viewModel.deleteCallLog(log.id) }
                                )
                            }

                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Button(
                                        onClick = { viewModel.clearAllCallLogs() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteForever, null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Wipe Cryptographic Database")
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 2: CONTACTS & FAVORITES MANAGEMENT
                    var showAddContactBlock by remember { mutableStateOf(false) }
                    var newName by remember { mutableStateOf("") }
                    var newPhone by remember { mutableStateOf("") }
                    var newNote by remember { mutableStateOf("") }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Search and Filter controls
                        item {
                            OutlinedTextField(
                                value = contactsSearchQuery,
                                onValueChange = { viewModel.contactsSearchQuery.value = it },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                label = { Text("Search Name or Number") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        // Sorting chips
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = contactsSortOrder == "NAME_ASC",
                                        onClick = { viewModel.contactsSortOrder.value = "NAME_ASC" },
                                        label = { Text("A-Z") }
                                    )
                                    FilterChip(
                                        selected = contactsSortOrder == "NAME_DESC",
                                        onClick = { viewModel.contactsSortOrder.value = "NAME_DESC" },
                                        label = { Text("Z-A") }
                                    )
                                }

                                Button(
                                    onClick = { showAddContactBlock = !showAddContactBlock },
                                    colors = ButtonDefaults.buttonColors(containerColor = BeigePrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (showAddContactBlock) "Close Form" else "Add Contact", fontSize = 11.sp)
                                }
                            }
                        }

                        // Custom Add Contact Form
                        if (showAddContactBlock) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (highContrastMode) Color.Black else Color(0xFFFCFAF5)),
                                    border = BorderStroke(1.dp, Color(0xFFD4B483)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Create New Offline Contact", fontWeight = FontWeight.Bold, color = BeigePrimary)
                                        OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Contact Name") }, singleLine = true)
                                        OutlinedTextField(value = newPhone, onValueChange = { newPhone = it }, label = { Text("Phone Number") }, singleLine = true)
                                        OutlinedTextField(value = newNote, onValueChange = { newNote = it }, label = { Text("Notes (Designation/Context)") }, singleLine = true)

                                        Button(
                                            onClick = {
                                                if (newName.isNotBlank() && newPhone.isNotBlank()) {
                                                    viewModel.addContact(newName, newPhone, newNote)
                                                    newName = ""
                                                    newPhone = ""
                                                    newNote = ""
                                                    showAddContactBlock = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = BeigePrimary),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Save Local Contact")
                                        }
                                    }
                                }
                            }
                        }

                        // Favorites Title and Management
                        val favorites = contactsList.filter { it.isFavorite }
                        if (favorites.isNotEmpty()) {
                            item {
                                Text("Starred / Favorite Contacts", fontWeight = FontWeight.Black, color = BeigePrimary, fontSize = 14.sp)
                            }

                            items(favorites) { contact ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9)),
                                    border = BorderStroke(1.dp, Color(0xFFD4B483)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(Color(0xFFD4B483).copy(alpha = 0.3f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(contact.name.take(2).uppercase(), fontWeight = FontWeight.Bold, color = BeigePrimary)
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(contact.phoneNumber, fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { viewModel.toggleContactFavorite(contact.id) }) {
                                                Icon(Icons.Default.Star, "Unstar", tint = Color(0xFFFFD54F))
                                            }
                                            IconButton(
                                                onClick = { viewModel.startOutgoingCall(contact.phoneNumber, contact.name) },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(Color(0xFF25D366).copy(alpha = 0.15f), CircleShape)
                                            ) {
                                                Icon(Icons.Default.Phone, "Call", tint = Color(0xFF137333), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Regular Contacts Title
                        item {
                            Text("All Local Contacts", fontWeight = FontWeight.Black, color = BeigePrimary, fontSize = 14.sp)
                        }

                        if (contactsList.isEmpty()) {
                            item {
                                EmptyStateBlock(
                                    icon = Icons.Default.Person,
                                    title = "No Contacts Found",
                                    subtitle = "Add offline contacts to manage prioritize routing haptics easily."
                                )
                            }
                        } else {
                            items(contactsList) { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White, RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFFEFEBE9), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(BeigeSecondary.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(contact.name.take(2).uppercase(), fontWeight = FontWeight.Bold, color = BeigePrimary)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(contact.phoneNumber, fontSize = 11.sp, color = Color.Gray)
                                            if (contact.note.isNotEmpty()) {
                                                Text(contact.note, fontSize = 9.sp, color = BeigeSecondary)
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { viewModel.toggleContactFavorite(contact.id) }) {
                                            Icon(
                                                imageVector = if (contact.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = "Star",
                                                tint = if (contact.isFavorite) Color(0xFFFFD54F) else Color.LightGray
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.startOutgoingCall(contact.phoneNumber, contact.name) },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(Color(0xFF25D366).copy(alpha = 0.1f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Phone, "Call", tint = Color(0xFF137333), modifier = Modifier.size(14.dp))
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteContact(contact.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // TAB 3: BOT LAUNCH RULES & FAQ DATABASE
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // A. Priya Agent status
                        item {
                            PriyaAgentStatusCard(
                                isPaused = isAiPaused,
                                onTogglePause = { viewModel.toggleAiPause() }
                            )
                        }

                        // B. Priorities block
                        item {
                            Text("Priority Routing Contact Actions", fontWeight = FontWeight.Black, color = BeigePrimary)
                        }

                        item {
                            AddPriorityBlock(onAddPriority = { num, name, action -> 
                                viewModel.addPriorityContact(num, name, action) 
                            })
                        }

                        if (priorityContacts.isEmpty()) {
                            item {
                                EmptyStateBlock(
                                    icon = Icons.Default.Contacts,
                                    title = "No Custom Routing Rules",
                                    subtitle = "Auto-Answer SIM/WhatsApp calls or bypass the voice agent."
                                )
                            }
                        } else {
                            items(priorityContacts) { contact ->
                                PriorityContactCard(contact = contact, onDelete = { viewModel.deletePriorityContact(contact.id) })
                            }
                        }

                        // C. Telugu FAQs list
                        item {
                            Text("Telugu AI FAQ Knowledge Database", fontWeight = FontWeight.Black, color = BeigePrimary)
                        }

                        item {
                            AddFaqBlock(onAddFaq = { q, a -> viewModel.addFaq(q, a) })
                        }

                        if (faqs.isEmpty()) {
                            item {
                                EmptyStateBlock(
                                    icon = Icons.Default.Quiz,
                                    title = "FAQ Base is Empty",
                                    subtitle = "Provide common query triggers in Telugu script to guide automatically answered logs."
                                )
                            }
                        } else {
                            items(faqs) { faq ->
                                FaqCard(faq = faq, onDelete = { viewModel.deleteFaq(faq.id) })
                            }
                        }
                    }
                }
                4 -> {
                    // TAB 4: SECURE SETTINGS & SIMULATION CHEATS
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // User Card Info
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (highContrastMode) Color.Black else Color(0xFFFCFAF5)),
                            border = BorderStroke(1.dp, Color(0xFFD4B483)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(BeigePrimary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentUserProfile.name.take(2).uppercase(),
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            fontSize = 18.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(currentUserProfile.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BeigePrimary)
                                        Text(currentUserProfile.phone, fontSize = 12.sp, color = Color.Gray)
                                        Text(currentUserProfile.email, fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        // Accessibility Switches
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (highContrastMode) Color.Black else Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Accessibility Options", fontWeight = FontWeight.Bold, color = BeigePrimary)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("High Contrast Theme", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Strict black/white outlines for visual aid", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Switch(
                                        checked = highContrastMode,
                                        onCheckedChange = { viewModel.toggleHighContrastMode() }
                                    )
                                }
                            }
                        }

                        // AI Voice Receptionist Persona Customizer
                        val selectedPersona by viewModel.selectedPersona.collectAsState()
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (highContrastMode) Color.Black else Color.White),
                            border = BorderStroke(1.dp, if (highContrastMode) Color.White else Color(0xFFE0E0E0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.RecordVoiceOver, null, tint = BeigePrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Telugu AI Receptionist Voice", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BeigePrimary)
                                }
                                
                                Text("Choose your preferred AI answering persona and speed:", fontSize = 12.sp, color = Color.Gray)
                                
                                viewModel.voicePersonas.forEach { persona ->
                                    val isSelected = selectedPersona.id == persona.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = if (isSelected) BeigePrimary.copy(alpha = 0.12f) else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.selectedPersonaId.value = persona.id }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { viewModel.selectedPersonaId.value = persona.id },
                                                colors = RadioButtonDefaults.colors(selectedColor = BeigePrimary)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Column {
                                                Text(persona.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(persona.description, fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }
                                        
                                        // Play Voice Introduction Test Button
                                        IconButton(
                                            onClick = { viewModel.previewVoicePersona(persona) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "Preview voice",
                                                tint = BeigePrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Simulation / Testing Controls
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
                            border = BorderStroke(1.dp, Color(0xFFFBC02D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Help, null, tint = Color(0xFFF57F17))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Simulation Portal (Interactive Testing)", fontWeight = FontWeight.Black, color = BeigePrimary)
                                }

                                Text("Test your Telugu Answering and Missed call alerts instantly:", fontSize = 12.sp)

                                Button(
                                    onClick = { viewModel.simulateCall("+91 94405 12345", "Kalyan Ram", "SIM") },
                                    colors = ButtonDefaults.buttonColors(containerColor = BeigePrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Simulate Incoming SIM Call", fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Button(
                                    onClick = { viewModel.simulateCall("+91 81234 56789", "Sravani", "WhatsApp") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Simulate Incoming WhatsApp Call", fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Button(
                                    onClick = { 
                                        viewModel.addSimulatedMissedCall("Ravi Teja", "+91 98765 43210")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Trigger Missed Call Alert Badge", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sign Out Button to return to Splash/Auth screens
                        OutlinedButton(
                            onClick = { viewModel.currentScreen.value = AppScreen.SPLASH },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BeigePrimary),
                            border = BorderStroke(1.dp, BeigePrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out", tint = BeigePrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign Out (Go to Splash / Login)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- CALL SIMULATION CONTROLLER PANEL ---

@Composable
fun CallSimulationPanel(
    onTriggerSim: (String, String, String) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("+91 98765 43210") }
    var callerName by remember { mutableStateOf("Ravi Teja") }
    var callType by remember { mutableStateOf("SIM") } // "SIM" or "WhatsApp"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.SettingsPhone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Interactive Call Answering Simulator",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Simulate a live phone or WhatsApp call to verify real-time voice answering, Gemini Telugu responses, and local secure encryption pipeline.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = callerName,
                    onValueChange = { callerName = it },
                    label = { Text("Caller Name") },
                    textStyle = TextStyle(fontSize = 14.sp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sim_caller_name"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    textStyle = TextStyle(fontSize = 14.sp),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("sim_caller_number"),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Choose SIM or WhatsApp Call type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Call Platform:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { callType = "SIM" }
                ) {
                    RadioButton(
                        selected = callType == "SIM",
                        onClick = { callType = "SIM" },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                    Text("SIM Telephony", fontSize = 13.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { callType = "WhatsApp" }
                ) {
                    RadioButton(
                        selected = callType == "WhatsApp",
                        onClick = { callType = "WhatsApp" },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF25D366))
                    )
                    Text("WhatsApp", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onTriggerSim(phoneNumber, callerName, callType) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("simulate_call_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Trigger simulation"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Launch Active simulated Call",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --- STATS ROW COMPONENT ---

@Composable
fun StatsRow(
    logsCount: Int,
    faqsCount: Int,
    priorityCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(modifier = Modifier.weight(1f), count = logsCount.toString(), label = "Secure Logs", icon = Icons.Default.History, tint = Color(0xFF38BDF8))
        StatCard(modifier = Modifier.weight(1f), count = faqsCount.toString(), label = "Telugu FAQs", icon = Icons.Default.QuestionAnswer, tint = Color(0xFF34D399))
        StatCard(modifier = Modifier.weight(1f), count = priorityCount.toString(), label = "Priority Lists", icon = Icons.Default.Bookmark, tint = Color(0xFFFBBF24))
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    count: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = count,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// --- EMPTY STATE CARD ---

@Composable
fun EmptyStateBlock(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

// --- LOG CARD COMPONENT ---

@Composable
fun CallLogCard(
    log: DecryptedCallLog,
    onDelete: () -> Unit,
    onDeleteLog: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Platform Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (log.callType == "WhatsApp") Color(0xFF25D366).copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (log.callType == "WhatsApp") Icons.Default.Chat else Icons.Default.Phone,
                        contentDescription = log.callType,
                        tint = if (log.callType == "WhatsApp") Color(0xFF25D366) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Caller details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = log.contactName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = log.phoneNumber,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = log.callType,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Call status tag
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = when (log.status) {
                            "Answered by AI" -> Color(0xFFE6F4EA)
                            "Auto-Replied" -> Color(0xFFE8F0FE)
                            "Bypassed" -> Color(0xFFFEF7E0)
                            else -> Color(0xFFFCE8E6)
                        },
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = log.status,
                            color = when (log.status) {
                                "Answered by AI" -> Color(0xFF137333)
                                "Auto-Replied" -> Color(0xFF1A73E8)
                                "Bypassed" -> Color(0xFFB06000)
                                else -> Color(0xFFC5221F)
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(log.timestamp)),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Concise Summary block with custom "Professional Polish" thick left border style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF25232A)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // The thick left border
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFD0BCFF))
                )
                
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Summarize,
                        contentDescription = "Summary",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log.summary,
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            // Expandable details (Audio player, transcripts, custom actions)
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                // Transcribed section
                Text(
                    text = "Caller voice transcription:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = "\"${log.transcription}\"",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Bot suggested response section
                if (log.suggestedReply.isNotEmpty()) {
                    Text(
                        text = "AI Telugu response spoken:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34D399)
                    )
                    Surface(
                        color = Color(0xFF1F2937),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    ) {
                        Text(
                            text = log.suggestedReply,
                            color = Color(0xFF34D399),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Interactive Audio recording playback control
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "Play Recording",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Call Audio Recording", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(log.audioFilePath, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                        }
                        Surface(
                            color = Color(0xFF34D399).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "SECURE LOCAL FILE",
                                color = Color(0xFF059669),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Delete Log Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDeleteLog,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete Encrypted Record", fontSize = 12.sp)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tap to expand details",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}


// --- BOT FAQ CONFIGURATION PANEL ---

@Composable
fun AddFaqBlock(
    onAddFaq: (String, String) -> Unit
) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Add custom FAQ for Bot",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Provide questions and answers. The AI bot uses these answers for auto-reply matching.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Customer Question (English or Telugu)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("faq_question_input"),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                label = { Text("Response to give (Type Telugu script for local TTS)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("faq_answer_input"),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (question.isNotEmpty() && answer.isNotEmpty()) {
                        onAddFaq(question, answer)
                        question = ""
                        answer = ""
                    }
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag("add_faq_button"),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save FAQ to Bot Database")
            }
        }
    }
}

@Composable
fun FaqCard(
    faq: DecryptedFaq,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Quiz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Q: ${faq.question}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "A: ${faq.answer}",
                    fontSize = 13.sp,
                    color = Color(0xFF0D9488),
                    fontFamily = FontFamily.Default
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete FAQ",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}


// --- PRIORITIES & ROUTING CONFIGURATION PANEL ---

@Composable
fun AddPriorityBlock(
    onAddPriority: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedAction by remember { mutableStateOf("AUTO_ANSWER") } // "AUTO_ANSWER", "BYPASS_BOT", "REJECT", "AUTO_REPLY"

    val actions = listOf(
        "AUTO_ANSWER" to "Auto-Answer by Bot",
        "BYPASS_BOT" to "Direct Ring Through",
        "REJECT" to "Auto-Reject (Block)",
        "AUTO_REPLY" to "Auto-Reply suggested text"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Manage Priority Contact Routing",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Route specific numbers automatically based on priority lists.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact Name") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("priority_name_input"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("priority_phone_input"),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Select routing action to trigger:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            // Radio selections for route action
            actions.forEach { (actionKey, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedAction = actionKey }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedAction == actionKey,
                        onClick = { selectedAction = actionKey }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = label, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (name.isNotEmpty() && phoneNumber.isNotEmpty()) {
                        onAddPriority(phoneNumber, name, selectedAction)
                        name = ""
                        phoneNumber = ""
                    }
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag("save_priority_button"),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Routing Rule")
            }
        }
    }
}

@Composable
fun PriorityContactCard(
    contact: DecryptedPriorityContact,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = contact.phoneNumber,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // Action Label Pill
            Surface(
                color = when (contact.routeAction) {
                    "AUTO_ANSWER" -> Color(0xFFE6F4EA)
                    "BYPASS_BOT" -> Color(0xFFFEF7E0)
                    "REJECT" -> Color(0xFFFCE8E6)
                    else -> Color(0xFFE8F0FE)
                },
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = when (contact.routeAction) {
                        "AUTO_ANSWER" -> "Auto Answer"
                        "BYPASS_BOT" -> "Bypass Bot"
                        "REJECT" -> "Auto-Reject"
                        else -> "Auto-Reply"
                    },
                    color = when (contact.routeAction) {
                        "AUTO_ANSWER" -> Color(0xFF137333)
                        "BYPASS_BOT" -> Color(0xFFB06000)
                        "REJECT" -> Color(0xFFC5221F)
                        else -> Color(0xFF1A73E8)
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete contact rule",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// --- PRIYA AI AGENT STATUS COMPONENT (Professional Polish) ---
@Composable
fun PriyaAgentStatusCard(
    isPaused: Boolean,
    onTogglePause: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2B2930)
        ),
        border = BorderStroke(1.dp, Color(0xFF3D3A42)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "AI AGENT STATUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCAC4D0),
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Priya",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(Telugu/Female)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD0BCFF)
                        )
                    }
                }

                // Status tag
                Surface(
                    color = Color(0xFF1C1B1F),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.dp, Color(0xFFD0BCFF))
                ) {
                    Text(
                        text = if (isPaused) "AI PAUSED" else "OFFLINE READY",
                        color = Color(0xFFD0BCFF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text(
                            text = "SIM TELEPHONY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF938F99)
                        )
                        Text(
                            text = if (isPaused) "Paused" else "Active",
                            color = if (isPaused) Color(0xFFEF4444) else Color(0xFF00E676),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .width(1.dp)
                            .background(Color(0xFF444444))
                    )
                    Column {
                        Text(
                            text = "WHATSAPP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF938F99)
                        )
                        Text(
                            text = if (isPaused) "Paused" else "Active",
                            color = if (isPaused) Color(0xFFEF4444) else Color(0xFF00E676),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Pause button
                Button(
                    onClick = onTogglePause,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPaused) Color(0xFF1C1B1F) else Color(0xFFD0BCFF),
                        contentColor = if (isPaused) Color(0xFFD0BCFF) else Color(0xFF381E72)
                    ),
                    border = if (isPaused) BorderStroke(1.dp, Color(0xFFD0BCFF)) else null,
                    shape = RoundedCornerShape(100.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isPaused) "ACTIVATE AI" else "PAUSE AI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// --- LOG DASHBOARD SECURE HEADER (Professional Polish) ---
@Composable
fun LogDashboardHeader(
    logsCount: Int,
    highContrastMode: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highContrastMode) Color.Black else Color(0xFF1C1B1F)
        ),
        border = BorderStroke(1.dp, if (highContrastMode) Color.White else Color(0xFF333333)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RECENT INTELLIGENCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (highContrastMode) Color.White else Color(0xFFCAC4D0),
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF00E676), CircleShape)
                    )
                    Text(
                        text = if (logsCount > 0) "E2EE Active ($logsCount Sealed)" else "E2EE Sealed Engine Ready",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (highContrastMode) Color.White else Color(0xFF938F99)
                    )
                }
            }

            // Shield visual indicator
            Surface(
                color = if (highContrastMode) Color.Black else Color(0xFF25232A),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (highContrastMode) Color.White else Color(0xFF3D3A42)),
                modifier = Modifier
                    .testTag("log_header_shield")
                    .padding(start = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "E2EE Status Shield",
                        tint = if (logsCount > 0) Color(0xFF00E676) else Color(0xFFD0BCFF),
                        modifier = Modifier
                            .size(18.dp)
                            .alpha(pulseAlpha)
                    )
                    Text(
                        text = if (logsCount > 0) "SECURED" else "VERIFIED",
                        color = if (logsCount > 0) Color(0xFF00E676) else Color(0xFFD0BCFF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

// --- VISUAL CALL LOGS ANALYTICS PANEL (Canvas donut + channel telemetry) ---
@Composable
fun CallLogsAnalyticsPanel(
    callLogs: List<DecryptedCallLog>,
    highContrastMode: Boolean
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = if (highContrastMode) Color.Black else Color.White),
        border = BorderStroke(1.dp, if (highContrastMode) Color.White else Color(0xFFD4B483).copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Toggle Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Analytics",
                        tint = BeigePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Real-time Call Analytics",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = BeigePrimary
                    )
                }
                
                IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle",
                        tint = BeigePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                if (callLogs.isEmpty()) {
                    Text(
                        text = "No call logs yet to analyze. Try simulating a call first!",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    // Compute statistics
                    val totalCalls = callLogs.size
                    val aiAnsweredCount = callLogs.count { it.status == "Answered by AI" }
                    val bypassedCount = callLogs.count { it.status == "Bypassed" }
                    val autoRepliedCount = callLogs.count { it.status == "Auto-Replied" || it.status == "Rejected" }
                    val missedCount = totalCalls - aiAnsweredCount - bypassedCount - autoRepliedCount
                    
                    val simCount = callLogs.count { it.callType == "SIM" }
                    val whatsappCount = callLogs.count { it.callType == "WhatsApp" }
                    
                    // Display Row of Stat Pill Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .border(0.5.dp, BeigePrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Answered by AI", fontSize = 9.sp, color = Color.Gray, maxLines = 1)
                            Text("$aiAnsweredCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BeigePrimary)
                        }
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .border(0.5.dp, BeigePrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Bypassed Bot", fontSize = 9.sp, color = Color.Gray, maxLines = 1)
                            Text("$bypassedCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
                        }
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .border(0.5.dp, BeigePrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Auto-Action", fontSize = 9.sp, color = Color.Gray, maxLines = 1)
                            Text("$autoRepliedCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Beautiful Custom Donut Chart + Legends
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Jetpack Compose Canvas Donut Chart
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.size(100.dp)) {
                                val strokeWidth = 14.dp.toPx()
                                val aiSweep = if (totalCalls > 0) (aiAnsweredCount.toFloat() / totalCalls) * 360f else 0f
                                val bypassedSweep = if (totalCalls > 0) (bypassedCount.toFloat() / totalCalls) * 360f else 0f
                                val autoSweep = if (totalCalls > 0) (autoRepliedCount.toFloat() / totalCalls) * 360f else 0f
                                val missedSweep = if (totalCalls > 0) (missedCount.coerceAtLeast(0).toFloat() / totalCalls) * 360f else 0f
                                
                                var startAngle = -90f
                                
                                // Draw AI segment (BeigePrimary / Orange Accent)
                                if (aiSweep > 0) {
                                    drawArc(
                                        color = Color(0xFFE5A93B),
                                        startAngle = startAngle,
                                        sweepAngle = aiSweep,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                                    )
                                    startAngle += aiSweep
                                }
                                
                                // Draw Bypassed segment (Blue Accent)
                                if (bypassedSweep > 0) {
                                    drawArc(
                                        color = Color(0xFF0288D1),
                                        startAngle = startAngle,
                                        sweepAngle = bypassedSweep,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                                    )
                                    startAngle += bypassedSweep
                                }
                                
                                // Draw Auto-Replied segment (Green Accent)
                                if (autoSweep > 0) {
                                    drawArc(
                                        color = Color(0xFF388E3C),
                                        startAngle = startAngle,
                                        sweepAngle = autoSweep,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                                    )
                                    startAngle += autoSweep
                                }
                                
                                // Draw Missed segment (Red Accent)
                                if (missedSweep > 0) {
                                    drawArc(
                                        color = Color(0xFFEF5350),
                                        startAngle = startAngle,
                                        sweepAngle = missedSweep,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                                    )
                                }
                            }
                            
                            // Center Text of Donut
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$totalCalls", fontSize = 16.sp, fontWeight = FontWeight.Black, color = BeigePrimary)
                                Text("Total Calls", fontSize = 8.sp, color = Color.Gray)
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Legends and percentages
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Call Distribution Status:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            
                            LegendItem(color = Color(0xFFE5A93B), label = "AI Answered: ${if (totalCalls > 0) (aiAnsweredCount * 100 / totalCalls) else 0}%")
                            LegendItem(color = Color(0xFF0288D1), label = "Bypassed: ${if (totalCalls > 0) (bypassedCount * 100 / totalCalls) else 0}%")
                            LegendItem(color = Color(0xFF388E3C), label = "Auto-Action: ${if (totalCalls > 0) (autoRepliedCount * 100 / totalCalls) else 0}%")
                            if (missedCount > 0) {
                                LegendItem(color = Color(0xFFEF5350), label = "Other/Missed: ${if (totalCalls > 0) (missedCount * 100 / totalCalls) else 0}%")
                            }
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.3f))
                    
                    // Channel Telemetry Sizing
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Channel Origin Analytics", 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold, 
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // SIM Telephony Bar
                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("SIM Telephony", fontSize = 10.sp, color = Color.Gray)
                                    Text("$simCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                LinearProgressIndicator(
                                    progress = { if (totalCalls > 0) simCount.toFloat() / totalCalls else 0f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                    color = BeigePrimary,
                                    trackColor = Color.LightGray.copy(alpha = 0.2f)
                                )
                            }
                            
                            // WhatsApp Bar
                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("WhatsApp", fontSize = 10.sp, color = Color.Gray)
                                    Text("$whatsappCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                LinearProgressIndicator(
                                    progress = { if (totalCalls > 0) whatsappCount.toFloat() / totalCalls else 0f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                    color = Color(0xFF25D366),
                                    trackColor = Color.LightGray.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 10.sp, color = Color.DarkGray)
    }
}

// --- NEW SCREENS & HIGHFIDELITY COMPOSABLES ---

// 1. SPLASH SCREEN (Beige Theme)
@Composable
fun SplashScreen(viewModel: AppViewModel) {
    val highContrastMode by viewModel.isHighContrastMode.collectAsState()
    val currentScreen = viewModel.currentScreen

    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        delay(2000)
        currentScreen.value = AppScreen.ONBOARDING
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (highContrastMode) Color.Black else MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Floating Skip to Dashboard Button
        Button(
            onClick = { viewModel.currentScreen.value = AppScreen.DASHBOARD },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD4B483).copy(alpha = 0.2f),
                contentColor = if (highContrastMode) Color.White else BeigePrimary
            ),
            border = BorderStroke(1.dp, Color(0xFFD4B483)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .testTag("skip_to_dashboard_splash")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Skip to Dashboard", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Skip",
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Pulsing Gold Emblem
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(scale)
                    .background(Color(0xFFD4B483).copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, Color(0xFFD4B483), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Logo",
                    tint = BeigePrimary,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "MB CONNECT",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = if (highContrastMode) Color.White else BeigePrimary,
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Secure Telugu AI Call Assistant",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (highContrastMode) Color.White else BeigeSecondary,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Elegant indicator
            CircularProgressIndicator(
                color = BeigePrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(
                onClick = { currentScreen.value = AppScreen.ONBOARDING },
                colors = ButtonDefaults.textButtonColors(contentColor = BeigePrimary)
            ) {
                Text("Skip Intro →", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// 2. ONBOARDING SCREEN
@Composable
fun OnboardingScreen(viewModel: AppViewModel) {
    val highContrastMode by viewModel.isHighContrastMode.collectAsState()
    var currentSlide by remember { mutableStateOf(0) }

    val slides = listOf(
        Triple(
            Icons.Default.Phone,
            "Telugu AI Bot Receptionist",
            "Automatically answer unknown numbers, calls and WhatsApp VoIP with Priya, speaking local Telugu script TTS helper. Never miss crucial leads again."
        ),
        Triple(
            Icons.Default.Dialpad,
            "Integrated Dialer & Keypad",
            "A beautifully styled beige dialing pad with interactive keypad matching, custom contact search, alphabetical sorting, and instant Favorites."
        ),
        Triple(
            Icons.Default.FiberManualRecord,
            "AES encrypted Call Recording",
            "Toggle local audio recordings on live calls with a single tap. All tapes are securely stored offline with end-to-end local GCM keys."
        )
    )

    val activeSlide = slides[currentSlide]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (highContrastMode) Color.Black else MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .navigationBarsPadding()
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Skip Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { viewModel.currentScreen.value = AppScreen.SIGN_UP },
                colors = ButtonDefaults.textButtonColors(contentColor = BeigePrimary)
            ) {
                Text("Skip Onboarding", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = { viewModel.currentScreen.value = AppScreen.DASHBOARD },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD4B483).copy(alpha = 0.2f),
                    contentColor = if (highContrastMode) Color.White else BeigePrimary
                ),
                border = BorderStroke(1.dp, Color(0xFFD4B483)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("skip_to_dashboard_onboarding")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Skip to Dashboard", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Skip",
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Active Slide Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFFD4B483).copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = activeSlide.first,
                    contentDescription = null,
                    tint = BeigePrimary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = activeSlide.second,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = if (highContrastMode) Color.White else BeigePrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = activeSlide.third,
                fontSize = 14.sp,
                color = if (highContrastMode) Color.White else BeigePrimary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Slide Dots Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                slides.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (currentSlide == index) 16.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (currentSlide == index) BeigePrimary else Color(0xFFD4B483))
                    )
                }
            }
        }

        // Action controls
        Button(
            onClick = {
                if (currentSlide < slides.size - 1) {
                    currentSlide++
                } else {
                    viewModel.currentScreen.value = AppScreen.SIGN_UP
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = BeigePrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = if (currentSlide == slides.size - 1) "Get Started" else "Next Feature",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// 3. SIGN UP SCREEN
@Composable
fun SignUpScreen(viewModel: AppViewModel) {
    val highContrastMode by viewModel.isHighContrastMode.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var agreed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (highContrastMode) Color.Black else MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Skip to Dashboard Button
        Button(
            onClick = { viewModel.currentScreen.value = AppScreen.DASHBOARD },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD4B483).copy(alpha = 0.2f),
                contentColor = if (highContrastMode) Color.White else BeigePrimary
            ),
            border = BorderStroke(1.dp, Color(0xFFD4B483)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .testTag("skip_to_dashboard_signup")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Skip to Dashboard", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Skip",
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = BeigePrimary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Create Secure Account",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = if (highContrastMode) Color.White else BeigePrimary
            )
            Text(
                text = "Configure your offline voice assistant dialer",
                fontSize = 12.sp,
                color = if (highContrastMode) Color.White else BeigeSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = if (highContrastMode) Color.Black else Color(0xFFFCFAF5)),
                border = BorderStroke(1.dp, if (highContrastMode) Color.White else Color(0xFFD4B483).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("WhatsApp Phone Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        placeholder = { Text("+91 98765 43210") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Security PIN / Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { agreed = !agreed }
                    ) {
                        Checkbox(
                            checked = agreed,
                            onCheckedChange = { agreed = it }
                        )
                        Text(
                            text = "I agree to local AES GCM encryption guidelines.",
                            fontSize = 11.sp,
                            color = BeigeSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (fullName.isNotEmpty() && phone.isNotEmpty()) {
                                viewModel.triggerSignUp(fullName, phone, email)
                            }
                        },
                        enabled = agreed && fullName.isNotBlank() && phone.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BeigePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Register & Verify OTP", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = { 
                    viewModel.currentScreen.value = AppScreen.SIGN_IN
                },
                colors = ButtonDefaults.textButtonColors(contentColor = BeigePrimary)
            ) {
                Text("Already registered? Sign In with WhatsApp OTP", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 3.5. SIGN IN PHONE SCREEN
@Composable
fun SignInScreen(viewModel: AppViewModel) {
    val highContrastMode by viewModel.isHighContrastMode.collectAsState()
    var phone by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (highContrastMode) Color.Black else MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Skip to Dashboard Button
        Button(
            onClick = { viewModel.currentScreen.value = AppScreen.DASHBOARD },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD4B483).copy(alpha = 0.2f),
                contentColor = if (highContrastMode) Color.White else BeigePrimary
            ),
            border = BorderStroke(1.dp, Color(0xFFD4B483)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .testTag("skip_to_dashboard_signin")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Skip to Dashboard", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Skip",
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF25D366).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    tint = Color(0xFF25D366),
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Sign In with WhatsApp",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = if (highContrastMode) Color.White else BeigePrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "We'll send a 4-digit security code to your WhatsApp",
                fontSize = 12.sp,
                color = if (highContrastMode) Color.White else BeigeSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = if (highContrastMode) Color.Black else Color(0xFFFCFAF5)),
                border = BorderStroke(1.dp, if (highContrastMode) Color.White else Color(0xFFD4B483).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Enter Your WhatsApp Number",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BeigePrimary
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Mobile Number") },
                        leadingIcon = { 
                            Row(
                                modifier = Modifier.padding(start = 12.dp, end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("+91", fontWeight = FontWeight.Bold, color = BeigePrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.Gray))
                            }
                        },
                        placeholder = { Text("9876543210") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (phone.isNotBlank()) {
                                val fullPhone = if (phone.startsWith("+")) phone else "+91 $phone"
                                viewModel.triggerWhatsAppOtp(fullPhone)
                                viewModel.currentScreen.value = AppScreen.SIGN_IN_OTP
                                Toast.makeText(context, "Simulated WhatsApp OTP sent!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = phone.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send OTP via WhatsApp", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("New here?", fontSize = 13.sp, color = BeigeSecondary)
                TextButton(
                    onClick = { 
                        viewModel.currentScreen.value = AppScreen.SIGN_UP 
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = BeigePrimary)
                ) {
                    Text("Create An Account", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 4. SIGN IN WITH WHATSAPP OTP
@Composable
fun OtpScreen(viewModel: AppViewModel) {
    val highContrastMode by viewModel.isHighContrastMode.collectAsState()
    val phoneNum by viewModel.otpSentNumber.collectAsState()
    val simulatedCode by viewModel.simulatedOtpCode.collectAsState()
    val errorText by viewModel.otpError.collectAsState()

    var otpInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (highContrastMode) Color.Black else MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Floating Skip to Dashboard Button
        Button(
            onClick = { viewModel.currentScreen.value = AppScreen.DASHBOARD },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD4B483).copy(alpha = 0.2f),
                contentColor = if (highContrastMode) Color.White else BeigePrimary
            ),
            border = BorderStroke(1.dp, Color(0xFFD4B483)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .testTag("skip_to_dashboard_otp")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Skip to Dashboard", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Skip",
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Simulated WhatsApp Notification Tray (Professional Polish)
            AnimatedVisibility(
                visible = simulatedCode.isNotEmpty(),
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    border = BorderStroke(1.dp, Color(0xFF25D366)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF25D366), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Chat, "WhatsApp", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("WhatsApp Verification Agent", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), fontSize = 13.sp)
                            Text(
                                "Your MB Connect OTP verification code is: $simulatedCode. Do not share.",
                                fontSize = 12.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Icon(Icons.Default.Security, null, tint = BeigePrimary, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("OTP Verification", fontSize = 24.sp, fontWeight = FontWeight.Black, color = if (highContrastMode) Color.White else BeigePrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "We sent a simulated WhatsApp OTP to $phoneNum",
                fontSize = 13.sp,
                color = if (highContrastMode) Color.White else BeigeSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = if (highContrastMode) Color.Black else Color(0xFFFCFAF5)),
                border = BorderStroke(1.dp, if (highContrastMode) Color.White else Color(0xFFD4B483).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { if (it.length <= 4) otpInput = it },
                        label = { Text("4-Digit WhatsApp OTP") },
                        placeholder = { Text("XXXX") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp),
                        singleLine = true,
                        isError = errorText.isNotEmpty()
                    )

                    if (errorText.isNotEmpty()) {
                        Text(
                            text = errorText,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val success = viewModel.verifyOtp(otpInput)
                            if (success) {
                                Toast.makeText(context, "Welcome, verified securely!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BeigePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Verify & Setup Completed", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { 
                                viewModel.triggerWhatsAppOtp(phoneNum)
                                Toast.makeText(context, "New simulated WhatsApp code sent!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Resend OTP Code", fontWeight = FontWeight.Bold, color = BeigePrimary)
                        }
                        TextButton(
                            onClick = { viewModel.currentScreen.value = AppScreen.FORGOT_PASSWORD }
                        ) {
                            Text("Forgot Security PIN?", color = BeigeSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(
                onClick = { viewModel.currentScreen.value = AppScreen.SIGN_IN },
                colors = ButtonDefaults.textButtonColors(contentColor = BeigePrimary)
            ) {
                Text("← Change Phone Number")
            }
        }
    }
}

// 5. FORGOT PASSWORD SCREEN
@Composable
fun ForgotPasswordScreen(viewModel: AppViewModel) {
    val highContrastMode by viewModel.isHighContrastMode.collectAsState()
    var emailInput by remember { mutableStateOf("") }
    var successAlert by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (highContrastMode) Color.Black else MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Help, null, tint = BeigePrimary, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Recovery Portal", fontSize = 24.sp, fontWeight = FontWeight.Black, color = if (highContrastMode) Color.White else BeigePrimary)
            Text("Recover your dialer security keys & credentials", fontSize = 12.sp, color = BeigeSecondary)

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = if (highContrastMode) Color.Black else Color(0xFFFCFAF5)),
                border = BorderStroke(1.dp, if (highContrastMode) Color.White else Color(0xFFD4B483).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Enter account email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = { successAlert = true },
                        enabled = emailInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BeigePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Retrieve Keys", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    if (successAlert) {
                        Surface(
                            color = Color(0xFFE8F0FE),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Simulated Reset: A security recovery token has been simulated to $emailInput to restore your AES keys locally.",
                                color = Color(0xFF1A73E8),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(
                onClick = { viewModel.currentScreen.value = AppScreen.SIGN_IN },
                colors = ButtonDefaults.textButtonColors(contentColor = BeigePrimary)
            ) {
                Text("← Return to Sign-in Screen")
            }
        }
    }
}

// 6. OUTGOING CALL OVERLAY (Immersive Dialer Active Call Screen)
@Composable
fun OutgoingCallOverlay(
    viewModel: AppViewModel,
    number: String,
    name: String,
    onEndCall: () -> Unit
) {
    val isRecording by viewModel.isCallRecording.collectAsState()
    val isMuted by viewModel.isCallMuted.collectAsState()
    val isSpeaker by viewModel.isSpeakerOn.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "outgoingPulse")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1B18)) // Soft warm dark chocolaty background
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Decorative Pulsing Call Wave
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFFD4B483).copy(alpha = 0.08f),
                radius = size.minDimension / 1.5f * waveScale
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Outgoing state
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                Surface(
                    color = Color(0xFFD4B483).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF00E676), CircleShape))
                        Text(
                            text = "SECURE OUTGOING CALL",
                            color = Color(0xFFD4B483),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = name.ifEmpty { "Unknown Caller" },
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = number,
                    color = Color(0xFFD7CCC8),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Middle Status Ring
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .background(Color(0xFF2C2724), CircleShape)
                        .border(1.dp, Color(0xFFD4B483).copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Mic else Icons.Default.Phone,
                        contentDescription = null,
                        tint = if (isRecording) Color(0xFFEF4444) else Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                if (isRecording) {
                    // Blinking Recording Dot overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(16.dp)
                            .background(Color.Red, CircleShape)
                    )
                }
            }

            // Call Controller Panel (Record, Mute, Speaker, Keypad, End)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text(
                    text = if (isRecording) "RECORDING SECURE AUDIO..." else "LINE ACTIVE - SECURED BY E2EE",
                    color = if (isRecording) Color(0xFFEF4444) else Color(0xFF00E676),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { viewModel.isCallMuted.value = !isMuted },
                            modifier = Modifier
                                .size(50.dp)
                                .background(if (isMuted) Color(0xFFD4B483) else Color(0xFF2C2724), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mute",
                                tint = if (isMuted) Color(0xFF1E1B18) else Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Mute", fontSize = 11.sp, color = Color.White)
                    }

                    // Integrated Call Recording Trigger
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { viewModel.isCallRecording.value = !isRecording },
                            modifier = Modifier
                                .size(50.dp)
                                .background(if (isRecording) Color(0xFFEF4444) else Color(0xFF2C2724), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = "Record Call",
                                tint = if (isRecording) Color.White else Color(0xFFEF4444)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(if (isRecording) "Recording" else "Record Call", fontSize = 11.sp, color = Color.White)
                    }

                    // Speaker
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { viewModel.isSpeakerOn.value = !isSpeaker },
                            modifier = Modifier
                                .size(50.dp)
                                .background(if (isSpeaker) Color(0xFFD4B483) else Color(0xFF2C2724), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Speaker",
                                tint = if (isSpeaker) Color(0xFF1E1B18) else Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Speaker", fontSize = 11.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // End Call Button
                Button(
                    onClick = onEndCall,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Hang Up",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}



