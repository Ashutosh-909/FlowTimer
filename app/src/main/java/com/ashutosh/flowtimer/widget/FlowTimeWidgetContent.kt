package com.ashutosh.flowtimer.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
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
import com.ashutosh.flowtimer.core.timer.TimerState

// ── Widget palette (base) ──
private val WidgetBackground = ColorProvider(Color(0xFF0D1B2A))
private val WidgetCardBackground = ColorProvider(Color(0xFF162234))
private val WidgetTextColor = ColorProvider(Color(0xFFEAEAEA))
private val WidgetTextDim = ColorProvider(Color(0xFF7A8B9E))
private val WidgetAccentGold = ColorProvider(Color(0xFFFFB347))

// ── Running state: warm orange background to grab attention ──
private val WidgetRunningBackground = ColorProvider(Color(0xFFE65100))
private val WidgetRunningButtonBg = ColorProvider(Color(0xFFBF360C))
private val WidgetRunningTextBright = ColorProvider(Color(0xFFFFF3E0))

// ── Finished state: soft emerald for achievement feel ──
private val WidgetFinishedBackground = ColorProvider(Color(0xFF2ECC71))
private val WidgetFinishedTextDark = ColorProvider(Color(0xFF1B5E20))
private val WidgetFinishedButtonBg = ColorProvider(Color(0xFF27AE60))

/**
 * Returns the state-dependent background [ColorProvider].
 */
private fun backgroundFor(timerState: TimerState): ColorProvider = when (timerState) {
    is TimerState.Idle -> WidgetBackground
    is TimerState.Running -> WidgetRunningBackground
    is TimerState.Finished -> WidgetFinishedBackground
}

/**
 * Returns the primary text color appropriate for the current background.
 */
private fun primaryTextColorFor(timerState: TimerState): ColorProvider = when (timerState) {
    is TimerState.Idle -> WidgetTextColor
    is TimerState.Running -> WidgetRunningTextBright
    is TimerState.Finished -> WidgetFinishedTextDark
}

/**
 * Returns the secondary/dim text color appropriate for the current background.
 */
private fun secondaryTextColorFor(timerState: TimerState): ColorProvider = when (timerState) {
    is TimerState.Idle -> WidgetTextDim
    is TimerState.Running -> ColorProvider(Color(0xFFFFCC80))
    is TimerState.Finished -> ColorProvider(Color(0xFF2E7D32))
}

/**
 * Returns the button background color appropriate for the current state background.
 */
private fun buttonBgFor(timerState: TimerState): ColorProvider = when (timerState) {
    is TimerState.Idle -> WidgetCardBackground
    is TimerState.Running -> WidgetRunningButtonBg
    is TimerState.Finished -> WidgetFinishedButtonBg
}

// ── Root Content ────────────────────────────────────────────────────────

