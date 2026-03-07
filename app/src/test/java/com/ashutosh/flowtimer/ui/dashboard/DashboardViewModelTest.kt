package com.ashutosh.flowtimer.ui.dashboard

import com.ashutosh.flowtimer.core.data.db.DailyAggregate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Unit tests for [DashboardViewModel] companion helpers:
 * date range computation, bar entry aggregation, and heatmap day mapping.
 *
 * These are pure JVM tests — no Android context required.
 */
class DashboardViewModelTest {

    // ── dateRangeLocalFor ────────────────────────────────────────────────

    @Test
    fun `WEEK range starts on Monday and spans 7 days`() {
        val (start, end) = DashboardViewModel.dateRangeLocalFor(
            DashboardViewModel.Timeframe.WEEK
        )
        assertEquals(DayOfWeek.MONDAY, start.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, end.dayOfWeek)
        assertEquals(6, java.time.temporal.ChronoUnit.DAYS.between(start, end))
    }

    @Test
    fun `MONTH range starts on 1st and ends on last day of month`() {
        val (start, end) = DashboardViewModel.dateRangeLocalFor(
            DashboardViewModel.Timeframe.MONTH
        )
        val today = LocalDate.now()
        assertEquals(1, start.dayOfMonth)
        assertEquals(today.lengthOfMonth(), end.dayOfMonth)
        assertEquals(today.month, start.month)
        assertEquals(today.month, end.month)
        assertEquals(today.year, start.year)
    }

    @Test
    fun `YEAR range starts on Jan 1 and ends on Dec 31`() {
        val (start, end) = DashboardViewModel.dateRangeLocalFor(
            DashboardViewModel.Timeframe.YEAR
        )
        val today = LocalDate.now()
        assertEquals(1, start.monthValue)
        assertEquals(1, start.dayOfMonth)
        assertEquals(12, end.monthValue)
        assertEquals(today.lengthOfYear(), end.dayOfYear)
    }

    // ── aggregatesToBarEntries – WEEK ────────────────────────────────────

    @Test
    fun `WEEK produces 7 bar entries Mon–Sun`() {
        val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)

        val entries = DashboardViewModel.aggregatesToBarEntries(
            DashboardViewModel.Timeframe.WEEK,
            emptyList(),
            monday,
            sunday
        )

