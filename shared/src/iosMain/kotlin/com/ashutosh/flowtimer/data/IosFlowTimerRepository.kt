package com.ashutosh.flowtimer.data

import com.ashutosh.flowtimer.currentEpochMillis
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class IosFlowTimerRepository : FlowTimerRepository {

    private val settings = Settings()

    private val _flowDurationMinutes = MutableStateFlow(
        settings.getInt(KEY_FLOW_DURATION, FlowTimerRepository.DEFAULT_FLOW_DURATION_MINUTES)
    )
    private val _timerState = MutableStateFlow(
        settings.getString(KEY_TIMER_STATE, FlowTimerRepository.DEFAULT_TIMER_STATE)
    )
    private val _remainingMillis = MutableStateFlow(
        settings.getLong(KEY_REMAINING_MILLIS, FlowTimerRepository.DEFAULT_REMAINING_MILLIS)
    )
    private val _lastStartEpoch = MutableStateFlow(
        settings.getLong(KEY_LAST_START_EPOCH, FlowTimerRepository.DEFAULT_LAST_START_EPOCH)
    )
    private val _completedSessionCount = MutableStateFlow(
        settings.getInt(KEY_SESSION_COUNT, FlowTimerRepository.DEFAULT_COMPLETED_SESSION_COUNT)
    )
    private val _lastCompletedEpoch = MutableStateFlow(
        settings.getLong(KEY_LAST_COMPLETED_EPOCH, FlowTimerRepository.DEFAULT_LAST_COMPLETED_EPOCH)
    )

    override val flowDurationMinutes: Flow<Int> = _flowDurationMinutes
    override val timerState: Flow<String> = _timerState
    override val remainingMillis: Flow<Long> = _remainingMillis
    override val lastStartEpoch: Flow<Long> = _lastStartEpoch
    override val completedSessionCount: Flow<Int> = _completedSessionCount
    override val lastCompletedEpoch: Flow<Long> = _lastCompletedEpoch

    override suspend fun setFlowDurationMinutes(minutes: Int) {
        settings.putInt(KEY_FLOW_DURATION, minutes)
        _flowDurationMinutes.value = minutes
    }

    override suspend fun setTimerState(state: String) {
        settings.putString(KEY_TIMER_STATE, state)
        _timerState.value = state
    }

    override suspend fun setRemainingMillis(millis: Long) {
        settings.putLong(KEY_REMAINING_MILLIS, millis)
        _remainingMillis.value = millis
    }

    override suspend fun setLastStartEpoch(epochMillis: Long) {
        settings.putLong(KEY_LAST_START_EPOCH, epochMillis)
        _lastStartEpoch.value = epochMillis
    }

    override suspend fun recordSessionCompleted() {
        val next = _completedSessionCount.value + 1
        val now = currentEpochMillis()
        settings.putInt(KEY_SESSION_COUNT, next)
        settings.putLong(KEY_LAST_COMPLETED_EPOCH, now)
        _completedSessionCount.value = next
        _lastCompletedEpoch.value = now
    }

    override suspend fun resetTimerState() {
        setTimerState(FlowTimerRepository.DEFAULT_TIMER_STATE)
        setRemainingMillis(FlowTimerRepository.DEFAULT_REMAINING_MILLIS)
        setLastStartEpoch(FlowTimerRepository.DEFAULT_LAST_START_EPOCH)
    }

    companion object {
        private const val KEY_FLOW_DURATION = "flow_duration_minutes"
        private const val KEY_TIMER_STATE = "timer_state"
        private const val KEY_REMAINING_MILLIS = "remaining_millis"
        private const val KEY_LAST_START_EPOCH = "last_start_epoch"
        private const val KEY_SESSION_COUNT = "completed_session_count"
        private const val KEY_LAST_COMPLETED_EPOCH = "last_completed_epoch"
    }
}
