package com.example.irled

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

enum class IrPriority(val level: Int) {
    BATTERY_CRITICAL(1),
    BATTERY_CHARGER_CONNECTED(2),
    BATTERY_FULL(3),
    CALL_ONGOING(4),
    CALL_MISSED(5),
    MSG_CELLULAR(6),
    MSG_EMAIL(7),
    MSG_SOCIAL(8),
    GENERAL_NOTIFICATION(9),
}

data class IrPattern(
    val pattern: LongArray,         // alternating [onMs, offMs, onMs, offMs, ...]
    val repeatCount: Int,           // -1 = indefinite
    val priority: IrPriority,
    val tag: String,
    val isGradient: Boolean = false,
    val isHighBrightness: Boolean = false,
    val creationTime: Long = SystemClock.elapsedRealtime()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IrPattern) return false
        if (!pattern.contentEquals(other.pattern)) return false
        if (repeatCount != other.repeatCount) return false
        if (priority != other.priority) return false
        if (tag != other.tag) return false
        if (isGradient != other.isGradient) return false
        if (isHighBrightness != other.isHighBrightness) return false
        return true
    }
    override fun hashCode(): Int {
        var result = pattern.contentHashCode()
        result = 31 * result + repeatCount
        result = 31 * result + priority.hashCode()
        result = 31 * result + tag.hashCode()
        result = 31 * result + isGradient.hashCode()
        result = 31 * result + isHighBrightness.hashCode()
        return result
    }
}

class IrPulseManager private constructor(private val context: Context) {

    private val hardwareManager = HardwareManager(context)
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    // Single wakelock held for the entire duration of any active pulsing.
    // Acquired when the first pattern starts, released when all patterns are done.
    // TAG format required by Android: "package:reason"
    private val wakeLock = powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        "com.example.irled:PulseWakeLock"
    ).apply { setReferenceCounted(false) } // non-reference-counted: one release always releases

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val activePatterns = ConcurrentHashMap<String, IrPattern>()

    @Volatile private var runningTag: String? = null
    private var pulseJob: Job? = null

    // ─── Public API ───────────────────────────────────────────────────────────

    fun setPattern(pattern: IrPattern) {
        val existing = activePatterns[pattern.tag]

        // Identical pattern already running for this tag — no-op.
        // Critical: prevents ACTION_BATTERY_CHANGED from restarting on every % tick.
        if (existing != null &&
            existing.priority == pattern.priority &&
            existing.pattern.contentEquals(pattern.pattern) &&
            runningTag == pattern.tag &&
            pulseJob?.isActive == true
        ) return

        activePatterns[pattern.tag] = pattern
        updatePulse()
    }

    fun removePattern(tag: String) {
        activePatterns.remove(tag)
        updatePulse()
    }

    fun clearAll() {
        activePatterns.clear()
        updatePulse()
    }

    fun clearNonBatteryPatterns() {
        val iterator = activePatterns.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val p = entry.value.priority
            if (p != IrPriority.BATTERY_CRITICAL &&
                p != IrPriority.BATTERY_CHARGER_CONNECTED &&
                p != IrPriority.BATTERY_FULL
            ) iterator.remove()
        }
        val newTop = activePatterns.values.minByOrNull { it.priority.level }
        if (newTop?.tag != runningTag || pulseJob?.isActive != true) {
            updatePulse()
        }
    }

    // ─── Core ─────────────────────────────────────────────────────────────────

    private fun updatePulse() {
        scope.launch {
            mutex.withLock {
                val topPattern = activePatterns.values.minByOrNull { it.priority.level }

                // Guard: same pattern already running — don't restart it.
                if (topPattern?.tag == runningTag && pulseJob?.isActive == true) return@withLock

                // Cancel the current job before switching patterns.
                pulseJob?.cancelAndJoin()
                runningTag = topPattern?.tag

                if (topPattern == null) {
                    // Nothing left to pulse — release the wakelock.
                    if (wakeLock.isHeld) wakeLock.release()
                    return@withLock
                }

                // Acquire wakelock before launching the loop.
                // Non-battery patterns: 5 minutes + 10s buffer.
                // Battery patterns: indefinite (released when cleared).
                val isBattery = topPattern.priority.level <= IrPriority.BATTERY_FULL.level
                if (!wakeLock.isHeld) {
                    if (isBattery) {
                        wakeLock.acquire() // held until clearAll / removePattern
                    } else {
                        wakeLock.acquire(610_000L) // 5 min + 10s safety buffer
                    }
                }

                pulseJob = launch { runPulseLoop(topPattern) }
            }
        }
    }

    private suspend fun runPulseLoop(topPattern: IrPattern) {
        val isBatteryPattern = topPattern.priority.level <= IrPriority.BATTERY_FULL.level
        val isIndefinite     = topPattern.repeatCount == -1
        val creationTime     = topPattern.creationTime
        val timeoutMs        = 600_000L  // 5-minute hard stop for non-battery patterns

        var count = 0

        while (coroutineContext.isActive && (isIndefinite || count < topPattern.repeatCount)) {

            // Non-battery patterns: hard stop after 5 minutes.
            if (isIndefinite && !isBatteryPattern &&
                SystemClock.elapsedRealtime() - creationTime >= timeoutMs) break

            // Emit every ON/OFF step in the pattern array precisely.
            var i = 0
            while (i < topPattern.pattern.size && coroutineContext.isActive) {
                val durationMs = topPattern.pattern[i]
                val isOnPhase  = (i % 2 == 0)

                if (isOnPhase) {
                    // ON — transmit IR pulse then hold the phase duration.
                    // transmitPulse() is fire-and-forget so delay() drives the timing.
                    val durationUs = (durationMs * 1000L).toInt()
                    if (topPattern.isGradient) {
                        hardwareManager.transmitGradient(durationUs, topPattern.isHighBrightness)
                    } else {
                        hardwareManager.transmitPulse(durationUs, topPattern.isHighBrightness)
                    }
                    delay(durationMs)
                } else {
                    // OFF — CPU stays alive (wakelock held), just sleep precisely.
                    delay(durationMs)
                }
                i++
            }

            count++
        }

        // Pattern finished naturally — clean up and promote next pattern if any.
        val timedOut = isIndefinite && !isBatteryPattern &&
                SystemClock.elapsedRealtime() - creationTime >= timeoutMs
        if (timedOut || (!isIndefinite && count >= topPattern.repeatCount)) {
            activePatterns.remove(topPattern.tag)
            runningTag = null
            scope.launch { updatePulse() }
        }
    }

    // ─── Singleton ────────────────────────────────────────────────────────────

    companion object {
        @Volatile private var INSTANCE: IrPulseManager? = null

        fun getInstance(context: Context): IrPulseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: IrPulseManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}