package com.ashutosh.flowtimer.ui.home

import android.app.ActivityManager
import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ashutosh.flowtimer.core.service.TimerForegroundService
import com.ashutosh.flowtimer.data.AndroidFlowTimerRepository
import com.ashutosh.flowtimer.data.FlowTimerRepository
import com.ashutosh.flowtimer.model.TimerUiState
import com.ashutosh.flowtimer.timer.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the home screen. Exposes a single [TimerUiState] derived from
 * [AndroidFlowTimerRepository] (DataStore). The foreground service is the source
 * of truth while the timer is active — the ViewModel reads persisted state
 * only, and sends control intents to the service.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AndroidFlowTimerRepository(application)

    private val _showDurationPicker = MutableStateFlow(false)
    val showDurationPicker: StateFlow<Boolean> = _showDurationPicker.asStateFlow()

    init {
        viewModelScope.launch {
            val stateName = repository.timerState.first()
            val state = TimerState.fromName(stateName)
            when (state) {
                is TimerState.Running -> {
                    if (!isTimerServiceRunning()) repository.resetTimerState()
                }
                is TimerState.Finished -> repository.resetTimerState()
                is TimerState.Idle -> { /* nothing to recover */ }
            }
        }
    }

    val uiState: StateFlow<TimerUiState> = combine(
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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TimerUiState()
    )

    // ── User actions ──────────────────────────────────────────────────────

    fun onTapHourglass() {
        val context = getApplication<Application>()
        when (uiState.value.timerState) {
            is TimerState.Idle -> {
                val intent = TimerForegroundService
                    .intent(context, TimerForegroundService.ACTION_START)
                    .putExtra(
                        TimerForegroundService.EXTRA_DURATION_MINUTES,
                        uiState.value.durationMinutes
                    )
                context.startForegroundService(intent)
            }
            is TimerState.Running, is TimerState.Finished -> onLongPressReset()
        }
    }

    fun onLongPressReset() {
        val context = getApplication<Application>()
        context.startService(
            TimerForegroundService.intent(context, TimerForegroundService.ACTION_RESET)
        )
    }

    fun onTapSetDuration() {
        if (uiState.value.timerState is TimerState.Idle) _showDurationPicker.value = true
    }

    fun onDismissDurationPicker() {
        _showDurationPicker.value = false
    }

    fun onConfirmDuration(minutes: Int) {
        viewModelScope.launch {
            repository.setFlowDurationMinutes(minutes)
            repository.setRemainingMillis(minutes * 60_000L)
        }
        _showDurationPicker.value = false
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun isTimerServiceRunning(): Boolean {
        val manager = getApplication<Application>()
            .getSystemService(ActivityManager::class.java)
        return manager.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == TimerForegroundService::class.java.name
        }
    }
}
