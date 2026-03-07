package com.ashutosh.flowtimer.core.data

import com.ashutosh.flowtimer.core.data.db.DailyAggregate
import com.ashutosh.flowtimer.core.data.db.FlowSession
import com.ashutosh.flowtimer.core.data.db.FlowSessionDao
import kotlinx.coroutines.flow.Flow

/**
 * Repository wrapping [FlowSessionDao] for session history operations.
 *
 * Provides a clean API for recording completed sessions and querying
 * aggregated data for the dashboard charts and heatmap.
 */
class SessionRepository(private val dao: FlowSessionDao) {

    /** Record a completed flow session in the Room database. */
    suspend fun recordSession(
        startEpoch: Long,
        durationMinutes: Int,
        completedEpoch: Long
    ) {
        dao.insert(
            FlowSession(
                startEpochMillis = startEpoch,
                durationMinutes = durationMinutes,
                completedEpochMillis = completedEpoch
            )
        )
    }

    /**
     * Observable daily aggregates (sum of minutes + count) within a date range.
     * Used by the dashboard bar chart and total focus text.
     */
    fun dailyAggregates(startMillis: Long, endMillis: Long): Flow<List<DailyAggregate>> =
        dao.dailyAggregates(startMillis, endMillis)

    /**
     * Observable list of individual sessions within a date range.
     * Ordered oldest-first.
     */
    fun sessionsInRange(startMillis: Long, endMillis: Long): Flow<List<FlowSession>> =
        dao.sessionsInRange(startMillis, endMillis)
}
