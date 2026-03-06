package com.ashutosh.flowtimer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ashutosh.flowtimer.currentEpochMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "flow_prefs")

private object Keys {
    val FLOW_DURATION_MINUTES = intPreferencesKey("flow_duration_minutes")
    val TIMER_STATE = stringPreferencesKey("timer_state")
    val REMAINING_MILLIS = longPreferencesKey("remaining_millis")
    val LAST_START_EPOCH = longPreferencesKey("last_start_epoch")
    val COMPLETED_SESSION_COUNT = intPreferencesKey("completed_session_count")
    val LAST_COMPLETED_EPOCH = longPreferencesKey("last_completed_epoch")
}

class AndroidFlowTimerRepository(private val dataStore: DataStore<Preferences>) : FlowTimerRepository {

    constructor(context: Context) : this(context.dataStore)

    override val flowDurationMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.FLOW_DURATION_MINUTES] ?: FlowTimerRepository.DEFAULT_FLOW_DURATION_MINUTES
    }

    override val timerState: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.TIMER_STATE] ?: FlowTimerRepository.DEFAULT_TIMER_STATE
    }

    override val remainingMillis: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.REMAINING_MILLIS] ?: FlowTimerRepository.DEFAULT_REMAINING_MILLIS
    }

    override val lastStartEpoch: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.LAST_START_EPOCH] ?: FlowTimerRepository.DEFAULT_LAST_START_EPOCH
    }

    override val completedSessionCount: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.COMPLETED_SESSION_COUNT] ?: FlowTimerRepository.DEFAULT_COMPLETED_SESSION_COUNT
    }

    override val lastCompletedEpoch: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.LAST_COMPLETED_EPOCH] ?: FlowTimerRepository.DEFAULT_LAST_COMPLETED_EPOCH
    }

    override suspend fun setFlowDurationMinutes(minutes: Int) {
        dataStore.edit { it[Keys.FLOW_DURATION_MINUTES] = minutes }
    }

    override suspend fun setTimerState(state: String) {
        dataStore.edit { it[Keys.TIMER_STATE] = state }
    }

    override suspend fun setRemainingMillis(millis: Long) {
        dataStore.edit { it[Keys.REMAINING_MILLIS] = millis }
    }

    override suspend fun setLastStartEpoch(epochMillis: Long) {
        dataStore.edit { it[Keys.LAST_START_EPOCH] = epochMillis }
    }

    override suspend fun recordSessionCompleted() {
        dataStore.edit { prefs ->
            val current = prefs[Keys.COMPLETED_SESSION_COUNT]
                ?: FlowTimerRepository.DEFAULT_COMPLETED_SESSION_COUNT
            prefs[Keys.COMPLETED_SESSION_COUNT] = current + 1
            prefs[Keys.LAST_COMPLETED_EPOCH] = currentEpochMillis()
        }
    }

    override suspend fun resetTimerState() {
        dataStore.edit { prefs ->
            prefs[Keys.TIMER_STATE] = FlowTimerRepository.DEFAULT_TIMER_STATE
            prefs[Keys.REMAINING_MILLIS] = FlowTimerRepository.DEFAULT_REMAINING_MILLIS
            prefs[Keys.LAST_START_EPOCH] = FlowTimerRepository.DEFAULT_LAST_START_EPOCH
        }
    }
}