        assertEquals(7, entries.size)
    }

    @Test
    fun `WEEK bar entries map aggregate minutes to correct day`() {
        val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)
        val wednesday = monday.plusDays(2)

        val aggregates = listOf(
            DailyAggregate(day = wednesday.toString(), totalMinutes = 45, sessionCount = 2)
        )

        val entries = DashboardViewModel.aggregatesToBarEntries(
            DashboardViewModel.Timeframe.WEEK,
            aggregates,
            monday,
            sunday
        )

        // Wednesday is index 2 (Mon=0, Tue=1, Wed=2)
        assertEquals(45, entries[2].minutes)
        // Other days should be 0
        assertEquals(0, entries[0].minutes)
        assertEquals(0, entries[1].minutes)
        assertEquals(0, entries[3].minutes)
    }

    @Test
    fun `WEEK bar entries with empty aggregates are all zero`() {
        val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)

        val entries = DashboardViewModel.aggregatesToBarEntries(
            DashboardViewModel.Timeframe.WEEK,
            emptyList(),
            monday,
            sunday
        )

        assertTrue(entries.all { it.minutes == 0 })
    }

    // ── aggregatesToBarEntries – MONTH ───────────────────────────────────

    @Test
    fun `MONTH produces correct number of week buckets`() {
        val today = LocalDate.now()
        val first = today.withDayOfMonth(1)
        val last = today.withDayOfMonth(today.lengthOfMonth())
        val expectedWeeks = ((last.dayOfMonth - 1) / 7) + 1

        val entries = DashboardViewModel.aggregatesToBarEntries(
            DashboardViewModel.Timeframe.MONTH,
            emptyList(),
            first,
            last
        )

        assertEquals(expectedWeeks, entries.size)
        // Labels should be W1, W2, ...
        entries.forEachIndexed { index, entry ->
            assertEquals("W${index + 1}", entry.label)
        }
    }

    @Test
    fun `MONTH aggregates minutes to correct week bucket`() {
        val today = LocalDate.now()
        val first = today.withDayOfMonth(1)
        val last = today.withDayOfMonth(today.lengthOfMonth())

        // Day 1 → W1 (index 0), Day 8 → W2 (index 1)
        val aggregates = listOf(
            DailyAggregate(day = first.toString(), totalMinutes = 30, sessionCount = 1),
            DailyAggregate(day = first.plusDays(7).toString(), totalMinutes = 60, sessionCount = 2)
        )

        val entries = DashboardViewModel.aggregatesToBarEntries(
            DashboardViewModel.Timeframe.MONTH,
            aggregates,
            first,
            last
        )

        assertEquals(30, entries[0].minutes)
        assertEquals(60, entries[1].minutes)
    }

    // ── aggregatesToBarEntries – YEAR ────────────────────────────────────

    @Test
    fun `YEAR produces 12 bar entries Jan–Dec`() {
        val today = LocalDate.now()
        val first = today.withDayOfYear(1)
        val last = today.withDayOfYear(today.lengthOfYear())

        val entries = DashboardViewModel.aggregatesToBarEntries(
            DashboardViewModel.Timeframe.YEAR,
            emptyList(),
            first,
            last
        )

        assertEquals(12, entries.size)
    }

    @Test
    fun `YEAR accumulates minutes in correct month`() {
        val today = LocalDate.now()
        val first = today.withDayOfYear(1)
        val last = today.withDayOfYear(today.lengthOfYear())

        // March data
        val marchDays = listOf(
            DailyAggregate(day = "${today.year}-03-01", totalMinutes = 25, sessionCount = 1),
            DailyAggregate(day = "${today.year}-03-15", totalMinutes = 50, sessionCount = 2)
        )

        val entries = DashboardViewModel.aggregatesToBarEntries(
            DashboardViewModel.Timeframe.YEAR,
            marchDays,
            first,
            last
        )

        // March is index 2 (0-based)
        assertEquals(75, entries[2].minutes)
        // Jan and Feb should be 0
        assertEquals(0, entries[0].minutes)
        assertEquals(0, entries[1].minutes)
    }

    // ── aggregatesToHeatmapDays ─────────────────────────────────────────

    @Test
    fun `heatmap covers full date range inclusive`() {
        val start = LocalDate.of(2026, 1, 1)
        val end = LocalDate.of(2026, 1, 10)

        val days = DashboardViewModel.aggregatesToHeatmapDays(
            emptyList(),
            start,
            end
        )

        assertEquals(10, days.size)
        assertEquals(start, days.first().date)
        assertEquals(end, days.last().date)
    }

    @Test
    fun `heatmap maps aggregate minutes to correct dates`() {
        val start = LocalDate.of(2026, 1, 1)
        val end = LocalDate.of(2026, 1, 5)

        val aggregates = listOf(
            DailyAggregate(day = "2026-01-03", totalMinutes = 90, sessionCount = 3)
        )

        val days = DashboardViewModel.aggregatesToHeatmapDays(
            aggregates,
            start,
            end
        )

        assertEquals(5, days.size)
        assertEquals(0, days[0].totalMinutes) // Jan 1
        assertEquals(0, days[1].totalMinutes) // Jan 2
        assertEquals(90, days[2].totalMinutes) // Jan 3
        assertEquals(0, days[3].totalMinutes) // Jan 4
        assertEquals(0, days[4].totalMinutes) // Jan 5
    }

    @Test
    fun `heatmap with no aggregates has all zeros`() {
        val start = LocalDate.of(2026, 6, 1)
        val end = LocalDate.of(2026, 6, 30)

        val days = DashboardViewModel.aggregatesToHeatmapDays(
            emptyList(),
            start,
            end
        )

        assertEquals(30, days.size)
        assertTrue(days.all { it.totalMinutes == 0 })
    }

    @Test
    fun `heatmap with single day range`() {
        val date = LocalDate.of(2026, 3, 7)

        val aggregates = listOf(
            DailyAggregate(day = "2026-03-07", totalMinutes = 25, sessionCount = 1)
        )

        val days = DashboardViewModel.aggregatesToHeatmapDays(
            aggregates,
            date,
            date
        )

        assertEquals(1, days.size)
        assertEquals(25, days[0].totalMinutes)
    }

    // ── UiState.formattedTotalTime ──────────────────────────────────────

    @Test
    fun `formattedTotalTime with hours and minutes`() {
        val state = DashboardViewModel.UiState(totalFocusMinutes = 155)
        assertEquals("2 h 35 min", state.formattedTotalTime)
    }

    @Test
    fun `formattedTotalTime with hours only`() {
        val state = DashboardViewModel.UiState(totalFocusMinutes = 120)
        assertEquals("2 h", state.formattedTotalTime)
    }

    @Test
    fun `formattedTotalTime with minutes only`() {
        val state = DashboardViewModel.UiState(totalFocusMinutes = 45)
        assertEquals("45 min", state.formattedTotalTime)
    }

    @Test
    fun `formattedTotalTime with zero`() {
        val state = DashboardViewModel.UiState(totalFocusMinutes = 0)
        assertEquals("0 min", state.formattedTotalTime)
    }
}
