package com.ashutosh.flowtimer.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.ashutosh.flowtimer.data.WearPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Provides a SHORT_TEXT complication showing remaining MM:SS and timer state.
 * Updated every 60 s via the manifest UPDATE_PERIOD_SECONDS meta-data.
 */
class FlowTimeComplicationService : ComplicationDataSourceService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener,
    ) {
        if (request.complicationType != ComplicationType.SHORT_TEXT) {
            listener.onComplicationData(null)
            return
        }

        scope.launch {
            try {
                val repository = WearPreferencesRepository(applicationContext)
                val timerState = repository.timerState.first()
                val remainingMillis = repository.remainingMillis.first()
                val durationMinutes = repository.flowDurationMinutes.first()

                val displayMs = when (timerState) {
                    "RUNNING" -> remainingMillis.coerceAtLeast(0L)
                    "FINISHED" -> 0L
                    else -> durationMinutes * 60_000L
                }
                val timeText = formatTime(displayMs)

                listener.onComplicationData(
                    ShortTextComplicationData.Builder(
                        text = PlainComplicationText.Builder(timeText).build(),
                        contentDescription = PlainComplicationText.Builder("Flow Timer: $timeText").build(),
                    ).build()
                )
            } catch (e: Exception) {
                listener.onComplicationData(null)
            }
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("25:00").build(),
            contentDescription = PlainComplicationText.Builder("Flow Timer").build(),
        ).build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun formatTime(millis: Long): String {
        val minutes = (millis / 60_000L).toInt()
        val seconds = ((millis % 60_000L) / 1_000L).toInt()
        return "%02d:%02d".format(minutes, seconds)
    }
}
