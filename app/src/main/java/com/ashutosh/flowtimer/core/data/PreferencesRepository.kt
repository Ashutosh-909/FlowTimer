package com.ashutosh.flowtimer.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Singleton DataStore instance scoped to the application context. */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "flow_prefs")

/**
 * Single point of access for all persisted preferences.
 *
 * All reads return a [Flow]; all writes are `suspend` functions.
 * No other class should access DataStore directly.
 */
class PreferencesRepository(private val dataStore: DataStore<Preferences>) {

    /** Secondary constructor accepting a [Context] for convenience. */
    constructor(context: Context) : this(context.dataStore)

    // ── Defaults ──

    companion object {
        const val DEFAULT_FLOW_DURATION_MINUTES = 25
        const val DEFAULT_TIMER_STATE = "IDLE"
        const val DEFAULT_REMAINING_MILLIS = 0L
        const val DEFAULT_LAST_START_EPOCH = 0L
        const val DEFAULT_COMPLETED_SESSION_COUNT = 0
        const val DEFAULT_LAST_COMPLETED_EPOCH = 0L
    }

    // ── Flow Duration ──

    val flowDurationMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.FLOW_DURATION_MINUTES] ?: DEFAULT_FLOW_DURATION_MINUTES
    }

    suspend fun setFlowDurationMinutes(minutes: Int) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.FLOW_DURATION_MINUTES] = minutes
        }
    }

    // ── Timer State ──

    val timerState: Flow<String> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.TIMER_STATE] ?: DEFAULT_TIMER_STATE
    }

    suspend fun setTimerState(state: String) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.TIMER_STATE] = state
        }
    }

    // ── Remaining Millis ──

    val remainingMillis: Flow<Long> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.REMAINING_MILLIS] ?: DEFAULT_REMAINING_MILLIS
    }

    suspend fun setRemainingMillis(millis: Long) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.REMAINING_MILLIS] = millis
        }
    }

    // ── Last Start Epoch ──

    val lastStartEpoch: Flow<Long> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.LAST_START_EPOCH] ?: DEFAULT_LAST_START_EPOCH
    }

    suspend fun setLastStartEpoch(epochMillis: Long) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.LAST_START_EPOCH] = epochMillis
        }
    }

    // ── Completed Session Count ──

    val completedSessionCount: Flow<Int> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.COMPLETED_SESSION_COUNT] ?: DEFAULT_COMPLETED_SESSION_COUNT
    }

    /** Increment the completed session count by 1 and record the completion time. */
    suspend fun recordSessionCompleted() {
        dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.COMPLETED_SESSION_COUNT] ?: DEFAULT_COMPLETED_SESSION_COUNT
            prefs[PreferencesKeys.COMPLETED_SESSION_COUNT] = current + 1
            prefs[PreferencesKeys.LAST_COMPLETED_EPOCH] = System.currentTimeMillis()
        }
    }

    // ── Last Completed Epoch ──

    val lastCompletedEpoch: Flow<Long> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.LAST_COMPLETED_EPOCH] ?: DEFAULT_LAST_COMPLETED_EPOCH
    }

    // ── Bulk Clear ──

    /** Resets all timer-related keys to defaults. Duration is preserved. */
    suspend fun resetTimerState() {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.TIMER_STATE] = DEFAULT_TIMER_STATE
            prefs[PreferencesKeys.REMAINING_MILLIS] = DEFAULT_REMAINING_MILLIS
            prefs[PreferencesKeys.LAST_START_EPOCH] = DEFAULT_LAST_START_EPOCH
        }
    }
}
