package com.ashutosh.flowtimer.data

import kotlinx.coroutines.flow.Flow

/**
 * Single point of access for all persisted timer preferences.
 * Platform implementations: [AndroidFlowTimerRepository] (DataStore),
 * [IosFlowTimerRepository] (NSUserDefaults via multiplatform-settings).
 */
interface FlowTimerRepository {

    val flowDurationMinutes: Flow<Int>
    val timerState: Flow<String>
    val remainingMillis: Flow<Long>
    val lastStartEpoch: Flow<Long>
    val completedSessionCount: Flow<Int>
    val lastCompletedEpoch: Flow<Long>

    suspend fun setFlowDurationMinutes(minutes: Int)
    suspend fun setTimerState(state: String)
    suspend fun setRemainingMillis(millis: Long)
    suspend fun setLastStartEpoch(epochMillis: Long)
    suspend fun recordSessionCompleted()

    /** Resets timer-related keys to defaults. Duration is preserved. */
    suspend fun resetTimerState()

    companion object {
        const val DEFAULT_FLOW_DURATION_MINUTES = 25
        const val DEFAULT_TIMER_STATE = "IDLE"
        const val DEFAULT_REMAINING_MILLIS = 0L
        const val DEFAULT_LAST_START_EPOCH = 0L
        const val DEFAULT_COMPLETED_SESSION_COUNT = 0
        const val DEFAULT_LAST_COMPLETED_EPOCH = 0L
    }
}
