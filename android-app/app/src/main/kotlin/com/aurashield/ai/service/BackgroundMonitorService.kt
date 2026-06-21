package com.aurashield.ai.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.core.app.NotificationCompat
import com.aurashield.ai.AuraShieldApp
import com.aurashield.ai.MainActivity
import com.aurashield.ai.R
import kotlinx.coroutines.*
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class BackgroundMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isRunning = false
    private var tfliteInterpreter: Interpreter? = null
    private var isThreatBypassed = false
    private var isCallAnalysisEngineActive = true
    private var isProcessingAudioBytes = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopServiceInternal()
            return START_NOT_STICKY
        }
        
        if (intent?.action == ACTION_DISMISS_OVERLAY) {
            isThreatBypassed = true
            hideOverlay()
            return START_STICKY
        }

        if (intent?.action == ACTION_SIMULATE_CALL) {
            isSimulatedCallActive = intent.getBooleanExtra(EXTRA_CALL_ACTIVE, false)
            Log.d(TAG, "Simulated call status changed: $isSimulatedCallActive")
            return START_STICKY
        }

        if (!isRunning) {
            isRunning = true
            isThreatBypassed = false
            startForegroundServiceCompat()
            startMonitoringLoop()
        }

        return START_STICKY
    }

    private fun startForegroundServiceCompat() {
        val notification = createNotification()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ requires specifying foregroundServiceType
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val stopIntent = Intent(this, BackgroundMonitorService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Custom notification layout / styling
        return NotificationCompat.Builder(this, AuraShieldApp.CHANNEL_ID)
            .setContentTitle("AuraShield AI Monitor Active")
            .setContentText("Performing background AI heuristics...")
            // Use system icon for notification for reliable compilation without external assets
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Monitoring",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = assets.openFd("voice_detector.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun initInterpreter() {
        try {
            tfliteInterpreter = Interpreter(loadModelFile())
            Log.i(TAG, "TFLite voice_detector model loaded successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TFLite interpreter: ${e.message}", e)
        }
    }

    private fun isCallActive(context: Context): Boolean {
        if (isSimulatedCallActive) {
            return true
        }
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (telephonyManager != null) {
                @Suppress("DEPRECATION")
                if (telephonyManager.callState != TelephonyManager.CALL_STATE_IDLE) {
                    return true
                }
            }
        }
        return false
    }

    private fun getForegroundPackageName(context: Context): String? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10000 // 10-second window
        
        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        
        if (usageStatsList.isNullOrEmpty()) {
            return null
        }
        
        val sortedStats = usageStatsList.sortedByDescending { it.lastTimeUsed }
        return sortedStats.firstOrNull()?.packageName
    }

    private fun startMonitoringLoop() {
        initInterpreter()
        
        serviceScope.launch {
            // Verify and demonstrate link resolution of ML runtimes
            initMLRuntimes()
            
            val inputData = Array(1) { Array(128) { Array(128) { FloatArray(3) } } }
            val outputData = Array(1) { FloatArray(1) }
            var tickCount = 0
            
            // Fallback reset in the main initialization block to start in a completely safe state
            var riskPercentage = 0f
            isProcessingAudioBytes = false
            
            val targetFinancialApps = listOf(
                "com.google.android.apps.nbu.paisa.user", // GPay
                "com.phonepe.app",                        // PhonePe
                "net.one97.paytm"                         // Paytm
            )
            
            while (isActive) {
                val callActive = isCallActive(this@BackgroundMonitorService)
                
                if (callActive) {
                    val hasPhoneStatePermission = ContextCompat.checkSelfPermission(this@BackgroundMonitorService, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                    val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                    val isCellularCallActive = if (hasPhoneStatePermission && telephonyManager != null) {
                        @Suppress("DEPRECATION")
                        telephonyManager.callState == TelephonyManager.CALL_STATE_OFFHOOK || 
                        telephonyManager.callState == TelephonyManager.CALL_STATE_RINGING
                    } else {
                        false
                    }

                    if (isCellularCallActive) {
                        tickCount++
                        if (isSimulatedAttackActive) {
                            riskPercentage = 95f
                            isProcessingAudioBytes = true
                        } else {
                            riskPercentage = 0f
                            isProcessingAudioBytes = false
                            tickCount = 0
                        }
                    } else if (tfliteInterpreter != null) {
                        try {
                            tickCount++
                            
                            // Run inference using the input data mapping [1, 128, 128, 3]
                            tfliteInterpreter?.run(inputData, outputData)
                            val realProbability = outputData[0][0]
                            
                            // Simulate threat logic:
                            // With a 500ms delay, we want the threat to trigger after 12 seconds (24 ticks).
                            // Let's make the threat active starting from tick 24 (12s).
                            val finalRealProb = if (tickCount >= 24) 0.08f else realProbability
                            
                            // Simulate active audio byte stream processing once the simulated threat is active
                            if (tickCount >= 24) {
                                isProcessingAudioBytes = true
                            }
                            
                            // Aggressive blocking layout bug fix check
                            riskPercentage = if (isCallAnalysisEngineActive && isProcessingAudioBytes) {
                                (1.0f - finalRealProb) * 100f
                            } else {
                                0f
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Inference execution error: ${e.message}", e)
                        }
                    } else {
                        Log.w(TAG, "TFLite interpreter not initialized.")
                    }

                    // Check foreground app
                    val foregroundApp = getForegroundPackageName(this@BackgroundMonitorService)
                    Log.i(TAG, "Call active. Ticks: $tickCount, Risk: $riskPercentage%, Foreground: $foregroundApp, Cellular: $isCellularCallActive")
                    
                    // Overlay trigger conditions
                    val isForegroundAppTarget = foregroundApp != null && targetFinancialApps.contains(foregroundApp)
                    
                    if (riskPercentage >= 80f && isForegroundAppTarget && !isThreatBypassed) {
                        Log.w(TAG, "CRITICAL THREAT DETECTED! Risk is $riskPercentage% and financial app $foregroundApp is in foreground. Displaying overlay.")
                        withContext(Dispatchers.Main) {
                            showOverlay()
                        }
                    }
                } else {
                    // Reset threat state when no call is active
                    if (tickCount > 0 || riskPercentage > 0f || isProcessingAudioBytes) {
                        Log.d(TAG, "No call active. Resetting call threat states to safe values.")
                        tickCount = 0
                        riskPercentage = 0f
                        isProcessingAudioBytes = false
                        isThreatBypassed = false // Reset bypass so they can test again on next call
                    }
                }
                
                delay(500) // 500ms periodic interval
            }
        }
    }


    private fun showOverlay() {
        if (overlayView != null) return // Already showing

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            val layoutParams = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                format = android.graphics.PixelFormat.TRANSLUCENT
            }

            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            overlayView = inflater.inflate(R.layout.layout_emergency_lock, null)

            // Bind overlay dismiss button
            overlayView?.findViewById<Button>(R.id.btnDismissLock)?.setOnClickListener {
                hideOverlay()
            }

            windowManager?.addView(overlayView, layoutParams)
            Log.d(TAG, "Emergency lockdown overlay added to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying window overlay: ${e.message}", e)
        }
    }

    private fun hideOverlay() {
        try {
            if (overlayView != null && windowManager != null) {
                windowManager?.removeView(overlayView)
                overlayView = null
                Log.d(TAG, "Emergency lockdown overlay removed from WindowManager")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing window overlay: ${e.message}", e)
        }
    }

    /**
     * Touch TensorFlow Lite and ONNX Mobile Runtime APIs to guarantee linking compiles correctly.
     */
    private fun initMLRuntimes() {
        try {
            // Touch TensorFlow Lite API
            val tfVersion = org.tensorflow.lite.TensorFlowLite.version()
            Log.i(TAG, "TFLite runtime loaded successfully. Version: $tfVersion")
            
            // Touch ONNX Runtime API
            val ortEnv = ai.onnxruntime.OrtEnvironment.getEnvironment()
            Log.i(TAG, "ONNX Runtime environment initialized successfully. Platform: Android")
        } catch (e: NoClassDefFoundError) {
            Log.e(TAG, "ML Runtime classes not found on classpath. Check your gradle dependencies: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing ML runtimes: ${e.message}", e)
        }
    }

    private fun stopServiceInternal() {
        Log.d(TAG, "Stopping service internally")
        isRunning = false
        hideOverlay() // Ensure overlay is removed on service destruction
        try {
            tfliteInterpreter?.close()
            tfliteInterpreter = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing interpreter: ${e.message}")
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")
        serviceScope.cancel() // cancel all coroutines
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // We don't bind to this service
    }

    companion object {
        private const val TAG = "AuraShieldService"
        private const val NOTIFICATION_ID = 1001
        
        const val ACTION_STOP_SERVICE = "com.aurashield.ai.action.STOP_SERVICE"
        const val ACTION_DISMISS_OVERLAY = "com.aurashield.ai.action.DISMISS_OVERLAY"
        const val ACTION_SIMULATE_CALL = "com.aurashield.ai.action.SIMULATE_CALL"
        const val EXTRA_CALL_ACTIVE = "com.aurashield.ai.extra.CALL_ACTIVE"
        
        var isSimulatedCallActive = false
        var isSimulatedAttackActive = false
    }
}
