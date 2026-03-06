package com.ashutosh.flowtimer.data

import android.content.ComponentName
import androidx.wear.tiles.TileService
import com.ashutosh.flowtimer.tile.FlowTimeTileService
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Listens for timer state changes published by the phone app via the Wearable DataLayer
 * on path [FLOWTIMER_STATE_PATH]. Persists received values to the local [WearPreferencesRepository]
 * and requests a Tile refresh so the watch face shows the latest state.
 */
class WearDataListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == FLOWTIMER_STATE_PATH
            ) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val timerState = dataMap.getString("timer_state", WearPreferencesRepository.DEFAULT_TIMER_STATE)
                val remainingMillis = dataMap.getLong("remaining_millis", WearPreferencesRepository.DEFAULT_REMAINING_MILLIS)
                val durationMinutes = dataMap.getInt("flow_duration_minutes", WearPreferencesRepository.DEFAULT_FLOW_DURATION_MINUTES)

                scope.launch {
                    val repository = WearPreferencesRepository(applicationContext)
                    repository.updateFromDataLayer(timerState, remainingMillis, durationMinutes)
                    requestTileUpdate()
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun requestTileUpdate() {
        TileService.getUpdater(applicationContext)
            .requestUpdate(FlowTimeTileService::class.java)
    }

    companion object {
        const val FLOWTIMER_STATE_PATH = "/flowtimer/state"
    }
}
