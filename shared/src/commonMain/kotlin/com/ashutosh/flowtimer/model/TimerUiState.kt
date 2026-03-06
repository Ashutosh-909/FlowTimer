package com.ashutosh.flowtimer.model

import com.ashutosh.flowtimer.data.FlowTimerRepository
import com.ashutosh.flowtimer.timer.TimerState

/**
 * Single immutable state object for the timer home screen.
 * Shared across Android and iOS.
 */
data class TimerUiState(
    val timerState: TimerState = TimerState.Idle,
    val displayMillis: Long = FlowTimerRepository.DEFAULT_FLOW_DURATION_MINUTES * 60_000L,
    val durationMinutes: Int = FlowTimerRepository.DEFAULT_FLOW_DURATION_MINUTES,
    val sandProgress: Float = 0f,
    val completedSessionCount: Int = 0
) {
    /** Formatted time string for display (e.g., "25:00"). */
    val formattedTime: String
        get() {
            val totalSeconds = (displayMillis / 1_000).toInt()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        }

    /** Accessible time description for screen readers. */
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

    val accessibilityStateDescription: String
        get() = when (timerState) {
            is TimerState.Idle -> "Idle, $formattedTime"
            is TimerState.Running -> "Running, $accessibilityTimeDescription remaining"
            is TimerState.Finished -> "Complete"
        }

    val isIdle: Boolean get() = timerState is TimerState.Idle
}
