package com.ashutosh.flowtimer.core.data.db

/**
 * Result of a daily aggregation query — sum of focus minutes
 * and session count for a single calendar day.
 *
 * [day] is formatted as `"YYYY-MM-DD"` (e.g. `"2026-03-07"`).
 */
data class DailyAggregate(
    val day: String,
    val totalMinutes: Int,
    val sessionCount: Int
)
