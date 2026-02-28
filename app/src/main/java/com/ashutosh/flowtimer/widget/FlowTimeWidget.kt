package com.ashutosh.flowtimer.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.ashutosh.flowtimer.core.data.PreferencesKeys
import com.ashutosh.flowtimer.core.data.PreferencesRepository
import com.ashutosh.flowtimer.core.timer.TimerState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Jetpack Glance widget for Flow Time.
 *
 * Renders three responsive size classes:
 * - **Small  (2×1, ~110×40 dp):** Timer readout + Play/Pause toggle.
 * - **Medium (3×2, ~180×110 dp):** Hourglass icon + Timer + Play/Pause/Reset.
 * - **Large  (4×2, ~250×110 dp):** Full header + Timer + all controls + duration label.
 *
 * State is read from Preferences DataStore via [PreferencesGlanceStateDefinition].
 */
class FlowTimeWidget : GlanceAppWidget() {

    companion object {
        /** Breakpoint for small widget (2×1 cells). */
        private val SMALL = DpSize(110.dp, 40.dp)

        /** Breakpoint for medium widget (3×2 cells). */
        private val MEDIUM = DpSize(180.dp, 110.dp)

        /** Breakpoint for large widget (4×2 cells). */
        private val LARGE = DpSize(250.dp, 110.dp)
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SMALL, MEDIUM, LARGE)
    )

    override val stateDefinition: GlanceStateDefinition<*> =
        PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()

            val timerState = TimerState.fromName(
                prefs[PreferencesKeys.TIMER_STATE]
                    ?: PreferencesRepository.DEFAULT_TIMER_STATE
            )
            val remainingMillis = prefs[PreferencesKeys.REMAINING_MILLIS]
                ?: PreferencesRepository.DEFAULT_REMAINING_MILLIS
            val durationMinutes = prefs[PreferencesKeys.FLOW_DURATION_MINUTES]
                ?: PreferencesRepository.DEFAULT_FLOW_DURATION_MINUTES

            // Determine display time: if idle, show full duration; otherwise remaining.
            val displayMillis = when (timerState) {
                is TimerState.Idle -> durationMinutes * 60_000L
                is TimerState.Running -> remainingMillis
                is TimerState.Finished -> 0L
            }

            GlanceTheme {
                FlowTimeWidgetContent(
                    timerState = timerState,
                    displayMillis = displayMillis,
                    durationMinutes = durationMinutes
                )
            }
        }
    }
}
