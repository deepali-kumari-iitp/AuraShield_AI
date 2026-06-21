package com.aurashield.ai

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.aurashield.ai.security.BiometricAuthManager
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.aurashield.ai.service.BackgroundMonitorService
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.math.sin

class MainActivity : FragmentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPhoneStatePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Call tracking permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkAndRequestPermissions()

        setContent {
            AuraShieldTheme {
                MainNavigationFrame()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPhoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }
    }

    private fun startMonitorService() {
        val serviceIntent = Intent(this, BackgroundMonitorService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopMonitorService() {
        val serviceIntent = Intent(this, BackgroundMonitorService::class.java).apply {
            action = BackgroundMonitorService.ACTION_STOP_SERVICE
        }
        startService(serviceIntent)
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager? ?: return false
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BackgroundMonitorService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    enum class Screen {
        Onboarding, Dashboard, Detection, KillSwitch, History
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainNavigationFrame() {
        var currentScreen by remember { mutableStateOf(Screen.Onboarding) }
        var isEngineEnabled by remember { mutableStateOf(isServiceRunning()) }
        val context = LocalContext.current

        Scaffold(
            bottomBar = {
                if (currentScreen != Screen.Onboarding) {
                    NavigationBar(
                        containerColor = Color(0xFF12162E),
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == Screen.Dashboard,
                            onClick = { currentScreen = Screen.Dashboard },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                            label = { Text("Console", fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF00C896),
                                unselectedIconColor = Color(0xFFE2E8F0).copy(alpha = 0.5f),
                                indicatorColor = Color(0xFF1F2336)
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.Detection,
                            onClick = { currentScreen = Screen.Detection },
                            icon = { Icon(Icons.Default.Refresh, contentDescription = "Scanning") },
                            label = { Text("Live Monitor", fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF00C896),
                                unselectedIconColor = Color(0xFFE2E8F0).copy(alpha = 0.5f),
                                indicatorColor = Color(0xFF1F2336)
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.KillSwitch,
                            onClick = { currentScreen = Screen.KillSwitch },
                            icon = { Icon(Icons.Default.Warning, contentDescription = "Kill Switch") },
                            label = { Text("System Lock", fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFFF6B6B),
                                unselectedIconColor = Color(0xFFE2E8F0).copy(alpha = 0.5f),
                                indicatorColor = Color(0xFF1F2336)
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.History,
                            onClick = { currentScreen = Screen.History },
                            icon = { Icon(Icons.Default.List, contentDescription = "Forensics") },
                            label = { Text("Risk Log", fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF00C896),
                                unselectedIconColor = Color(0xFFE2E8F0).copy(alpha = 0.5f),
                                indicatorColor = Color(0xFF1F2336)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B0F26))
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    Screen.Onboarding -> PermissionOnboardingScreen(
                        isEngineActive = isEngineEnabled,
                        onEngineToggle = { enabled ->
                            isEngineEnabled = enabled
                            if (enabled) {
                                startMonitorService()
                                Toast.makeText(context, "Call Monitoring Engine Active", Toast.LENGTH_SHORT).show()
                            } else {
                                stopMonitorService()
                                Toast.makeText(context, "Monitoring Engine Stopped", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onNavigateToDashboard = { currentScreen = Screen.Dashboard }
                    )
                    Screen.Dashboard -> AdvancedDashboardScreen(
                        isEngineActive = isEngineEnabled,
                        onEngineToggle = { enabled ->
                            isEngineEnabled = enabled
                            if (enabled) startMonitorService() else stopMonitorService()
                        }
                    )
                    Screen.Detection -> EdgeLiveDetectionScreen()
                    Screen.KillSwitch -> SystemLockScreen()
                    Screen.History -> ForensicHistoryScreen()
                }
            }
        }
    }

    // 1. PermissionOnboardingScreen
    @Composable
    fun PermissionOnboardingScreen(
        isEngineActive: Boolean,
        onEngineToggle: (Boolean) -> Unit,
        onNavigateToDashboard: () -> Unit
    ) {
        val context = LocalContext.current
        var isOverlayEnabled by remember {
            mutableStateOf(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else {
                    true
                }
            )
        }

        // Poll overlay permission state when app resumes
        LaunchedEffect(Unit) {
            while (true) {
                isOverlayEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else {
                    true
                }
                delay(1000)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Central Logo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color(0xFF1F2336), RoundedCornerShape(60.dp))
                    .border(2.dp, Color(0xFF00C896), RoundedCornerShape(60.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFF00C896),
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "AURA SHIELD AI",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Edge-Based Call Security & Intercept Core",
                fontSize = 13.sp,
                color = Color(0xFFE2E8F0).copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // Toggle 1: Live Call Analysis Engine
            PermissionToggleCard(
                title = "Live Call Analysis Engine",
                description = "Enables real-time heuristics and voice stream analysis via TFLite models running locally.",
                checked = isEngineActive,
                onCheckedChange = { onEngineToggle(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Toggle 2: System Overlay Transaction Shield
            PermissionToggleCard(
                title = "System Overlay Transaction Shield",
                description = "Draws secure transactional covers during calls to block critical app overlays and UPI hacks.",
                checked = isOverlayEnabled,
                onCheckedChange = { checked ->
                    if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "Overlay Permission toggled in Settings", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onNavigateToDashboard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00C896),
                    contentColor = Color.Black
                )
            ) {
                Text("PROCEED TO CONSOLE", fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    fun PermissionToggleCard(
        title: String,
        description: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2336))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Text(
                        description,
                        fontSize = 11.sp,
                        color = Color(0xFFE2E8F0).copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp),
                        lineHeight = 15.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color(0xFF00C896),
                        uncheckedTrackColor = Color(0xFF0B0F26)
                    )
                )
            }
        }
    }

    // 2. AdvancedDashboardScreen
    @Composable
    fun AdvancedDashboardScreen(
        isEngineActive: Boolean,
        onEngineToggle: (Boolean) -> Unit
    ) {
        var gpayMonitored by remember { mutableStateOf(true) }
        var phonepeMonitored by remember { mutableStateOf(true) }
        var paytmMonitored by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // You Are Protected Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF00C896), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "YOU ARE PROTECTED",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // AI Protection Circular Score (92%)
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // Circle border
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFF1F2336),
                        style = Stroke(width = 12.dp.toPx())
                    )
                    drawArc(
                        color = Color(0xFF00C896),
                        startAngle = -90f,
                        sweepAngle = 360f * 0.92f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "92%",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Protection Score",
                        fontSize = 11.sp,
                        color = Color(0xFFE2E8F0).copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Registry Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF12162E))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Protected Financial Apps Registry",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        "Securing transactions during outgoing/incoming phone sessions.",
                        fontSize = 11.sp,
                        color = Color(0xFFE2E8F0).copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                    )

                    RegistryItem(name = "Google Pay (GPay)", checked = gpayMonitored, onCheckedChange = { gpayMonitored = it })
                    Divider(color = Color(0xFF1F2336), modifier = Modifier.padding(vertical = 8.dp))
                    RegistryItem(name = "PhonePe", checked = phonepeMonitored, onCheckedChange = { phonepeMonitored = it })
                    Divider(color = Color(0xFF1F2336), modifier = Modifier.padding(vertical = 8.dp))
                    RegistryItem(name = "Paytm", checked = paytmMonitored, onCheckedChange = { paytmMonitored = it })
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            var isAttackSimulated by remember { mutableStateOf(BackgroundMonitorService.isSimulatedAttackActive) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2336))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Simulate Voice Attack Trigger", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        Text(
                            "Forces 95% threat risk during an active cellular call.",
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0).copy(alpha = 0.5f),
                            lineHeight = 14.sp
                        )
                    }
                    Switch(
                        checked = isAttackSimulated,
                        onCheckedChange = { checked ->
                            isAttackSimulated = checked
                            BackgroundMonitorService.isSimulatedAttackActive = checked
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFF6B6B),
                            checkedTrackColor = Color(0xFFFF6B6B).copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }

    @Composable
    fun RegistryItem(name: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (checked) Color(0xFF00C896) else Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(name, color = Color.White, fontSize = 14.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color(0xFF00C896),
                    uncheckedTrackColor = Color(0xFF1F2336)
                )
            )
        }
    }

    // 3. EdgeLiveDetectionScreen
    @Composable
    fun EdgeLiveDetectionScreen() {
        val infiniteTransition = rememberInfiniteTransition(label = "wave")
        val wavePhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "ACTIVE SCANNING MODE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00C896),
                letterSpacing = 1.sp
            )

            Text(
                "Analyzing Incoming Stream",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Scanning visualizer canvas containing circular loader & live waveform lines
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFF12162E), RoundedCornerShape(20.dp))
                    .border(1.2.dp, Color(0xFF1F2445), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Multi-layered Canvas wave drawing
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val middle = height / 2f
                    
                    val path1 = Path()
                    val path2 = Path()
                    
                    path1.moveTo(0f, middle)
                    path2.moveTo(0f, middle)
                    
                    for (x in 0..width.toInt() step 5) {
                        val angle = (x.toFloat() / width) * 4f * Math.PI.toFloat()
                        
                        // Layer 1 path
                        val y1 = middle + sin(angle + wavePhase) * 40f
                        path1.lineTo(x.toFloat(), y1)
                        
                        // Layer 2 path (faster speed shift and different amplitude)
                        val y2 = middle + sin(angle * 1.5f - wavePhase) * 20f
                        path2.lineTo(x.toFloat(), y2)
                    }
                    
                    drawPath(
                        path = path1,
                        color = Color(0xFF00C896),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawPath(
                        path = path2,
                        color = Color(0xFF00C896).copy(alpha = 0.4f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Scanning center status tag
                Box(
                    modifier = Modifier
                        .background(Color(0xFF0B0F26).copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Voice Loop: 500ms", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Call Simulation Console Card
            var isCallSimulated by remember { mutableStateOf(BackgroundMonitorService.isSimulatedCallActive) }
            val context = LocalContext.current

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2336))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Simulate Call Stream", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        Text(
                            "Simulates active voice connection inputs for testing the GPay overlay blocking trigger.",
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0).copy(alpha = 0.5f),
                            lineHeight = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isCallSimulated,
                        onCheckedChange = { checked ->
                            isCallSimulated = checked
                            val intent = Intent(context, BackgroundMonitorService::class.java).apply {
                                action = BackgroundMonitorService.ACTION_SIMULATE_CALL
                                putExtra(BackgroundMonitorService.EXTRA_CALL_ACTIVE, checked)
                            }
                            context.startService(intent)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color(0xFF00C896),
                            uncheckedTrackColor = Color(0xFF0B0F26)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Edge inference metrics statistics card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2336))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF00C896),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Local Inference Statistics", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    InferenceInfoRow("Local Inference Latency", "14 ms")
                    Divider(color = Color(0xFF12162E), modifier = Modifier.padding(vertical = 8.dp))
                    InferenceInfoRow("Voice Buffer Window", "500 ms")
                    Divider(color = Color(0xFF12162E), modifier = Modifier.padding(vertical = 8.dp))
                    InferenceInfoRow("Model Footprint", "~15 MB (Quantized TFLite)")
                }
            }
        }
    }

    @Composable
    fun InferenceInfoRow(label: String, value: String) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color(0xFFE2E8F0).copy(alpha = 0.6f), fontSize = 13.sp)
            Text(value, color = Color(0xFF00C896), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }

    // 4. SystemLockScreen
    @Composable
    fun SystemLockScreen() {
        var isThreatActive by remember { mutableStateOf(true) }
        var secondsLeft by remember { mutableStateOf(900) } // 15 minutes = 900s
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft--
            }
        }

        val minutes = secondsLeft / 60
        val seconds = secondsLeft % 60
        val formattedTime = String.format(Locale.US, "%02d:%02d", minutes, seconds)

        if (!isThreatActive) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF00C896),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "SYSTEM BYPASS APPROVED",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
                Text(
                    text = "Identity verified successfully. Financial transaction locks have been removed and system overlay dismissed.",
                    color = Color(0xFFE2E8F0).copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                    lineHeight = 18.sp
                )
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Neon coral red alert block icon
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(Color(0xFFFF6B6B).copy(alpha = 0.1f), RoundedCornerShape(45.dp))
                    .border(2.dp, Color(0xFFFF6B6B), RoundedCornerShape(45.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFFF6B6B),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "🔒 SYSTEM INTERCEPTED",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B6B),
                letterSpacing = 1.sp
            )

            Text(
                text = "A suspected clone voice stream matched banking social engineering heuristics. Critical financial transactions have been overlay locked.",
                fontSize = 13.sp,
                color = Color(0xFFE2E8F0).copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp, bottom = 28.dp),
                lineHeight = 18.sp
            )

            // Timer display card
            Card(
                modifier = Modifier.fillMaxWidth(0.8f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2336))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LOCKOUT COUNTDOWN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedTime,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Action override buttons
            val activity = context as? FragmentActivity
            Button(
                onClick = {
                    if (activity != null) {
                        BiometricAuthManager.showBiometricPrompt(activity) {
                            isThreatActive = false
                            // Dismiss active WindowManager overlay view by sending dismiss intent to service
                            val intent = Intent(context, BackgroundMonitorService::class.java).apply {
                                action = BackgroundMonitorService.ACTION_DISMISS_OVERLAY
                            }
                            context.startService(intent)
                            Toast.makeText(context, "Bypass Auth Approved!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Requesting Local System Biometrics...", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B), contentColor = Color.White)
            ) {
                Text("Verify via Biometric Override", fontWeight = FontWeight.Bold)
            }
        }
    }

    // 5. ForensicHistoryScreen
    data class ThreatLog(
        val phone: String,
        val risk: Int,
        val timestamp: String,
        val details: String,
        val classification: String
    )

    @Composable
    fun ForensicHistoryScreen() {
        val mockLogs = remember {
            listOf(
                ThreatLog("+1 (555) 019-2834", 92, "10 mins ago", "Deepfake anomaly identified. Formant shifts match cloned model pattern.", "AI Voice Clone"),
                ThreatLog("+1 (555) 048-1920", 45, "1 hour ago", "Suspect pitch variance detected. Voice features altered.", "Pitch Variance"),
                ThreatLog("+1 (555) 012-7384", 2, "3 hours ago", "Acoustics matching authenticated signature.", "Safe Contact"),
                ThreatLog("+1 (555) 089-3829", 95, "Yesterday", "Financial request keywords mapping deepfake model payload.", "AI Clone / Scammer")
            )
        }

        var expandedIndex by remember { mutableStateOf(-1) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Forensic History Log", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "Analysis records of call voice sessions.",
                fontSize = 12.sp,
                color = Color(0xFFE2E8F0).copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 20.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(mockLogs) { index, log ->
                    ForensicLogCard(
                        log = log,
                        expanded = index == expandedIndex,
                        onToggle = {
                            expandedIndex = if (expandedIndex == index) -1 else index
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun ForensicLogCard(
        log: ThreatLog,
        expanded: Boolean,
        onToggle: () -> Unit
    ) {
        val riskColor = when {
            log.risk >= 80 -> Color(0xFFFF6B6B)
            log.risk >= 30 -> Color(0xFFFF9800)
            else -> Color(0xFF00C896)
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2336))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(log.phone, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        Text(log.timestamp, fontSize = 11.sp, color = Color(0xFFE2E8F0).copy(alpha = 0.5f))
                    }
                    Box(
                        modifier = Modifier
                            .background(riskColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .border(1.dp, riskColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("${log.risk}% RISK", color = riskColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                // Expandable sub-panel containing spectrogram placeholder
                if (expanded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color(0xFF0B0F26))
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Analyzed Mel-Spectrogram Layer", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f))

                    // Draw image-like spectrogram placeholder box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(top = 6.dp)
                            .background(Color(0xFF12162E), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val numLines = 40
                            val lineW = w / numLines
                            
                            // Draw mock heat spectrum lines
                            for (i in 0 until numLines) {
                                val lineH = h * (0.2f + 0.6f * sin(i.toFloat() * 0.4f).coerceAtLeast(0f))
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFFFF6B6B), Color(0xFFFF9800), Color(0xFF00C896))
                                    ),
                                    topLeft = androidx.compose.ui.geometry.Offset(i * lineW, h - lineH),
                                    size = androidx.compose.ui.geometry.Size(lineW - 2.dp.toPx(), lineH)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Classification", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f))
                    Text(log.classification, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Forensic Details Log", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f))
                    Text(log.details, color = Color(0xFFE2E8F0).copy(alpha = 0.8f), fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp), lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
fun AuraShieldTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = Color(0xFF00C896),       // Electric Mint Green
        secondary = Color(0xFF00C896),
        background = Color(0xFF0B0F26),      // Deep Midnight Navy
        surface = Color(0xFF1F2336),         // Container surface
        onPrimary = Color(0xFF000000),
        onBackground = Color(0xFFE2E8F0),
        onSurface = Color(0xFFFFFFFF),
        error = Color(0xFFFF6B6B),           // Neon Coral Red
        primaryContainer = Color(0xFF0D323F)
    )
    MaterialTheme(
        colorScheme = darkColorScheme,
        content = content
    )
}
