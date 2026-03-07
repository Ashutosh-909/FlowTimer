package com.ashutosh.flowtimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ashutosh.flowtimer.ui.dashboard.DashboardViewModel
import com.ashutosh.flowtimer.ui.theme.FlowTimerTheme
import com.ashutosh.flowtimer.ui.theme.GlowBlue
import com.ashutosh.flowtimer.ui.theme.GlowCyan
import com.ashutosh.flowtimer.ui.theme.PixelText
import com.ashutosh.flowtimer.ui.theme.PixelTextDim
import com.ashutosh.flowtimer.ui.theme.SandGold
import com.ashutosh.flowtimer.ui.theme.SpaceBackground
import com.ashutosh.flowtimer.ui.theme.SpaceMid
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Cell model for the internal heatmap grid.
 */
private data class DayCell(
    val date: LocalDate,
    val minutes: Int,
    val isInRange: Boolean
)

/**
 * GitHub-style calendar heatmap showing daily focus intensity over ~6 months.
 *
 * Layout: 7 rows (Mon–Sun) × N columns (weeks). Each cell is a 12 dp square
 * with 2 dp gap. Day-of-week labels on the left are fixed; the grid is
 * horizontally scrollable and auto-scrolls to the current week on first load.
 *
 * Color scale (5 levels):
 * - 0 min → [SpaceMid]
 * - 1–15 min → [GlowBlue] 30 %
 * - 16–45 min → [GlowBlue] 60 %
 * - 46–90 min → [GlowCyan] 85 %
 * - 91+ min → [SandGold] 100 %
 *
 * Tapping a cell shows a tooltip with the date and focus minutes.
 *
 * @param modifier Modifier to apply.
 * @param days Flat list of [DashboardViewModel.HeatmapDay] spanning ~6 months.
 */
