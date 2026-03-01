package com.ashutosh.flowtimer.core.data

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Single source of truth for all Preferences DataStore keys.
 * No inline key strings anywhere else in the codebase.
 */
internal object PreferencesKeys {

    /** User-configured flow duration in minutes. Default: 25. */
    val FLOW_DURATION_MINUTES = intPreferencesKey("flow_duration_minutes")

    /** Persisted timer state for widget cold read. Default: "IDLE". */
    val TIMER_STATE = stringPreferencesKey("timer_state")

    /** Remaining time in milliseconds for widget display & service recovery. Default: 0. */
    val REMAINING_MILLIS = longPreferencesKey("remaining_millis")

    /** Epoch millis when timer was last started (drift correction). Default: 0. */
    val LAST_START_EPOCH = longPreferencesKey("last_start_epoch")

    /** Total number of completed flow sessions. Default: 0. */
    val COMPLETED_SESSION_COUNT = intPreferencesKey("completed_session_count")

    /** Epoch millis of the most recent session completion. Default: 0. */
    val LAST_COMPLETED_EPOCH = longPreferencesKey("last_completed_epoch")
}
