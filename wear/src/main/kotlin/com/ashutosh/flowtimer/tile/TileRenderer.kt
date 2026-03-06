package com.ashutosh.flowtimer.tile

import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders

/**
 * Builds the Wear Tile layout element from timer state.
 * Uses SpaceBackground (#0D1B2A) and a simple two-line display:
 *   line 1 — MM:SS remaining (or full duration when idle)
 *   line 2 — state label: FLOWING / DONE / READY
 */
internal object TileRenderer {

    private val COLOR_BACKGROUND = ColorBuilders.argb(0xFF0D1B2A.toInt())
    private val COLOR_TIME = ColorBuilders.argb(0xFFFFFFFF.toInt())
    private val COLOR_STATE = ColorBuilders.argb(0xFF7EC8E3.toInt())

    fun buildLayout(
        timerState: String,
        remainingMillis: Long,
        durationMinutes: Int,
    ): LayoutElementBuilders.LayoutElement {
        val timeText = formatTime(timerState, remainingMillis, durationMinutes)
        val stateLabel = when (timerState) {
            "RUNNING" -> "FLOWING"
            "FINISHED" -> "DONE"
            else -> "READY"
        }

        return LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(COLOR_BACKGROUND)
                            .build()
                    )
                    .build()
            )
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .addContent(
                LayoutElementBuilders.Column.Builder()
                    .setWidth(DimensionBuilders.wrap())
                    .setHeight(DimensionBuilders.wrap())
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .addContent(
                        LayoutElementBuilders.Text.Builder()
                            .setText(timeText)
                            .setFontStyle(
                                LayoutElementBuilders.FontStyle.Builder()
                                    .setSize(DimensionBuilders.sp(24f))
                                    .setColor(COLOR_TIME)
                                    .build()
                            )
                            .build()
                    )
                    .addContent(
                        LayoutElementBuilders.Spacer.Builder()
                            .setHeight(DimensionBuilders.dp(8f))
                            .build()
                    )
                    .addContent(
                        LayoutElementBuilders.Text.Builder()
                            .setText(stateLabel)
                            .setFontStyle(
                                LayoutElementBuilders.FontStyle.Builder()
                                    .setSize(DimensionBuilders.sp(12f))
                                    .setColor(COLOR_STATE)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun formatTime(timerState: String, remainingMillis: Long, durationMinutes: Int): String {
        val ms = when (timerState) {
            "RUNNING" -> remainingMillis.coerceAtLeast(0L)
            "FINISHED" -> 0L
            else -> durationMinutes * 60_000L
        }
        val minutes = (ms / 60_000L).toInt()
        val seconds = ((ms % 60_000L) / 1_000L).toInt()
        return "%02d:%02d".format(minutes, seconds)
    }
}
