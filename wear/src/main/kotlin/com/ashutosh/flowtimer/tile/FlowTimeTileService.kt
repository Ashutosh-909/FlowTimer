package com.ashutosh.flowtimer.tile

import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.ashutosh.flowtimer.data.WearPreferencesRepository
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.TimelineBuilders

/**
 * Wear OS Tile that displays the current Flow Timer state.
 *
 * Reads timer state from [WearPreferencesRepository] (local DataStore on the watch),
 * which is kept up-to-date by [com.ashutosh.flowtimer.data.WearDataListenerService]
 * via the Wearable DataLayer.
 *
 * Layout: MM:SS time + state label (FLOWING / READY / DONE) on SpaceBackground.
 */
class FlowTimeTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: WearPreferencesRepository

    override fun onCreate() {
        super.onCreate()
        repository = WearPreferencesRepository(applicationContext)
    }

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<TileBuilders.Tile> = scope.future {
        val timerState = repository.timerState.first()
        val remainingMillis = repository.remainingMillis.first()
        val durationMinutes = repository.flowDurationMinutes.first()

        val layout = LayoutElementBuilders.Layout.Builder()
            .setRoot(TileRenderer.buildLayout(timerState, remainingMillis, durationMinutes))
            .build()

        val timelineEntry = TimelineBuilders.TimelineEntry.Builder()
            .setLayout(layout)
            .build()

        val timeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(timelineEntry)
            .build()

        TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(timeline)
            .build()
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> = scope.future {
        ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val RESOURCES_VERSION = "1"
    }
}