@Composable
fun CalendarHeatmap(
    modifier: Modifier = Modifier,
    days: List<DashboardViewModel.HeatmapDay>
) {
    if (days.isEmpty()) return

    val scrollState = rememberScrollState()

    // Auto-scroll to rightmost column (current week) on first load.
    LaunchedEffect(days) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    val sortedDays = remember(days) { days.sortedBy { it.date } }
    val firstDate = sortedDays.first().date
    val lastDate = sortedDays.last().date

    // Adjust start to the Monday of the first week.
    val startMonday = firstDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    // Quick lookup from date to minutes.
    val dayMap = remember(sortedDays) { sortedDays.associateBy { it.date } }

    // Build week columns.
    val weeks = remember(startMonday, lastDate, dayMap) {
        val result = mutableListOf<List<DayCell>>()
        var weekStart = startMonday
        while (!weekStart.isAfter(lastDate)) {
            val weekCells = (0..6).map { dayOffset ->
                val date = weekStart.plusDays(dayOffset.toLong())
                val minutes = dayMap[date]?.totalMinutes ?: 0
                val inRange = !date.isBefore(firstDate) && !date.isAfter(lastDate)
                DayCell(date, minutes, inRange)
            }
            result.add(weekCells)
            weekStart = weekStart.plusWeeks(1)
        }
        result
    }

    // Selected cell for tooltip
    var selectedCell by remember { mutableStateOf<DayCell?>(null) }

    val totalDays = sortedDays.size
    val activeDays = sortedDays.count { it.totalMinutes > 0 }

    Column(
        modifier = modifier.clearAndSetSemantics {
            contentDescription =
                "Calendar heatmap showing $totalDays days of history with $activeDays active days"
        }
    ) {
        // ── Tooltip ──
        if (selectedCell != null) {
            val cell = selectedCell!!
            val dateText = "${cell.date.dayOfMonth} ${
                cell.date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            } ${cell.date.year}"
            Text(
                text = "$dateText · ${cell.minutes} min",
                style = MaterialTheme.typography.labelSmall,
                color = PixelText,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row {
            // ── Fixed day-of-week labels ──
            Column(
                modifier = Modifier.padding(end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Spacer for month label row
                Spacer(modifier = Modifier.height(12.dp))

                val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                dayLabels.forEach { label ->
                    Box(
                        modifier = Modifier.size(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = PixelTextDim
                        )
                    }
                }
            }

            // ── Scrollable grid ──
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                weeks.forEachIndexed { weekIndex, weekCells ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // ── Month label ──
                        val containsFirst = weekCells.any { it.date.dayOfMonth == 1 && it.isInRange }
                        val isFirstWeek = weekIndex == 0

                        if (containsFirst) {
                            val firstOfMonth = weekCells.first { it.date.dayOfMonth == 1 && it.isInRange }
                            Text(
                                text = firstOfMonth.date.month.getDisplayName(
                                    TextStyle.SHORT, Locale.getDefault()
                                ).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = PixelTextDim,
                                modifier = Modifier.height(12.dp)
                            )
                        } else if (isFirstWeek) {
                            val firstInRange = weekCells.firstOrNull { it.isInRange }
                            if (firstInRange != null) {
                                Text(
                                    text = firstInRange.date.month.getDisplayName(
                                        TextStyle.SHORT, Locale.getDefault()
                                    ).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PixelTextDim,
                                    modifier = Modifier.height(12.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // ── Day cells ──
                        weekCells.forEach { cell ->
                            val color = if (cell.isInRange) {
                                heatmapCellColor(cell.minutes)
                            } else {
                                Color.Transparent
                            }

                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(color, RoundedCornerShape(2.dp))
                                    .then(
                                        if (cell.isInRange) {
                                            Modifier.clickable {
                                                selectedCell =
                                                    if (selectedCell?.date == cell.date) null else cell
                                            }
                                        } else {
                                            Modifier
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Maps focus minutes for a single day to a heatmap color.
 *
 * 5-level scale:
 * - 0 → [SpaceMid]
 * - 1–15 → [GlowBlue] 30 %
 * - 16–45 → [GlowBlue] 60 %
 * - 46–90 → [GlowCyan] 85 %
 * - 91+ → [SandGold]
 */
private fun heatmapCellColor(minutes: Int): Color = when {
    minutes == 0 -> SpaceMid
    minutes <= 15 -> GlowBlue.copy(alpha = 0.3f)
    minutes <= 45 -> GlowBlue.copy(alpha = 0.6f)
    minutes <= 90 -> GlowCyan.copy(alpha = 0.85f)
    else -> SandGold
}

// ── Previews ─────────────────────────────────────────────────────────────

private fun sampleHeatmapDays(): List<DashboardViewModel.HeatmapDay> {
    val today = LocalDate.now()
    val start = today.minusMonths(6)
    val days = mutableListOf<DashboardViewModel.HeatmapDay>()
    var d = start
    var index = 0
    while (!d.isAfter(today)) {
        // Varied sample data
        val minutes = when {
            index % 7 == 0 -> 0
            index % 5 == 0 -> 100
            index % 3 == 0 -> 50
            index % 2 == 0 -> 20
            else -> 5
        }
        days.add(DashboardViewModel.HeatmapDay(d, minutes))
        d = d.plusDays(1)
        index++
    }
    return days
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1B2A, widthDp = 400)
@Composable
private fun CalendarHeatmapPreview() {
    FlowTimerTheme {
        Box(modifier = Modifier.background(SpaceBackground).padding(16.dp)) {
            CalendarHeatmap(
                days = sampleHeatmapDays(),
                modifier = Modifier.fillMaxWidth().height(140.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1B2A, widthDp = 400)
@Composable
private fun CalendarHeatmapEmptyPreview() {
    FlowTimerTheme {
        Box(modifier = Modifier.background(SpaceBackground).padding(16.dp)) {
            val today = LocalDate.now()
            val start = today.minusMonths(6)
            val emptyDays = mutableListOf<DashboardViewModel.HeatmapDay>()
            var d = start
            while (!d.isAfter(today)) {
                emptyDays.add(DashboardViewModel.HeatmapDay(d, 0))
                d = d.plusDays(1)
            }
            CalendarHeatmap(
                days = emptyDays,
                modifier = Modifier.fillMaxWidth().height(140.dp)
            )
        }
    }
}
