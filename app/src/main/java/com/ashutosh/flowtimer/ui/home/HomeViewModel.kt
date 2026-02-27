package com.ashutosh.flowtimer.ui.home

import android.app.ActivityManager
import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ashutosh.flowtimer.core.data.PreferencesRepository
import com.ashutosh.flowtimer.core.service.TimerForegroundService
import com.ashutosh.flowtimer.core.timer.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the home screen. Exposes a single [UiState] derived from
 * [PreferencesRepository] (DataStore). The foreground service is the source
 * of truth while the timer is active — the ViewModel reads persisted state
 * only, and sends control intents to the service.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PreferencesRepository(application)

    /** Whether the duration picker dialog should be shown. */
    private val _showDurationPicker = MutableStateFlow(false)
    val showDurationPicker: StateFlow<Boolean> = _showDurationPicker.asStateFlow()

    init {
        // Recover from stale DataStore state left by a previous crash.
        // If the persisted state is Running/Paused but the foreground service
        // is not alive, reset to Idle so the user isn't stuck on 00:00.
        viewModelScope.launch {
            val stateName = repository.timerState.first()
            val state = TimerState.fromName(stateName)
            when (state) {
                is TimerState.Running, is TimerState.Paused -> {
                    if (!isTimerServiceRunning()) {
                        repository.resetTimerState()
                    }
                }
                is TimerState.Finished -> {
                    // Finished with no service → reset so the user sees the
                    // duration again instead of a stuck 00:00.
                    repository.resetTimerState()
                }
                is TimerState.Idle -> { /* nothing to recover */ }
            }
        }
    }

    /**
     * Combined UI state built from DataStore flows.
     *
     * The service writes `timer_state`, `remaining_millis`, and
     * `flow_duration_minutes` on every tick, so this flow stays in sync.
     */
    val uiState: StateFlow<UiState> = combine(
        repository.timerState,
        repository.remainingMillis,
        repository.flowDurationMinutes
    ) { stateName, remainingMs, durationMinutes ->
        val timerState = TimerState.fromName(stateName)
        val totalMs = durationMinutes * 60_000L

        val displayMillis = when (timerState) {
            is TimerState.Idle -> totalMs
            is TimerState.Running, is TimerState.Paused -> remainingMs
            is TimerState.Finished -> 0L
        }

        val sandProgress = when (timerState) {
            is TimerState.Idle -> 0f
            is TimerState.Finished -> 1f
            is TimerState.Running, is TimerState.Paused -> {
                if (totalMs > 0) {
                    1f - (remainingMs.toFloat() / totalMs.toFloat())
                } else {
                    0f
                }.coerceIn(0f, 1f)
            }
        }

        UiState(
            timerState = timerState,
            displayMillis = displayMillis,
            durationMinutes = durationMinutes,
            sandProgress = sandProgress
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = UiState()
    )

    // ── User actions ─────────────────────────────────────────────────────

    /** Tap hourglass: start → pause → resume cycle. */
    fun onTapHourglass() {
        val current = uiState.value.timerState
        val context = getApplication<Application>()

        when (current) {
            is TimerState.Idle -> {
                val intent = TimerForegroundService.intent(context, TimerForegroundService.ACTION_START)
                    .putExtra(TimerForegroundService.EXTRA_DURATION_MINUTES, uiState.value.durationMinutes)
                context.startForegroundService(intent)
            }
            is TimerState.Running -> {
                context.startService(
                    TimerForegroundService.intent(context, TimerForegroundService.ACTION_PAUSE)
                )
            }
            is TimerState.Paused -> {
                context.startForegroundService(
                    TimerForegroundService.intent(context, TimerForegroundService.ACTION_RESUME)
                )
            }
            is TimerState.Finished -> {
                // Tap on finished → reset
                onLongPressReset()
            }
        }
    }

    /** Long-press hourglass: reset timer to persisted duration. */
    fun onLongPressReset() {
        val context = getApplication<Application>()
        context.startService(
            TimerForegroundService.intent(context, TimerForegroundService.ACTION_RESET)
        )
    }

    /** Tap "Set your focus time" — show duration picker. */
    fun onTapSetDuration() {
        if (uiState.value.timerState is TimerState.Idle) {
            _showDurationPicker.value = true
        }
    }

    /** Dismiss the duration picker without saving. */
    fun onDismissDurationPicker() {
        _showDurationPicker.value = false
    }

    /** Confirm a new duration from the picker. */
    fun onConfirmDuration(minutes: Int) {
        viewModelScope.launch {
            repository.setFlowDurationMinutes(minutes)
            // Also update remaining millis to reflect the new duration in Idle
            repository.setRemainingMillis(minutes * 60_000L)
        }
        _showDurationPicker.value = false
    }

    // ── UiState ──────────────────────────────────────────────────────────

    /**
     * Single immutable state object for the home screen.
     */
    data class UiState(
        val timerState: TimerState = TimerState.Idle,
        val displayMillis: Long = PreferencesRepository.DEFAULT_FLOW_DURATION_MINUTES * 60_000L,
        val durationMinutes: Int = PreferencesRepository.DEFAULT_FLOW_DURATION_MINUTES,
        val sandProgress: Float = 0f
    ) {
        /** Formatted time string for display (e.g., "25:00"). */
        val formattedTime: String
            get() {
                val totalSeconds = (displayMillis / 1_000).toInt()
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                return String.format("%02d:%02d", minutes, seconds)
            }

        /** Accessible time description for TalkBack. */
        val accessibilityTimeDescription: String
            get() {
                val totalSeconds = (displayMillis / 1_000).toInt()
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                return buildString {
                    if (minutes > 0) append("$minutes minute${if (minutes != 1) "s" else ""}")
                    if (minutes > 0 && seconds > 0) append(", ")
                    if (seconds > 0 || minutes == 0) append("$seconds second${if (seconds != 1) "s" else ""}")
                    if (timerState is TimerState.Finished) append(". Timer complete.")
                }
            }

        /** Accessibility state description for the hourglass. */
        val accessibilityStateDescription: String
            get() = when (timerState) {
                is TimerState.Idle -> "Idle, $formattedTime"
                is TimerState.Running -> "Running, $accessibilityTimeDescription remaining"
                is TimerState.Paused -> "Paused, $accessibilityTimeDescription remaining"
                is TimerState.Finished -> "Complete"
            }

        /** Whether the timer is in idle state. */
        val isIdle: Boolean get() = timerState is TimerState.Idle
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /**
     * Check whether [TimerForegroundService] is currently running.
     * Used to detect stale DataStore state after a crash.
     */
    @Suppress("DEPRECATION") // getRunningServices is deprecated but still works for own services
    private fun isTimerServiceRunning(): Boolean {
        val manager = getApplication<Application>()
            .getSystemService(ActivityManager::class.java)
        return manager.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == TimerForegroundService::class.java.name
        }
    }
}
