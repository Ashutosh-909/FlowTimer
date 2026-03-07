package com.ashutosh.flowtimer.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data-access object for [FlowSession] records.
 *
 * All queries that return [Flow] are observable and re-emit
 * whenever the underlying table changes.
 */
@Dao
interface FlowSessionDao {

    @Insert
    suspend fun insert(session: FlowSession)

    /** All sessions in a date range, ordered oldest-first. */
    @Query(
        """
        SELECT * FROM flow_sessions
        WHERE completedEpochMillis BETWEEN :startMillis AND :endMillis
        ORDER BY completedEpochMillis ASC
        """
    )
    fun sessionsInRange(startMillis: Long, endMillis: Long): Flow<List<FlowSession>>

    /**
     * Daily aggregation: sum of [FlowSession.durationMinutes] grouped by
     * calendar date (device-local timezone via SQLite's `localtime` modifier).
     */
    @Query(
        """
        SELECT date(completedEpochMillis / 1000, 'unixepoch', 'localtime') AS day,
               SUM(durationMinutes) AS totalMinutes,
               COUNT(*) AS sessionCount
        FROM flow_sessions
        WHERE completedEpochMillis BETWEEN :startMillis AND :endMillis
        GROUP BY day
        ORDER BY day ASC
        """
    )
    fun dailyAggregates(startMillis: Long, endMillis: Long): Flow<List<DailyAggregate>>
}
