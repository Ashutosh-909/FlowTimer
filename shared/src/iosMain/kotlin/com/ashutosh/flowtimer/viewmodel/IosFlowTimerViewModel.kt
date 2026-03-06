package com.ashutosh.flowtimer.viewmodel

import com.ashutosh.flowtimer.currentEpochMillis
import com.ashutosh.flowtimer.data.FlowTimerRepository
import com.ashutosh.flowtimer.data.IosFlowTimerRepository
import com.ashutosh.flowtimer.elapsedRealtimeMillis
import com.ashutosh.flowtimer.model.TimerUiState
import com.ashutosh.flowtimer.timer.TimerEngine
import com.ashutosh.flowtimer.timer.TimerState
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * iOS-side ViewModel. Owns the [TimerEngine] (no foreground service on iOS)
 * and persists state via [IosFlowTimerRepository] (NSUserDefaults).
 *
 * Expose state to SwiftUI via [observeUiState] / [observeShowPicker] with
 * callback-based subscription (no KMP-NativeCoroutines dependency required).
 */
class IosFlowTimerViewModel {

    private val repository: FlowTimerRepository = IosFlowTimerRepository()
    private val scope = MainScope()
    private val engine = TimerEngine(scope) { elapsedRealtimeMillis() }

    private val _showDurationPicker = MutableStateFlow(false)
    val showDurationPicker: StateFlow<Boolean> = _showDurationPicker.asStateFlow()

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    init {
        restorePersistedState()
        observeEngine()
        buildUiState()
    }

    // ── Restore ──────────────────────────────────────────────────────────

    private fun restorePersistedState() {
        scope.launch {
            val stateName = repository.timerState.first()
            val state = TimerState.fromName(stateName)
            if (state is TimerState.Running) {
                val persistedRemaining = repository.remainingMillis.first()
                val lastEpoch = repository.lastStartEpoch.first()
                val duration = repository.flowDurationMinutes.first()
                val elapsedSincePersist = if (lastEpoch > 0L) {
                    (currentEpochMillis() - lastEpoch).coerceAtLeast(0L)
                } else 0L
                val correctedRemaining = (persistedRemaining - elapsedSincePersist)
                    .coerceAtLeast(0L)
                if (correctedRemaining > 0L) {
                    engine.restore(
                        timerState = state,
                        remainingMs = correctedRemaining,
                        totalDurationMs = duration * 60_000L,
                        lastStartEpoch = lastEpoch
                    )
                    repository.setLastStartEpoch(currentEpochMillis())
                } else {
                    finishTimer()
                }
            } else if (state is TimerState.Finished) {
                // Reset stale finished state so user sees duration on relaunch
                repository.resetTimerState()
            }
        }
    }

    // ── Engine observation ────────────────────────────────────────────────

    private fun observeEngine() {
        scope.launch {
            combine(engine.state, engine.remainingMillis) { s, r -> s to r }
                .distinctUntilChanged()
                .collect { (state, remaining) ->
                    when (state) {
                        is TimerState.Running -> {
                            repository.setTimerState(state.name)
                            repository.setRemainingMillis(remaining)
                            repository.setLastStartEpoch(currentEpochMillis())
                        }
                        is TimerState.Finished -> finishTimer()
                        is TimerState.Idle -> { /* handled by reset */ }
                    }
                }
        }
    }

    private fun buildUiState() {
        scope.launch {
            combine(
                repository.timerState,
                repository.remainingMillis,
                repository.flowDurationMinutes,
                repository.completedSessionCount
            ) { stateName, remainingMs, durationMinutes, sessionCount ->
                val timerState = TimerState.fromName(stateName)
                val totalMs = durationMinutes * 60_000L
                val displayMillis = when (timerState) {
                    is TimerState.Idle -> totalMs
                    is TimerState.Running -> remainingMs
                    is TimerState.Finished -> 0L
                }
                val sandProgress = when (timerState) {
                    is TimerState.Idle -> 0f
                    is TimerState.Finished -> 1f
                    is TimerState.Running -> if (totalMs > 0) {
                        (1f - remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                }
                TimerUiState(
                    timerState = timerState,
                    displayMillis = displayMillis,
                    durationMinutes = durationMinutes,
                    sandProgress = sandProgress,
                    completedSessionCount = sessionCount
                )
            }.collect { _uiState.value = it }
        }
    }

    // ── User actions ──────────────────────────────────────────────────────

    fun onTapHourglass() {
        when (_uiState.value.timerState) {
            is TimerState.Idle -> {
                engine.start(_uiState.value.durationMinutes)
                scope.launch { repository.setLastStartEpoch(currentEpochMillis()) }
            }
            is TimerState.Running, is TimerState.Finished -> onLongPressReset()
        }
    }

    fun onLongPressReset() {
        engine.cancel()
        scope.launch {
            val duration = repository.flowDurationMinutes.first()
            engine.reset(duration)
            repository.resetTimerState()
        }
        _showDurationPicker.value = false
    }

    fun onTapSetDuration() {
        if (_uiState.value.timerState is TimerState.Idle) {
            _showDurationPicker.value = true
        }
    }

    fun onDismissDurationPicker() {
        _showDurationPicker.value = false
    }

    fun onConfirmDuration(minutes: Int) {
        scope.launch {
            repository.setFlowDurationMinutes(minutes)
            repository.setRemainingMillis(minutes * 60_000L)
        }
        _showDurationPicker.value = false
    }

    // ── Swift-compatible observation ──────────────────────────────────────

    fun observeUiState(callback: (TimerUiState) -> Unit): FlowCancellable {
        val job = scope.launch { _uiState.collect { callback(it) } }
        return FlowCancellable(job)
    }

    fun observeShowPicker(callback: (Boolean) -> Unit): FlowCancellable {
        val job = scope.launch { _showDurationPicker.collect { callback(it) } }
        return FlowCancellable(job)
    }

    fun destroy() = scope.cancel()

    // ── Private helpers ───────────────────────────────────────────────────

    private suspend fun finishTimer() {
        repository.setTimerState(TimerState.Finished.name)
        repository.setRemainingMillis(0L)
        repository.recordSessionCompleted()
    }
}
