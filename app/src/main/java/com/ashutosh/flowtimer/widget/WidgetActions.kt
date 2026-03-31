package com.ashutosh.flowtimer.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.ashutosh.flowtimer.core.service.TimerForegroundService

/**
 * Widget action callbacks that send intents to [TimerForegroundService].
 *
 * Each callback is a thin bridge — builds an [Intent] with the appropriate
 * action string, starts the foreground service, and returns. No Activity is
 * ever launched, per the project convention.
 *
 * **Important:** Service intents must be fired immediately (no suspension
 * before the call) so they land within the broadcast-receiver foreground-
 * service exemption window on Android 14+.
 */

// ── Start / Resume ──────────────────────────────────────────────────────

/**
 * Sends [TimerForegroundService.ACTION_START] to begin or resume a flow session.
 *
 * Does **not** read DataStore before starting the service — the service
 * itself reads the persisted duration and corrects if needed. This avoids
 * any delay that could cause the broadcast-receiver FGS exemption to expire
 * on Android 14+ (non-debuggable builds).
 */
class StartAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = TimerForegroundService.intent(context, TimerForegroundService.ACTION_START)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException on API 31+ or
            // SecurityException — log and degrade gracefully.
            Log.e("FlowTimeWidget", "Cannot start foreground service from widget", e)
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
        try {
            val intent = TimerForegroundService.intent(context, TimerForegroundService.ACTION_RESET)
            context.startService(intent)
        } catch (e: Exception) {
            Log.e("FlowTimeWidget", "Cannot send reset to service from widget", e)
        }
    }
}
