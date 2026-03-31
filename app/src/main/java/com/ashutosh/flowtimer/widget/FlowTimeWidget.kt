package com.ashutosh.flowtimer.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.ashutosh.flowtimer.core.data.PreferencesKeys
import com.ashutosh.flowtimer.core.data.PreferencesRepository
import com.ashutosh.flowtimer.core.timer.TimerState

/**
 * Jetpack Glance widget for Flow Time.
 *
 * Renders three responsive size classes:
 * - **Small  (2×1, ~110×40 dp):** Timer readout + Play/Pause toggle.
 * - **Medium (3×2, ~180×110 dp):** Hourglass icon + Timer + Play/Pause/Reset.
 * - **Large  (4×2, ~250×110 dp):** Full header + Timer + all controls + duration label.
 *
 * State is read from Glance's Preferences state store via [currentState].
 * The service pushes values into this store with [pushStateAndUpdate].
 */
class FlowTimeWidget : GlanceAppWidget() {

    companion object {
        /** Breakpoint for small widget (2×1 cells). */
        private val SMALL = DpSize(110.dp, 40.dp)

        /** Breakpoint for medium widget (3×2 cells). */
        private val MEDIUM = DpSize(180.dp, 110.dp)

        /** Breakpoint for large widget (4×2 cells). */
        private val LARGE = DpSize(250.dp, 110.dp)

        /** Breakpoint for extra-large widget (5×3+ cells). */
        private val EXTRA_LARGE = DpSize(320.dp, 180.dp)

        /**
         * Push timer state into every widget instance's Glance state store
         * and trigger a visual update.
         *
         * Call this from [TimerForegroundService] after writing to the app's
         * DataStore. Glance only re-renders when its managed state changes,
         * so we must copy the relevant values here.
         */
        suspend fun pushStateAndUpdate(
            context: Context,
            stateName: String,
            remainingMillis: Long,
            durationMinutes: Int
        ) {
            val manager = GlanceAppWidgetManager(context)
            val widget = FlowTimeWidget()
            val glanceIds = manager.getGlanceIds(FlowTimeWidget::class.java)
            glanceIds.forEach { glanceId ->
                updateAppWidgetState(
                    context,
                    PreferencesGlanceStateDefinition,
                    glanceId
                ) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[PreferencesKeys.TIMER_STATE] = stateName
                        this[PreferencesKeys.REMAINING_MILLIS] = remainingMillis
                        this[PreferencesKeys.FLOW_DURATION_MINUTES] = durationMinutes
                    }
                }
                widget.update(context, glanceId)
            }
        }
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SMALL, MEDIUM, LARGE, EXTRA_LARGE)
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
