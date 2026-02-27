package com.ashutosh.flowtimer.core.timer

/**
 * Sealed class representing the three possible states of the timer.
 * Exhaustive `when` — no `else` branch needed.
 *
 * There is no Paused state: tapping while running resets to Idle.
 */
sealed class TimerState {
    data object Idle : TimerState()
    data object Running : TimerState()
    data object Finished : TimerState()

    /** Serialization name for DataStore persistence. */
    val name: String
        get() = when (this) {
            is Idle -> "IDLE"
            is Running -> "RUNNING"
            is Finished -> "FINISHED"
        }

    companion object {
        /** Deserialize from DataStore string. */
        fun fromName(name: String): TimerState = when (name) {
            "RUNNING" -> Running
            "FINISHED" -> Finished
            else -> Idle
        }
    }
}
