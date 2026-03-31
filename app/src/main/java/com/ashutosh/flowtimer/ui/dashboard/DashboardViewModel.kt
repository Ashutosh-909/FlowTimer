package com.ashutosh.flowtimer.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ashutosh.flowtimer.core.data.SessionRepository
import com.ashutosh.flowtimer.core.data.db.DailyAggregate
import com.ashutosh.flowtimer.core.data.db.FlowTimerDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * ViewModel for the dashboard screen. Queries [SessionRepository] for
 * session history and aggregates data by timeframe (Week / Month / Year).
 * Also provides 6-month heatmap data independent of the selected timeframe.
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    /** Time grouping for the bar chart. */
    enum class Timeframe { WEEK, MONTH, YEAR }

    /** Single bar in the chart: label + total focus minutes. */
    data class BarEntry(val label: String, val minutes: Int)

    /** One day in the calendar heatmap. */
    data class HeatmapDay(val date: LocalDate, val totalMinutes: Int)

    /** Immutable UI state for the dashboard screen. */
    data class UiState(
        val timeframe: Timeframe = Timeframe.WEEK,
        val totalFocusMinutes: Int = 0,
        val barData: List<BarEntry> = emptyList(),
        val heatmapData: List<HeatmapDay> = emptyList(),
        val isLoading: Boolean = true
    ) {
        /** Formatted total focus time string (e.g., "2 h 35 min" or "45 min"). */
        val formattedTotalTime: String
            get() {
                val hours = totalFocusMinutes / 60
                val minutes = totalFocusMinutes % 60
                return when {
                    hours > 0 && minutes > 0 -> "$hours h $minutes min"
                    hours > 0 -> "$hours h"
                    else -> "$minutes min"
                }
            }
    }

    private val sessionRepository: SessionRepository
    private val _timeframe = MutableStateFlow(Timeframe.WEEK)

    init {
        val db = FlowTimerDatabase.getInstance(application)
        sessionRepository = SessionRepository(db.flowSessionDao())
    }

    /** Combined UI state stream. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState> = combine(
        _timeframe.flatMapLatest { tf ->
            val (startDate, endDate) = dateRangeLocalFor(tf)
            val zone = ZoneId.systemDefault()
            val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            sessionRepository.dailyAggregates(startMillis, endMillis).map { aggregates ->
                Triple(tf, aggregates, startDate to endDate)
            }
        },
        heatmapFlow()
    ) { barTriple, heatmapDays ->
        val (tf, aggregates, dateRange) = barTriple
        val barData = aggregatesToBarEntries(tf, aggregates, dateRange.first, dateRange.second)
        UiState(
            timeframe = tf,
            totalFocusMinutes = aggregates.sumOf { it.totalMinutes },
            barData = barData,
            heatmapData = heatmapDays,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = UiState()
    )

    /** Switch the active timeframe, triggering a bar chart reload. */
    fun onTimeframeSelected(timeframe: Timeframe) {
        _timeframe.value = timeframe
    }

    // ── Private helpers ────────────────────────────────────────────────

    private fun heatmapFlow(): Flow<List<HeatmapDay>> {
        val today = LocalDate.now()
        val sixMonthsAgo = today.minusMonths(6)
        val zone = ZoneId.systemDefault()
        val startMillis = sixMonthsAgo.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return sessionRepository.dailyAggregates(startMillis, endMillis).map { aggregates ->
            aggregatesToHeatmapDays(aggregates, sixMonthsAgo, today)
        }
    }

    // ── Static helpers (testable) ──────────────────────────────────────

    internal companion object {

        /** Compute the [LocalDate] range for a given [Timeframe]. */
        fun dateRangeLocalFor(timeframe: Timeframe): Pair<LocalDate, LocalDate> {
            val today = LocalDate.now()
            return when (timeframe) {
                Timeframe.WEEK -> {
                    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    monday to monday.plusDays(6)
                }
                Timeframe.MONTH -> {
                    val first = today.withDayOfMonth(1)
                    first to today.withDayOfMonth(today.lengthOfMonth())
                }
                Timeframe.YEAR -> {
                    val first = today.withDayOfYear(1)
                    first to today.withDayOfYear(today.lengthOfYear())
                }
            }
        }

        /** Convert daily aggregates into [BarEntry] list based on [Timeframe]. */
        fun aggregatesToBarEntries(
            timeframe: Timeframe,
            aggregates: List<DailyAggregate>,
            startDate: LocalDate,
            endDate: LocalDate
        ): List<BarEntry> {
            val aggregateMap = aggregates.associate {
                LocalDate.parse(it.day) to it.totalMinutes
            }
            return when (timeframe) {
                Timeframe.WEEK -> {
                    val monday = startDate
                    (0..6).map { index ->
                        val dow = DayOfWeek.of(index + 1)
                        val day = monday.plusDays(index.toLong())
                        BarEntry(
                            label = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                .uppercase(),
                            minutes = aggregateMap[day] ?: 0
                        )
                    }
                }
                Timeframe.MONTH -> {
                    val weeks = mutableMapOf<Int, Int>()
                    var day = startDate
                    while (!day.isAfter(endDate)) {
                        val weekIndex = (day.dayOfMonth - 1) / 7
                        weeks[weekIndex] =
                            (weeks[weekIndex] ?: 0) + (aggregateMap[day] ?: 0)
                        day = day.plusDays(1)
                    }
                    val weekCount = ((endDate.dayOfMonth - 1) / 7) + 1
                    (0 until weekCount).map { w ->
                        BarEntry("W${w + 1}", weeks[w] ?: 0)
                    }
                }
                Timeframe.YEAR -> {
                    (1..12).map { month ->
                        val monthStart = LocalDate.of(startDate.year, month, 1)
                        val monthEnd =
                            monthStart.withDayOfMonth(monthStart.lengthOfMonth())
                        var total = 0
                        var d = monthStart
                        while (!d.isAfter(monthEnd)) {
                            total += aggregateMap[d] ?: 0
                            d = d.plusDays(1)
                        }
                        BarEntry(
                            label = monthStart.month.getDisplayName(
                                TextStyle.SHORT,
                                Locale.getDefault()
                            ).uppercase(),
                            minutes = total
                        )
                    }
                }
            }
        }

        /** Convert daily aggregates into a flat list of [HeatmapDay] for the given range. */
        fun aggregatesToHeatmapDays(
            aggregates: List<DailyAggregate>,
            startDate: LocalDate,
            endDate: LocalDate
        ): List<HeatmapDay> {
            val aggregateMap = aggregates.associate {
                LocalDate.parse(it.day) to it.totalMinutes
            }
            val days = mutableListOf<HeatmapDay>()
            var day = startDate
            while (!day.isAfter(endDate)) {
                days.add(HeatmapDay(day, aggregateMap[day] ?: 0))
                day = day.plusDays(1)
            }
            return days
        }
    }
}
