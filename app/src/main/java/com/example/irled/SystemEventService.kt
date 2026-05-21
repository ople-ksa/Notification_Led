package com.example.irled

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.irled.preferences.SettingsManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Foreground service that monitors system-wide events such as battery changes,
 * power connection, and telephony state (calls).
 * It triggers specific IR LED patterns through [IrPulseManager] based on these events.
 */
class SystemEventService : Service() {

    // Coroutine scope for background tasks to avoid blocking the main thread.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var settingsManager: SettingsManager
    private lateinit var irPulseManager: IrPulseManager
    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: TelephonyCallback? = null

    /**
     * Receiver for battery and screen-related system broadcasts.
     */
    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            serviceScope.launch {
                when (intent?.action) {
                    Intent.ACTION_POWER_CONNECTED -> {
                        // Triggers a confirmation blink when the charger is plugged in.
                        if (settingsManager.chargingAlertsEnabled.first()) {
                            triggerDoubleBlink()
                        }
                    }
                    Intent.ACTION_BATTERY_CHANGED -> {
                        // Monitor battery percentage and charging status.
                        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                        val batteryPct = (level * 100 / scale.toFloat()).toInt()
                        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                        val isPlugged = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                                      status == BatteryManager.BATTERY_STATUS_FULL

                        // 1. Critical Charge State (<20% and not charging)
                        // This uses a unique pattern to alert the user to plug in.
                        if (batteryPct < 20 && !isPlugged) {
                            irPulseManager.setPattern(
                                IrPattern(longArrayOf(500L, 500L), -1, IrPriority.BATTERY_CRITICAL, "battery_critical")
                            )
                        } else {
                            irPulseManager.removePattern("battery_critical")
                        }

                        // 2. Full Charged State (100% and plugged)
                        // Alerts the user that charging is complete.
                        if (batteryPct == 100 && isPlugged) {
                            irPulseManager.setPattern(
                                IrPattern(longArrayOf(1000L, 500L), -1, IrPriority.BATTERY_FULL, "battery_full")
                            )
                        } else {
                            irPulseManager.removePattern("battery_full")
                        }
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        // User unlocked the device; clear pending notification blinks.
                        irPulseManager.clearNonBatteryPatterns()
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        // Screen turned on; assume the user is checking the device.
                        irPulseManager.clearNonBatteryPatterns()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(this)
        irPulseManager = IrPulseManager.getInstance(this)
        
        // Ensure the service stays alive by running in the foreground with a notification.
        startAsForeground()
        
        registerSystemReceiver()
        registerTelephonyListener()
        
        // Start a recurring task to ensure the NotificationListener stays bound.
        startPersistenceCheck()
    }

    /**
     * Periodically requests the system to rebind the NotificationListenerService.
     * This helps ensure that notifications are always caught even if the system kills the listener.
     */
    private fun startPersistenceCheck() {
        serviceScope.launch {
            while (isActive) {
                delay(60 * 1000L)
                // Only request rebind if the listener is not currently receiving events.
                // isNotificationListenerConnected() checks the system's binding state.
                val componentName = ComponentName(this@SystemEventService, IrNotificationListenerService::class.java)
                val isConnected = NotificationManagerCompat.getEnabledListenerPackages(this@SystemEventService)
                    .contains(packageName)
                if (!isConnected) {
                    try {
                        NotificationListenerService.requestRebind(componentName)
                    } catch (e: Exception) {
                        // Silently fail if rebind fails (e.g., service not enabled by user)
                    }
                }
            }
        }
    }

    /**
     * Configures and starts this service as a foreground service.
     * Android 8.0+ requires a Notification Channel.
     */
    private fun startAsForeground() {
        val channelId = "irled_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "IR LED Notification Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the IR LED notification service running in the background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("IR LED Service Active")
            .setContentText("Monitoring notifications and system events")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        // START_STICKY ensures the system restarts the service if it's killed due to memory pressure.
        return START_STICKY
    }

    /**
     * Registers the broadcast receiver for battery and power events.
     */
    private fun registerSystemReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(systemReceiver, filter)
    }

    /**
     * Sets up a listener for call state changes (IDLE, RINGING, OFFHOOK).
     */
    private fun registerTelephonyListener() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        telephonyManager = getSystemService(TELEPHONY_SERVICE) as? TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Modern API for Android 12+
            telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleCallState(state)
                }
            }
            telephonyCallback?.let {
                telephonyManager?.registerTelephonyCallback(mainExecutor, it)
            }
        } else {
            // Deprecated API for older versions
            @Suppress("DEPRECATION")
            telephonyManager?.listen(object : android.telephony.PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleCallState(state)
                }
            }, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    /**
     * Handles changes in the phone call state.
     */
    private fun handleCallState(state: Int) {
        serviceScope.launch {
            when (state) {
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    // Ongoing call: pulses the IR LED as a status indicator.
                    irPulseManager.setPattern(
                        IrPattern(longArrayOf(150L, 2500L), -1, IrPriority.CALL_ONGOING, "call_ongoing")
                    )
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    // Call ended: stop the ongoing call pattern.
                    irPulseManager.removePattern("call_ongoing")
                }
            }
        }
    }

    /**
     * Triggers a specific "double blink" pattern for transient events like power connection.
     */
    private fun triggerDoubleBlink() {
        serviceScope.launch {
            // Uses a finite repeatCount (2) to signal a successful action.
            irPulseManager.setPattern(
                IrPattern(longArrayOf(50L, 200L), 2, IrPriority.BATTERY_CHARGER_CONNECTED, "charger_connect")
            )
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        // Stop all IR patterns before shutting down.
        irPulseManager.clearAll()
        //Clean up resources to precent memory leaks.
        unregisterReceiver(systemReceiver)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let {
                telephonyManager?.unregisterTelephonyCallback(it)
            }
        } else {
            @Suppress("DEPRECATION")
            telephonyManager?.listen(null, android.telephony.PhoneStateListener.LISTEN_NONE)
        }
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1
    }
}
