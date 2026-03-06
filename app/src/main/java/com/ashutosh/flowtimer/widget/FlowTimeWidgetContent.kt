package com.ashutosh.flowtimer.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ashutosh.flowtimer.R
import com.ashutosh.flowtimer.timer.TimerState

// ── Widget palette ──
private val WidgetBackground = ColorProvider(Color(0xFF0D1B2A))
private val WidgetCardBackground = ColorProvider(Color(0xFF162234))
private val WidgetTextColor = ColorProvider(Color(0xFFEAEAEA))
private val WidgetTextDim = ColorProvider(Color(0xFF7A8B9E))
private val WidgetAccentBlue = ColorProvider(Color(0xFF4A90D9))
private val WidgetAccentGold = ColorProvider(Color(0xFFFFB347))

/**
 * Root content composable for the Flow Time widget.
 * Dispatches to the appropriate layout based on the Glance-provided size.
 */
@Composable
internal fun FlowTimeWidgetContent(
    timerState: TimerState,
    displayMillis: Long,
    durationMinutes: Int
) {
    val size = LocalSize.current
    val formattedTime = formatMillis(displayMillis)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetBackground)
            .cornerRadius(16.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            size.width >= 250.dp -> LargeWidgetLayout(
                timerState = timerState,
                formattedTime = formattedTime,
                durationMinutes = durationMinutes
            )
            size.width >= 180.dp -> MediumWidgetLayout(
                timerState = timerState,
                formattedTime = formattedTime
            )
            else -> SmallWidgetLayout(
                timerState = timerState,
                formattedTime = formattedTime
            )
        }
    }
}

// ── Small Layout (2×1, ~110×40 dp) ──────────────────────────────────────

/**
 * Compact layout: timer readout + Play/Pause toggle.
 */
@Composable
private fun SmallWidgetLayout(
    timerState: TimerState,
    formattedTime: String
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HourglassImage(timerState, sizeDP = 28)
        Spacer(GlanceModifier.width(4.dp))
        TimerText(formattedTime, fontSize = 14)
        Spacer(GlanceModifier.width(8.dp))
        PlayPauseButton(timerState)
    }
}

// ── Medium Layout (3×2, ~180×110 dp) ────────────────────────────────────

/**
 * Medium layout: hourglass icon + timer + Play/Pause + Reset.
 */
@Composable
private fun MediumWidgetLayout(
    timerState: TimerState,
    formattedTime: String
) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HourglassImage(timerState, sizeDP = 36)
        Spacer(GlanceModifier.height(4.dp))

        TimerText(formattedTime, fontSize = 16)
        Spacer(GlanceModifier.height(8.dp))

        // Status indicator
        StatusText(timerState)
        Spacer(GlanceModifier.height(4.dp))

        Row(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlayPauseButton(timerState)
            Spacer(GlanceModifier.width(8.dp))
            ResetButton(timerState)
        }
    }
}

// ── Large Layout (4×2, ~250×110 dp) ─────────────────────────────────────

/**
 * Full layout: header + timer + all controls + duration label.
 */
@Composable
private fun LargeWidgetLayout(
    timerState: TimerState,
    formattedTime: String,
    durationMinutes: Int
) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title row
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            HourglassImage(timerState, sizeDP = 20)
            Spacer(GlanceModifier.width(4.dp))
            Text(
                text = "FLOW TIME",
                style = TextStyle(
                    color = WidgetTextColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
        Spacer(GlanceModifier.height(4.dp))

        // Timer readout
        TimerText(formattedTime, fontSize = 20)
        Spacer(GlanceModifier.height(4.dp))

        // Status + duration label
        Row(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusText(timerState)
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = "${durationMinutes} min",
                style = TextStyle(
                    color = WidgetTextDim,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
        Spacer(GlanceModifier.height(4.dp))

        // Control buttons
        Row(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlayPauseButton(timerState)
            Spacer(GlanceModifier.width(8.dp))
            ResetButton(timerState)
        }
    }
}

// ── Shared Components ───────────────────────────────────────────────────

/**
 * Hourglass PNG image. Shows normal orientation when idle/finished,
 * flipped 180° (pre-rotated drawable) when running.
 */
@Composable
private fun HourglassImage(timerState: TimerState, sizeDP: Int) {
    val drawableRes = when (timerState) {
        is TimerState.Running -> R.drawable.hourglass_flipped
        is TimerState.Idle -> R.drawable.hourglass
        is TimerState.Finished -> R.drawable.hourglass
    }
    Image(
        provider = ImageProvider(drawableRes),
        contentDescription = "Hourglass",
        contentScale = ContentScale.Fit,
        modifier = GlanceModifier.size(sizeDP.dp)
    )
}

@Composable
private fun TimerText(formattedTime: String, fontSize: Int) {
    Text(
        text = formattedTime,
        style = TextStyle(
            color = WidgetTextColor,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    )
}

@Composable
private fun StatusText(timerState: TimerState) {
    val (label, color) = when (timerState) {
        is TimerState.Idle -> "READY" to WidgetTextDim
        is TimerState.Running -> "FLOWING" to WidgetAccentBlue
        is TimerState.Finished -> "DONE ✦" to WidgetAccentGold
    }
    Text(
        text = label,
        style = TextStyle(
            color = color,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
    )
}

/**
 * Play/Reset toggle button.
 * Shows ▶ when idle/finished, ↺ when running.
 * Tapping sends [StartAction] (idle/finished) or [ResetAction] (running).
 */
@Composable
private fun PlayPauseButton(timerState: TimerState) {
    val label = when (timerState) {
        is TimerState.Running -> "↺"
        is TimerState.Idle -> "▶"
        is TimerState.Finished -> "▶"
    }
    val contentDesc = when (timerState) {
        is TimerState.Running -> "Reset timer"
        is TimerState.Idle -> "Start timer"
        is TimerState.Finished -> "Start timer"
    }
    // Running → reset (pause/stop); Idle/Finished → start
    val action: Action = when (timerState) {
        is TimerState.Running -> actionRunCallback<ResetAction>()
        is TimerState.Idle -> actionRunCallback<StartAction>()
        is TimerState.Finished -> actionRunCallback<StartAction>()
    }
    WidgetButton(text = label, contentDescription = contentDesc, action = action)
}

/**
 * Reset button. Visually dimmed when idle (no action attached).
 */
@Composable
private fun ResetButton(timerState: TimerState) {
    val isIdle = timerState is TimerState.Idle
    WidgetButton(
        text = "↺",
        contentDescription = "Reset timer",
        dimmed = isIdle,
        action = if (isIdle) null else actionRunCallback<ResetAction>()
    )
}

@Composable
private fun WidgetButton(
    text: String,
    contentDescription: String,
    dimmed: Boolean = false,
    action: Action? = null
) {
    val modifier = GlanceModifier
        .size(36.dp)
        .cornerRadius(8.dp)
        .background(WidgetCardBackground)
        .let { mod -> if (action != null) mod.clickable(action) else mod }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = if (dimmed) WidgetTextDim else WidgetTextColor,
                fontSize = 16.sp
            )
        )
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────

/** Format milliseconds to `MM:SS` display string. */
internal fun formatMillis(millis: Long): String {
    val totalSeconds = (millis / 1_000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
