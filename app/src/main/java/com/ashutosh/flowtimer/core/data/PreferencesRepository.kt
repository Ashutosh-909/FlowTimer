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
