package com.ashutosh.flowtimer.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single completed flow session.
 *
 * Stored in Room so the dashboard can query by date range,
 * aggregate by day, and render charts/heatmaps.
 */
@Entity(tableName = "flow_sessions")
data class FlowSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [System.currentTimeMillis] when the session was started. */
    val startEpochMillis: Long,
    /** Configured duration for this session (minutes). */
    val durationMinutes: Int,
    /** [System.currentTimeMillis] when the session finished. */
    val completedEpochMillis: Long
)
