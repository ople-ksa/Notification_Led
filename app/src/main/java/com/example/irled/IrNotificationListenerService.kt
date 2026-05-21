package com.example.irled

import android.app.KeyguardManager
import android.os.PowerManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.irled.data.AppDatabase
import com.example.irled.data.AppCategory
import kotlinx.coroutines.*

/**
 * Service that listens for status bar notifications and triggers IR LED pulses.
 * It maps specific notification types (like missed calls or messages) to distinct IR patterns.
 */
class IrNotificationListenerService : NotificationListenerService() {

    // Scope for background processing of notifications and database lookups.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var irPulseManager: IrPulseManager
    private lateinit var db: AppDatabase
    private lateinit var keyguardManager: KeyguardManager
    private lateinit var powerManager: PowerManager

    /**
     * Receiver for screen-off events. 
     * When the screen turns off, we wait a moment and then re-trigger pulses for all 
     * currently active notifications that the user hasn't dismissed yet.
     */
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                serviceScope.launch {
                    // Small delay to ensure the system has settled into the screen-off state.
                    delay(500)
                    // Only trigger if the screen is still off.
                    if (!powerManager.isInteractive) {
                        retriggerActiveNotifications()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        irPulseManager = IrPulseManager.getInstance(this)
        db = AppDatabase.getDatabase(this)
        keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        powerManager = getSystemService(POWER_SERVICE) as PowerManager

        // Listen for screen-off events to resume pulsing when the user stops using the phone.
        registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        serviceScope.cancel()
    }

    /**
     * Called by the system when a new notification is posted.
     */
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val packageName = sbn?.packageName ?: return

        // Don't trigger for our own service notification.
        if (packageName == this.packageName) return

        // To save battery and prevent distraction, don't pulse if the screen is already on.
        if (powerManager.isInteractive) {
            return
        }

        processNotification(sbn)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Sticky service ensures the listener is restarted if the system kills it.
        return START_STICKY
    }

    /**
     * Called when the service is connected and ready to receive notification events.
     */
    override fun onListenerConnected() {
        super.onListenerConnected()
        // Immediately check for any existing notifications that should be pulsing.
        retriggerActiveNotifications()
    }

    /**
     * Analyzes a notification and decides which IR pattern to trigger.
     */
    private fun processNotification(sbn: StatusBarNotification) {
        serviceScope.launch {
            try {
                val packageName = sbn.packageName
                // Check user settings in the database to see if this app is enabled for IR alerts.
                val setting = db.appSettingDao().getSetting(packageName)
                if (setting != null && setting.isEnabled) {
                    //un used code sentence -> keyguardManager.isKeyguardLocked

                    // Notifications loop indefinitely (-1) if they are active while the device is locked.
                    // The Smart Frequency and 5-minute timeout in IrPulseManager will handle power management.
                    val shouldLoop = true
                    val repeatCount = if (shouldLoop) -1 else setting.repeatCount

                    // Extract notification content to identify specific event types (like missed calls).
                    val title = sbn.notification.extras.getString(android.app.Notification.EXTRA_TITLE) ?: ""
                    val text = sbn.notification.extras.getString(android.app.Notification.EXTRA_TEXT) ?: ""
                    val isMissedCall = title.contains("missed", ignoreCase = true) ||
                            text.contains("missed", ignoreCase = true) ||
                            packageName.contains("telecom") || packageName.contains("dialer")

                    // Select the IR pattern and priority based on the notification type.
                    val (pattern, priority) = when {
                        isMissedCall -> {
                            val missedCount = getMissedCallCount()
                            if (missedCount >= 2) {
                                // Multi-pulse for multiple missed calls (Higher visibility).
                                longArrayOf(100L, 200L, 100L, 2500L) to IrPriority.CALL_MISSED
                            } else {
                                // Standard blink for a single missed call.
                                longArrayOf(250L, 2500L) to IrPriority.CALL_MISSED
                            }
                        }
                        setting.isMessageApp -> {
                            // Distinct blink durations for different categories of messaging apps.
                            when (setting.category) {
                                AppCategory.CELLULAR -> longArrayOf(100L, 2500L) to IrPriority.MSG_CELLULAR
                                AppCategory.EMAIL -> longArrayOf(400L, 2500L) to IrPriority.MSG_EMAIL
                                AppCategory.SOCIAL -> longArrayOf(700L, 2500L) to IrPriority.MSG_SOCIAL
                                else -> longArrayOf(700L, 2500L) to IrPriority.MSG_SOCIAL
                            }
                        }
                        else -> {
                            // Use user-defined custom durations for general apps.
                            longArrayOf(setting.onDurationMs, setting.offDurationMs) to IrPriority.GENERAL_NOTIFICATION
                        }
                    }

                    // Tag includes notification ID so each notification is tracked individually.
                    // Multiple notifications from the same app won't cancel each other.
                    val tag = "${packageName}:${sbn.id}"
                    irPulseManager.setPattern(
                        IrPattern(
                            pattern = pattern,
                            repeatCount = repeatCount,
                            priority = priority,
                            tag = tag,
                            isGradient = setting.isGradient,
                            isHighBrightness = setting.isHighBrightness
                        )
                    )
                }
            } catch (e: Exception) {
                // Ignore errors during setting lookups to keep the service stable.
            }
        }
    }

    /**
     * Iterates through all currently visible notifications and triggers IR patterns for them.
     * Only runs when the screen is off - same guard as onNotificationPosted.
     */
    private fun retriggerActiveNotifications() {
        if (powerManager.isInteractive) return
        activeNotifications?.forEach { sbn ->
            if (sbn.packageName != this.packageName) {
                processNotification(sbn)
            }
        }
    }

    /**
     * Helper to count how many "missed call" notifications are currently present.
     */
    private fun getMissedCallCount(): Int {
        val active = activeNotifications ?: return 0
        return active.count { sbn ->
            val title = sbn.notification.extras.getString(android.app.Notification.EXTRA_TITLE) ?: ""
            val text = sbn.notification.extras.getString(android.app.Notification.EXTRA_TEXT) ?: ""
            title.contains("missed", ignoreCase = true) ||
                    text.contains("missed", ignoreCase = true) ||
                    sbn.packageName.contains("telecom") ||
                    sbn.packageName.contains("dialer")
        }
    }

    /**
     * Called by the system when the user dismisses or clicks a notification.
     */
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        val packageName = sbn?.packageName ?: return
        val tag = "${packageName}:${sbn.id}"
        // Remove only this specific notification's pattern, not all patterns for the app.
        irPulseManager.removePattern(tag)
    }
}