/**
 * Root content composable for the Flow Time widget.
 *
 * Visual design per state:
 * - **Idle:** Dark space background. Entire widget is tappable to start.
 *   Inviting "▶ START FLOW" call-to-action.
 * - **Running:** Bold blue background — unmissable on the home screen as
 *   a reminder the user is in flow. Shows countdown prominently with a
 *   small stop button.
 * - **Finished:** Soft emerald green background — celebratory achievement
 *   feel. "WELL DONE! ✦" status. Tappable to restart.
 *
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

    // Idle and Finished: entire widget is tappable (start / restart)
    val wholeWidgetAction: Action? = when (timerState) {
        is TimerState.Idle -> actionRunCallback<StartAction>()
        is TimerState.Finished -> actionRunCallback<StartAction>()
        is TimerState.Running -> null
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundFor(timerState))
            .cornerRadius(16.dp)
            .padding(8.dp)
            .let { mod -> if (wholeWidgetAction != null) mod.clickable(wholeWidgetAction) else mod },
        contentAlignment = Alignment.Center
    ) {
        when {
            size.width >= 320.dp && size.height >= 180.dp -> ExtraLargeWidgetLayout(
                timerState = timerState,
                formattedTime = formattedTime,
                durationMinutes = durationMinutes,
                widgetWidth = size.width,
                widgetHeight = size.height
            )
            size.width >= 250.dp -> LargeWidgetLayout(
                timerState = timerState,
                formattedTime = formattedTime,
                durationMinutes = durationMinutes,
                widgetWidth = size.width,
                widgetHeight = size.height
            )
            size.width >= 180.dp -> MediumWidgetLayout(
                timerState = timerState,
                formattedTime = formattedTime,
                durationMinutes = durationMinutes,
                widgetWidth = size.width,
                widgetHeight = size.height
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
 * Compact layout. State-driven content:
 * - Idle: "▶ START" + duration
 * - Running: countdown + small stop button (bold blue bg)
 * - Finished: "✦ DONE" + restart affordance (green bg)
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
        when (timerState) {
            is TimerState.Idle -> {
                HourglassImage(timerState, sizeDP = 24)
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    text = "▶ START",
                    style = TextStyle(
                        color = WidgetAccentGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    text = formattedTime,
                    style = TextStyle(
                        color = WidgetTextDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
            is TimerState.Running -> {
                TimerText(formattedTime, fontSize = 16, textColor = WidgetRunningTextBright)
                Spacer(GlanceModifier.width(8.dp))
                StopButton(buttonSize = 28)
            }
            is TimerState.Finished -> {
                Text(
                    text = "✦ DONE!",
                    style = TextStyle(
                        color = WidgetFinishedTextDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    text = "▶",
                    style = TextStyle(
                        color = WidgetFinishedTextDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

// ── Medium Layout (3×2, ~180×110 dp) ────────────────────────────────────

/**
 * Medium layout with state-driven content.
 * Scales icon/text based on actual widget dimensions.
 */
