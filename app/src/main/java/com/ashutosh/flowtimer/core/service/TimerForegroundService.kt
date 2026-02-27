package com.ashutosh.flowtimer.core.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
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
        timerEngine = TimerEngine(serviceScope)

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

    private fun handleStart(durationMinutesExtra: Int) {
        serviceScope.launch {
            val durationMinutes = if (durationMinutesExtra > 0) {
                durationMinutesExtra
            } else {
                repository.flowDurationMinutes.first()
            }

            timerEngine.start(durationMinutes)

            // Persist the start epoch for drift correction / widget cold read
            repository.setLastStartEpoch(System.currentTimeMillis())

            startForegroundWithNotification(
                timerEngine.remainingMillis.value,
                isPaused = false
            )
        }
    }

    private fun handlePause() {
        timerEngine.pause()
        updateNotification(timerEngine.remainingMillis.value, isPaused = true)
        persistState(TimerState.Paused, timerEngine.remainingMillis.value)
    }

    private fun handleResume() {
        timerEngine.resume()
        updateNotification(timerEngine.remainingMillis.value, isPaused = false)

        serviceScope.launch {
            repository.setLastStartEpoch(System.currentTimeMillis())
        }
    }

    private fun handleReset() {
        timerEngine.cancel()

        serviceScope.launch {
            val duration = repository.flowDurationMinutes.first()
            timerEngine.reset(duration)
            repository.resetTimerState()
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(NotificationHelper.COMPLETION_NOTIFICATION_ID)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Called when the system restarts the service after a process kill
     * (because of [START_STICKY]). Restores timer state from DataStore.
     */
    private fun handleRestart() {
        serviceScope.launch {
            val stateName = repository.timerState.first()
            val state = TimerState.fromName(stateName)
            val remainingMs = repository.remainingMillis.first()
            val durationMinutes = repository.flowDurationMinutes.first()
            val lastStartEpoch = repository.lastStartEpoch.first()
            val totalDurationMs = durationMinutes * 60_000L

            when (state) {
                is TimerState.Running, is TimerState.Paused -> {
                    timerEngine.restore(
                        timerState = state,
                        remainingMs = remainingMs,
                        totalDurationMs = totalDurationMs,
                        lastStartEpoch = lastStartEpoch
                    )
                    startForegroundWithNotification(
                        remainingMs,
                        isPaused = state is TimerState.Paused
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
        }
    }

    private fun onTimerFinished() {
        serviceScope.launch {
            repository.setTimerState(TimerState.Finished.name)
            repository.setRemainingMillis(0L)
        }

        // Show completion notification
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(
            NotificationHelper.COMPLETION_NOTIFICATION_ID,
            NotificationHelper.buildCompletionNotification(this)
        )

        // Remove the ongoing timer notification and stop foreground
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
