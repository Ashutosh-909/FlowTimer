package com.ashutosh.flowtimer.core.timer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Coroutine-based countdown timer emitting [TimerState] and remaining millis.
 *
 * Uses wall-clock anchoring via [elapsedRealtimeProvider] for drift-proof timing.
 * The timer ticks every ~1 s and recalculates remaining time from the anchor,
 * not from cumulative delay.
 *
 * There is no pause/resume — stopping a running timer resets to Idle.
 *
 * @param scope Coroutine scope for the tick loop (e.g., service scope).
 * @param elapsedRealtimeProvider Returns monotonic clock millis (e.g.,
 *   `SystemClock.elapsedRealtime()`). Must be provided by the caller so that
 *   `core/timer` stays free of Android framework imports.
 */
class TimerEngine(
    private val scope: CoroutineScope,
    private val elapsedRealtimeProvider: () -> Long
) {
    private val _state = MutableStateFlow<TimerState>(TimerState.Idle)
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val _remainingMillis = MutableStateFlow(0L)
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

    /** Total duration for the current session in millis. */
    private var totalDurationMillis = 0L

    /** Elapsed realtime anchor when the timer was last started. */
    private var anchorMillis = 0L

    private var tickJob: Job? = null

    /**
     * Start a new countdown for [durationMinutes] minutes.
     * If already running, this is a no-op.
     */
    fun start(durationMinutes: Int) {
        if (_state.value is TimerState.Running) return

        tickJob?.cancel()
        tickJob = null

        totalDurationMillis = durationMinutes * 60_000L
        anchorMillis = elapsedRealtimeProvider()
        _remainingMillis.value = totalDurationMillis
        _state.value = TimerState.Running
        startTickLoop()
    }

    /**
     * Reset the timer to idle with the given [durationMinutes].
     * Can be called from any state.
     */
    fun reset(durationMinutes: Int) {
        tickJob?.cancel()
        tickJob = null

        totalDurationMillis = durationMinutes * 60_000L
        anchorMillis = 0L
        _remainingMillis.value = totalDurationMillis
        _state.value = TimerState.Idle
    }

    /** Cancel the tick loop without changing state. Used for cleanup. */
    fun cancel() {
        tickJob?.cancel()
        tickJob = null
    }

    /**
     * Restore the engine to a specific state (e.g., after process death).
     * Used by the foreground service to recover from persisted DataStore values.
     */
    fun restore(
        timerState: TimerState,
        remainingMs: Long,
        totalDurationMs: Long,
        lastStartEpoch: Long
    ) {
        totalDurationMillis = totalDurationMs
        _remainingMillis.value = remainingMs

        when (timerState) {
            is TimerState.Running -> {
                // Set anchor back by the already-elapsed portion so the tick
                // loop's (totalDurationMillis - (now - anchorMillis)) == remainingMs.
                val alreadyElapsed = totalDurationMs - remainingMs
                anchorMillis = elapsedRealtimeProvider() - alreadyElapsed
                _state.value = TimerState.Running
                startTickLoop()
            }
            is TimerState.Finished -> {
                _state.value = TimerState.Finished
            }
            is TimerState.Idle -> {
                _state.value = TimerState.Idle
            }
        }
    }

    private fun startTickLoop() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                val now = elapsedRealtimeProvider()
                val totalElapsed = now - anchorMillis
                val remaining = (totalDurationMillis - totalElapsed).coerceAtLeast(0L)
                _remainingMillis.value = remaining

                if (remaining <= 0L) {
                    _state.value = TimerState.Finished
                    break
                }

                delay(1000L)
            }
        }
    }
}