@Composable
private fun MediumWidgetLayout(
    timerState: TimerState,
    formattedTime: String,
    durationMinutes: Int,
    widgetWidth: Dp,
    widgetHeight: Dp
) {
    val scaleFactor = minOf(widgetWidth / 180.dp, widgetHeight / 110.dp)
    val timerFontSize = (18 * scaleFactor).toInt().coerceIn(14, 28)
    val statusFontSize = (9 * scaleFactor).toInt().coerceIn(7, 14)
    val buttonSize = (36 * scaleFactor).toInt().coerceIn(32, 52)
    val hourglassSize = (36 * scaleFactor).toInt().coerceIn(28, 56)

    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (timerState) {
            is TimerState.Idle -> {
                HourglassImage(timerState, sizeDP = hourglassSize)
                Spacer(GlanceModifier.height(4.dp))
                TimerText(formattedTime, fontSize = timerFontSize, textColor = WidgetTextDim)
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    text = "▶ TAP TO START",
                    style = TextStyle(
                        color = WidgetAccentGold,
                        fontSize = statusFontSize.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
            is TimerState.Running -> {
                StatusText(timerState, fontSize = statusFontSize)
                Spacer(GlanceModifier.height(4.dp))
                TimerText(formattedTime, fontSize = timerFontSize, textColor = WidgetRunningTextBright)
                Spacer(GlanceModifier.height(8.dp))
                StopButton(buttonSize = buttonSize)
            }
            is TimerState.Finished -> {
                StatusText(timerState, fontSize = statusFontSize)
                Spacer(GlanceModifier.height(4.dp))
                TimerText(formattedTime, fontSize = timerFontSize, textColor = WidgetFinishedTextDark)
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    text = "▶ AGAIN",
                    style = TextStyle(
                        color = WidgetFinishedTextDark,
                        fontSize = statusFontSize.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }
}

// ── Large Layout (4×2, ~250×110 dp) ─────────────────────────────────────

/**
 * Full layout: header + timer + status + controls.
 * Scales proportionally based on actual widget dimensions.
 */
@Composable
private fun LargeWidgetLayout(
    timerState: TimerState,
    formattedTime: String,
    durationMinutes: Int,
    widgetWidth: Dp,
    widgetHeight: Dp
) {
    val scaleFactor = minOf(widgetWidth / 250.dp, widgetHeight / 110.dp)
    val hourglassSize = (20 * scaleFactor).toInt().coerceIn(16, 36)
    val titleFontSize = (10 * scaleFactor).toInt().coerceIn(8, 18)
    val timerFontSize = (22 * scaleFactor).toInt().coerceIn(16, 36)
    val labelFontSize = (8 * scaleFactor).toInt().coerceIn(7, 14)
    val buttonSize = (36 * scaleFactor).toInt().coerceIn(32, 56)

    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title row
        Row(verticalAlignment = Alignment.CenterVertically) {
            HourglassImage(timerState, sizeDP = hourglassSize)
            Spacer(GlanceModifier.width(4.dp))
            Text(
                text = "FLOW TIME",
                style = TextStyle(
                    color = primaryTextColorFor(timerState),
                    fontSize = titleFontSize.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
        Spacer(GlanceModifier.height((4 * scaleFactor).toInt().coerceAtLeast(4).dp))

        when (timerState) {
            is TimerState.Idle -> {
                TimerText(formattedTime, fontSize = timerFontSize, textColor = WidgetTextDim)
                Spacer(GlanceModifier.height((4 * scaleFactor).toInt().coerceAtLeast(4).dp))
                Row(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${durationMinutes} min",
                        style = TextStyle(
                            color = WidgetTextDim,
                            fontSize = labelFontSize.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
                Spacer(GlanceModifier.height((4 * scaleFactor).toInt().coerceAtLeast(4).dp))
                Text(
                    text = "▶ TAP TO START",
                    style = TextStyle(
                        color = WidgetAccentGold,
                        fontSize = labelFontSize.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
            is TimerState.Running -> {
                TimerText(formattedTime, fontSize = timerFontSize, textColor = WidgetRunningTextBright)
                Spacer(GlanceModifier.height((4 * scaleFactor).toInt().coerceAtLeast(4).dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StatusText(timerState, fontSize = labelFontSize)
                    Spacer(GlanceModifier.width(12.dp))
                    StopButton(buttonSize = buttonSize)
                }
            }
            is TimerState.Finished -> {
                TimerText(formattedTime, fontSize = timerFontSize, textColor = WidgetFinishedTextDark)
                Spacer(GlanceModifier.height((4 * scaleFactor).toInt().coerceAtLeast(4).dp))
                StatusText(timerState, fontSize = labelFontSize)
                Spacer(GlanceModifier.height((4 * scaleFactor).toInt().coerceAtLeast(4).dp))
                Text(
                    text = "▶ START AGAIN",
                    style = TextStyle(
                        color = WidgetFinishedTextDark,
                        fontSize = labelFontSize.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }
}

// ── Extra-Large Layout (5×3+, ~320×180 dp) ──────────────────────────────

/**
 * Extra-large layout. Same structure as Large but with proportionally
 * larger elements to fill the space.
 */
@Composable
private fun ExtraLargeWidgetLayout(
    timerState: TimerState,
    formattedTime: String,
    durationMinutes: Int,
    widgetWidth: Dp,
    widgetHeight: Dp
) {
    val scaleFactor = minOf(widgetWidth / 320.dp, widgetHeight / 180.dp)
    val hourglassSize = (32 * scaleFactor).toInt().coerceIn(28, 56)
    val titleFontSize = (14 * scaleFactor).toInt().coerceIn(12, 24)
    val timerFontSize = (36 * scaleFactor).toInt().coerceIn(28, 52)
    val labelFontSize = (10 * scaleFactor).toInt().coerceIn(9, 18)
    val buttonSize = (48 * scaleFactor).toInt().coerceIn(40, 72)
    val spacing = (8 * scaleFactor).toInt().coerceAtLeast(8)

    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title row
        Row(verticalAlignment = Alignment.CenterVertically) {
            HourglassImage(timerState, sizeDP = hourglassSize)
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = "FLOW TIME",
                style = TextStyle(
                    color = primaryTextColorFor(timerState),
                    fontSize = titleFontSize.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
        Spacer(GlanceModifier.height(spacing.dp))

        when (timerState) {
            is TimerState.Idle -> {
                TimerText(formattedTime, fontSize = timerFontSize, textColor = WidgetTextDim)
                Spacer(GlanceModifier.height(spacing.dp))
                Row(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${durationMinutes} min",
                        style = TextStyle(
                            color = WidgetTextDim,
                            fontSize = labelFontSize.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
                Spacer(GlanceModifier.height(spacing.dp))
                Text(
                    text = "▶ TAP TO START",
                    style = TextStyle(
                        color = WidgetAccentGold,
                        fontSize = (labelFontSize + 2).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
            is TimerState.Running -> {
                TimerText(formattedTime, fontSize = timerFontSize, textColor = WidgetRunningTextBright)
                Spacer(GlanceModifier.height(spacing.dp))
                StatusText(timerState, fontSize = labelFontSize)
                Spacer(GlanceModifier.height(spacing.dp))
                StopButton(buttonSize = buttonSize)
            }
            is TimerState.Finished -> {
                TimerText(formattedTime, fontSize = timerFontSize, textColor = WidgetFinishedTextDark)
                Spacer(GlanceModifier.height(spacing.dp))
                StatusText(timerState, fontSize = labelFontSize)
                Spacer(GlanceModifier.height(spacing.dp))
                Text(
                    text = "▶ START AGAIN",
                    style = TextStyle(
                        color = WidgetFinishedTextDark,
                        fontSize = labelFontSize.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
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
    val contentDesc = when (timerState) {
        is TimerState.Idle -> "Flow timer idle"
        is TimerState.Running -> "Flow timer running"
        is TimerState.Finished -> "Flow session complete"
    }
    Image(
        provider = ImageProvider(drawableRes),
        contentDescription = contentDesc,
        contentScale = ContentScale.Fit,
        modifier = GlanceModifier.size(sizeDP.dp)
    )
}

/**
 * Timer readout text with configurable color for state-dependent styling.
 */
@Composable
private fun TimerText(
    formattedTime: String,
    fontSize: Int,
    textColor: ColorProvider = WidgetTextColor
) {
    Text(
        text = formattedTime,
        style = TextStyle(
            color = textColor,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    )
}

/**
 * Status label that adapts to the current timer state.
 * - Idle: "READY" (dim)
 * - Running: "IN FLOW" (bright on blue bg)
 * - Finished: "WELL DONE! ✦" (dark on green bg)
 */
@Composable
private fun StatusText(timerState: TimerState, fontSize: Int = 8) {
    val (label, color) = when (timerState) {
        is TimerState.Idle -> "READY" to WidgetTextDim
        is TimerState.Running -> "IN FLOW" to WidgetRunningTextBright
        is TimerState.Finished -> "WELL DONE! ✦" to WidgetFinishedTextDark
    }
    Text(
        text = label,
        style = TextStyle(
            color = color,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    )
}

/**
 * Small stop/reset button shown during Running state.
 * Sends [ResetAction] to stop the timer and return to Idle.
 */
@Composable
private fun StopButton(buttonSize: Int = 36) {
    WidgetButton(
        text = "■",
        contentDescription = "Stop timer",
        action = actionRunCallback<ResetAction>(),
        buttonSize = buttonSize,
        backgroundColor = WidgetRunningButtonBg
    )
}

/**
 * Generic styled button box with rounded corners.
 */
@Composable
private fun WidgetButton(
    text: String,
    contentDescription: String,
    action: Action,
    buttonSize: Int = 36,
    backgroundColor: ColorProvider = WidgetCardBackground
) {
    Box(
        modifier = GlanceModifier
            .size(buttonSize.dp)
            .cornerRadius(8.dp)
            .background(backgroundColor)
            .clickable(action),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = WidgetTextColor,
                fontSize = (buttonSize * 0.44f).toInt().sp
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
