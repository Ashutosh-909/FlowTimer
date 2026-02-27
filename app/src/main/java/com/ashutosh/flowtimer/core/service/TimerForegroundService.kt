package com.ashutosh.flowtimer.core.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.SystemClock
import com.ashutosh.flowtimer.core.data.PreferencesRepository
import com.ashutosh.flowtimer.core.timer.TimerEngine
import com.ashutosh.flowtimer.core.timer.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the timer alive through screen-off and
 * backgrounding. This is the **single source of truth** while a flow session
 * is active.
 *
 * Communicates with the rest of the app via:
 * - Incoming [Intent] actions: [ACTION_START], [ACTION_PAUSE], [ACTION_RESUME],
 *   [ACTION_RESET].
 * - Outgoing state via [PreferencesRepository] (DataStore), which the widget
 *   and ViewModel observe.
 *
 * Uses [START_STICKY] so the system restarts the service after a kill; on
 * restart the engine restores state from DataStore.
 */
class TimerForegroundService : Service() {

    companion object {
        const val ACTION_START = "com.ashutosh.flowtimer.ACTION_START"
        const val ACTION_PAUSE = "com.ashutosh.flowtimer.ACTION_PAUSE"
        const val ACTION_RESUME = "com.ashutosh.flowtimer.ACTION_RESUME"
        const val ACTION_RESET = "com.ashutosh.flowtimer.ACTION_RESET"

        /** Optional extra: flow duration in minutes (used with [ACTION_START]). */
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"

        /** Convenience factory for launching the service with an action. */
        fun intent(context: Context, action: String): Intent =
            Intent(context, TimerForegroundService::class.java).apply {
                this.action = action
            }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var timerEngine: TimerEngine
    private lateinit var repository: PreferencesRepository

    // ── Lifecycle ────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()

        NotificationHelper.createChannels(this)

        repository = PreferencesRepository(applicationContext)
        timerEngine = TimerEngine(serviceScope, elapsedRealtimeProvider = SystemClock::elapsedRealtime)

        observeEngineState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(
                intent.getIntExtra(EXTRA_DURATION_MINUTES, -1)
            )
            ACTION_PAUSE -> handlePause()
            ACTION_RESUME -> handleResume()
            ACTION_RESET -> handleReset()
            null -> handleRestart() // System restart (START_STICKY)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        timerEngine.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ── Intent handlers ──────────────────────────────────────────────────

    /**
     * Start a new flow session.
     *
     * **Critical:** [startForeground] must be called synchronously (before
     * any suspension point) because the caller used [startForegroundService].
     * Failing to do so within the system timeout (~5 s on API 34+) triggers
     * `ForegroundServiceDidNotStartInTimeException`.
     */
    private fun handleStart(durationMinutesExtra: Int) {
        // 1. Determine duration synchronously — use the passed extra or
        //    fall back to the compile-time default. We correct later if the
        //    persisted value differs.
        val durationMinutes = if (durationMinutesExtra > 0) {
            durationMinutesExtra
        } else {
            PreferencesRepository.DEFAULT_FLOW_DURATION_MINUTES
        }

        // 2. Start engine & go foreground immediately — no suspension.
        timerEngine.start(durationMinutes)
        startForegroundWithNotification(
            timerEngine.remainingMillis.value,
            isPaused = false
        )

        // 3. Persist asynchronously (safe — foreground is already active).
        serviceScope.launch {
            repository.setLastStartEpoch(System.currentTimeMillis())

            // If we used the compile-time default, check the actual persisted
            // preference and correct if it differs.
            if (durationMinutesExtra <= 0) {
                val persisted = repository.flowDurationMinutes.first()
                if (persisted != durationMinutes) {
                    timerEngine.cancel()
                    timerEngine.start(persisted)
                    updateNotification(timerEngine.remainingMillis.value, isPaused = false)
                }
            }
        }
    }

    private fun handlePause() {
        timerEngine.pause()
        val remaining = timerEngine.remainingMillis.value
        updateNotification(remaining, isPaused = true)
        serviceScope.launch {
            repository.setTimerState(TimerState.Paused.name)
            repository.setRemainingMillis(remaining)
        }
    }

    private fun handleResume() {
        timerEngine.resume()
        val remaining = timerEngine.remainingMillis.value
        updateNotification(remaining, isPaused = false)

        serviceScope.launch {
            repository.setTimerState(TimerState.Running.name)
            repository.setRemainingMillis(remaining)
            repository.setLastStartEpoch(System.currentTimeMillis())
        }
    }

    private fun handleReset() {
        timerEngine.cancel()

        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(NotificationHelper.COMPLETION_NOTIFICATION_ID)

        stopForeground(STOP_FOREGROUND_REMOVE)

        // Persist reset THEN stop. Calling stopSelf() before the coroutine
        // completes would cancel serviceScope in onDestroy(), losing the
        // DataStore write and leaving the app stuck in stale state.
        serviceScope.launch {
            val duration = repository.flowDurationMinutes.first()
            timerEngine.reset(duration)
            repository.resetTimerState()
            stopSelf()
        }
    }

    /**
     * Called when the system restarts the service after a process kill
     * (because of [START_STICKY]). Restores timer state from DataStore.
     *
     * For Running timers, adjusts [remainingMs] by the wall-clock time
     * that elapsed between the last persist and now (drift correction).
     */
    private fun handleRestart() {
        serviceScope.launch {
            val stateName = repository.timerState.first()
            val state = TimerState.fromName(stateName)
            val persistedRemainingMs = repository.remainingMillis.first()
            val durationMinutes = repository.flowDurationMinutes.first()
            val lastStartEpoch = repository.lastStartEpoch.first()
            val totalDurationMs = durationMinutes * 60_000L

            when (state) {
                is TimerState.Running -> {
                    // Compensate for wall-clock time elapsed since last persist
                    val elapsedSincePersist = if (lastStartEpoch > 0L) {
                        (System.currentTimeMillis() - lastStartEpoch).coerceAtLeast(0L)
                    } else {
                        0L
                    }
                    val correctedRemaining = (persistedRemainingMs - elapsedSincePersist)
                        .coerceAtLeast(0L)

                    if (correctedRemaining <= 0L) {
                        // Timer would have finished while we were dead
                        onTimerFinished()
                        return@launch
                    }

                    timerEngine.restore(
                        timerState = state,
                        remainingMs = correctedRemaining,
                        totalDurationMs = totalDurationMs,
                        lastStartEpoch = lastStartEpoch
                    )
                    // Update the epoch to now for future drift corrections
                    repository.setLastStartEpoch(System.currentTimeMillis())

                    startForegroundWithNotification(
                        correctedRemaining,
                        isPaused = false
                    )
                }
                is TimerState.Paused -> {
                    // Paused timers don't drift — restore as-is
                    timerEngine.restore(
                        timerState = state,
                        remainingMs = persistedRemainingMs,
                        totalDurationMs = totalDurationMs,
                        lastStartEpoch = lastStartEpoch
                    )
                    startForegroundWithNotification(
                        persistedRemainingMs,
                        isPaused = true
                    )
                }
                is TimerState.Idle, is TimerState.Finished -> {
                    // Nothing to restore — stop the service
                    stopSelf()
                }
            }
        }
    }

    // ── Observation & persistence ────────────────────────────────────────

    /**
     * Observe [TimerEngine] state & remaining millis and:
     * 1. Update the ongoing notification every second.
     * 2. Persist state to DataStore so the widget can read it.
     * 3. Show the completion notification and stop when finished.
     */
    private fun observeEngineState() {
        serviceScope.launch {
            combine(
                timerEngine.state,
                timerEngine.remainingMillis
            ) { state, remaining -> state to remaining }
                .distinctUntilChanged()
                .collect { (state, remaining) ->
                    when (state) {
                        is TimerState.Running -> {
                            updateNotification(remaining, isPaused = false)
                            persistState(TimerState.Running, remaining)
                        }
                        is TimerState.Paused -> {
                            updateNotification(remaining, isPaused = true)
                            persistState(TimerState.Paused, remaining)
                        }
                        is TimerState.Finished -> {
                            onTimerFinished()
                        }
                        is TimerState.Idle -> {
                            // No-op; handled by handleReset
                        }
                    }
                }
        }
    }

    private fun persistState(state: TimerState, remainingMs: Long) {
        serviceScope.launch {
            repository.setTimerState(state.name)
            repository.setRemainingMillis(remainingMs)
            // Keep epoch fresh for Running state so drift correction after
            // process death uses the most recent anchor.
            if (state is TimerState.Running) {
                repository.setLastStartEpoch(System.currentTimeMillis())
            }
        }
    }

    private fun onTimerFinished() {
        // Show completion notification
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(
            NotificationHelper.COMPLETION_NOTIFICATION_ID,
            NotificationHelper.buildCompletionNotification(this)
        )

        // Remove the ongoing timer notification and stop foreground
        stopForeground(STOP_FOREGROUND_REMOVE)

        // Persist THEN stop — ensures DataStore write is not cancelled.
        serviceScope.launch {
            repository.setTimerState(TimerState.Finished.name)
            repository.setRemainingMillis(0L)
            stopSelf()
        }
    }

    // ── Notification helpers ─────────────────────────────────────────────

    private fun startForegroundWithNotification(remainingMs: Long, isPaused: Boolean) {
        val notification = NotificationHelper.buildTimerNotification(
            this,
            remainingMs,
            isPaused
        )
        startForeground(
            NotificationHelper.TIMER_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
    }

    private fun updateNotification(remainingMs: Long, isPaused: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(
            NotificationHelper.TIMER_NOTIFICATION_ID,
            NotificationHelper.buildTimerNotification(this, remainingMs, isPaused)
        )
    }
}
