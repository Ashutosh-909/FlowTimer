package com.ashutosh.flowtimer.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ashutosh.flowtimer.MainActivity
import com.ashutosh.flowtimer.R

/**
 * Centralised notification builder for the timer foreground service.
 *
 * Creates two channels:
 * - **Timer** — ongoing foreground-service notification while the timer is active.
 * - **Completion** — high-priority heads-up notification when the timer finishes.
 */
internal object NotificationHelper {

    const val TIMER_CHANNEL_ID = "flow_timer_channel"
    const val COMPLETION_CHANNEL_ID = "flow_timer_completion_channel"

    const val TIMER_NOTIFICATION_ID = 1
    const val COMPLETION_NOTIFICATION_ID = 2

    /** Must be called once (idempotent) before posting any notification. */
    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        val timerChannel = NotificationChannel(
            TIMER_CHANNEL_ID,
            "Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing notification while a flow session is active"
            setShowBadge(false)
        }

        val completionChannel = NotificationChannel(
            COMPLETION_CHANNEL_ID,
            "Timer Complete",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alert when your flow session finishes"
        }

        manager.createNotificationChannels(listOf(timerChannel, completionChannel))
    }

    // ── Ongoing timer notification ──────────────────────────────────────

    /**
     * Build the ongoing foreground notification showing remaining time.
     *
     * Includes a **Cancel** action to reset the timer.
     */
    fun buildTimerNotification(
        context: Context,
        remainingMs: Long
    ): Notification {
        val minutes = (remainingMs / 60_000).toInt()
        val seconds = ((remainingMs % 60_000) / 1_000).toInt()
        val timeText = String.format("%02d:%02d", minutes, seconds)

        val contentTitle = "Flow Time"
        val contentText = "$timeText remaining"

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, TIMER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_hourglass_notification)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        // Cancel (reset) action
        builder.addAction(
            0,
            "Cancel",
            buildServicePendingIntent(context, TimerForegroundService.ACTION_RESET, 2)
        )

        return builder.build()
    }

    // ── Completion notification ──────────────────────────────────────────

    /** Build the heads-up notification shown when the timer finishes. */
    fun buildCompletionNotification(context: Context): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, COMPLETION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_hourglass_notification)
            .setContentTitle("Flow time up!")
            .setContentText("Great focus session — tap to return")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000L)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .build()
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun buildServicePendingIntent(
        context: Context,
        action: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, TimerForegroundService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
