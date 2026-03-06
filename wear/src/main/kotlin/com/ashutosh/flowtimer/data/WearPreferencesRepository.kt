package com.ashutosh.flowtimer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Singleton DataStore for the Wear OS process. Separate from the phone store. */
private val Context.wearDataStore: DataStore<Preferences> by preferencesDataStore(name = "wear_flow_prefs")

/**
 * Local DataStore on the watch, populated by [WearDataListenerService] via DataLayer.
 * Read by [com.ashutosh.flowtimer.tile.FlowTimeTileService] and
 * [com.ashutosh.flowtimer.complication.FlowTimeComplicationService].
 */
class WearPreferencesRepository(context: Context) {

    private val dataStore = context.wearDataStore

    val timerState: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_TIMER_STATE] ?: DEFAULT_TIMER_STATE
    }

    val remainingMillis: Flow<Long> = dataStore.data.map { prefs ->
        prefs[KEY_REMAINING_MILLIS] ?: DEFAULT_REMAINING_MILLIS
    }

    val flowDurationMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_FLOW_DURATION_MINUTES] ?: DEFAULT_FLOW_DURATION_MINUTES
    }

    suspend fun updateFromDataLayer(
        timerState: String,
        remainingMillis: Long,
        durationMinutes: Int,
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_TIMER_STATE] = timerState
            prefs[KEY_REMAINING_MILLIS] = remainingMillis
            prefs[KEY_FLOW_DURATION_MINUTES] = durationMinutes
        }
    }

    companion object {
        private val KEY_TIMER_STATE = stringPreferencesKey("timer_state")
        private val KEY_REMAINING_MILLIS = longPreferencesKey("remaining_millis")
        private val KEY_FLOW_DURATION_MINUTES = intPreferencesKey("flow_duration_minutes")

        const val DEFAULT_TIMER_STATE = "IDLE"
        const val DEFAULT_REMAINING_MILLIS = 0L
        const val DEFAULT_FLOW_DURATION_MINUTES = 25
    }
}
