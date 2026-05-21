package com.example.irled

import android.content.Context
import android.hardware.ConsumerIrManager
import androidx.core.app.NotificationManagerCompat

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Handles direct interaction with the device's Infrared (IR) hardware.
 */
class HardwareManager(private val context: Context) {
    // Mutex to ensure only one IR transmission happens at a time across all services.
    private val mutex = Mutex()

    // Lazy initialization of the ConsumerIrManager.
    private val irManager: ConsumerIrManager? by lazy {
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    }

    /**
     * Checks if the device has an IR emitter.
     */
    fun hasIrEmitter(): Boolean {
        return irManager?.hasIrEmitter() ?: false
    }

    /**
     * Checks if the app has been granted notification listener access by the user.
     */
    fun isNotificationAccessGranted(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    /**
     * Returns a string representation of supported IR carrier frequencies.
     */
    fun getIrFrequencies(): String {
        val frequencies = irManager?.carrierFrequencies ?: return "None"
        return frequencies.joinToString { "${it.minFrequency}-${it.maxFrequency} Hz" }
    }

    /**
     * Transmits a single IR pulse of a specific duration.
     * @param durationUs Duration of the pulse in microseconds.
     * @param isHighBrightness If true, uses full power. Otherwise, limited to 5% brightness via PWM.
     */
    suspend fun transmitPulse(durationUs: Int, isHighBrightness: Boolean) {
        if (isHighBrightness) {
            transmitWithCarrier(intArrayOf(durationUs, 50, 10))
        } else {
            val pwmFrequency = 250
            val pwmPeriodUs = 1_000_000 / pwmFrequency
            val numCycles = (durationUs / pwmPeriodUs).coerceAtLeast(1)
            val pattern = IntArray(numCycles * 2 + 1)
            for (i in 0 until numCycles) {
                val onUs = (0.05f * pwmPeriodUs).toInt().coerceIn(1, pwmPeriodUs - 1)
                val offUs = pwmPeriodUs - onUs
                pattern[i * 2] = onUs
                pattern[i * 2 + 1] = offUs
            }
            pattern[pattern.size - 1] = 10
            transmitWithCarrier(pattern)
        }
    }

    /**
     * Transmits an IR pulse that fades in and out using PWM.
     * @param durationUs Total duration of the gradient effect in microseconds.
     * @param isHighBrightness If true, peak brightness is 100%. Otherwise, peak is 5%.
     */
    suspend fun transmitGradient(durationUs: Int, isHighBrightness: Boolean) {
        val pwmFrequency = 250 // Hz, high enough to avoid flickering
        val pwmPeriodUs = 1_000_000 / pwmFrequency
        val numCycles = (durationUs / pwmPeriodUs).coerceAtLeast(1)

        val pattern = IntArray(numCycles * 2 + 1)
        for (i in 0 until numCycles) {
            //For loop begins
            val progress = i.toFloat() / (numCycles - 1).coerceAtLeast(1)

            // Triangle wave: 0% -> 100% -> 0%
            val fullTriangle = 1.0f - Math.abs(2.0f * progress - 1.0f)

            //Expo-curve
            val slowTriangle = Math.pow(fullTriangle.toDouble(), 2.0).toFloat()
            val maxDuty = if (isHighBrightness) 1.0f else 0.05f
            val dutyCycle = slowTriangle * maxDuty
            val onUs = (dutyCycle * pwmPeriodUs).toInt().coerceIn(1, pwmPeriodUs - 1)
            val offUs = pwmPeriodUs - onUs

            pattern[i * 2] = onUs
            pattern[i * 2 + 1] = offUs
        }
        pattern[pattern.size - 1] = 10 // Ensure odd-length for compatibility

        transmitWithCarrier(pattern)
    }

    private suspend fun transmitWithCarrier(pattern: IntArray) {
        val frequencies = irManager?.carrierFrequencies
        if (frequencies.isNullOrEmpty()) return

        var frequency = 38000
        val is38kSupported = frequencies.any { frequency in it.minFrequency..it.maxFrequency }
        if (!is38kSupported) {
            frequency = frequencies[0].minFrequency
        }

        mutex.withLock {
            try {
                irManager?.transmit(frequency, pattern)
            } catch (e: Exception) {
                // Silently handle errors
            }
        }
    }
}
