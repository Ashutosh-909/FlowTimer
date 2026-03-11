package com.ashutosh.flowtimer.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.ashutosh.flowtimer.core.data.PreferencesRepository
import com.ashutosh.flowtimer.core.service.TimerForegroundService
import kotlinx.coroutines.flow.first

/**
 * Widget action callbacks that send intents to [TimerForegroundService].
 *
 * Each callback is a thin bridge — builds an [Intent] with the appropriate
 * action string, starts the foreground service, and returns. No Activity is
 * ever launched, per the project convention.
 */

// ── Start / Resume ──────────────────────────────────────────────────────

/**
 * Sends [TimerForegroundService.ACTION_START] to begin or resume a flow session.
 * Reads the current duration from DataStore and passes it to the service.
 */
class StartAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Read the current duration from DataStore
        val repository = PreferencesRepository(context)
        val durationMinutes = repository.flowDurationMinutes.first()

        val intent = TimerForegroundService.intent(context, TimerForegroundService.ACTION_START)
            .putExtra(TimerForegroundService.EXTRA_DURATION_MINUTES, durationMinutes)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}

// ── Reset ───────────────────────────────────────────────────────────────

/**
 * Sends [TimerForegroundService.ACTION_RESET] to stop the timer and return
 * to idle state with the persisted duration.
 */
class ResetAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = TimerForegroundService.intent(context, TimerForegroundService.ACTION_RESET)
        context.startService(intent)
    }
}
