package com.ashutosh.flowtimer.core.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Instrumented tests for [FlowSessionDao].
 *
 * Uses an in-memory Room database so tests are fast and isolated.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class FlowSessionDaoTest {

    private lateinit var database: FlowTimerDatabase
    private lateinit var dao: FlowSessionDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FlowTimerDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.flowSessionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Insert & Query ──────────────────────────────────────────────────

    @Test
    fun insert_thenQueryInRange_returnsSameSession() = runTest {
        val session = FlowSession(
            startEpochMillis = 1_709_000_000_000L,
            durationMinutes = 25,
            completedEpochMillis = 1_709_001_500_000L
        )
        dao.insert(session)

        val results = dao.sessionsInRange(
            startMillis = 1_709_000_000_000L,
            endMillis = 1_709_002_000_000L
        ).first()

        assertEquals(1, results.size)
        assertEquals(25, results[0].durationMinutes)
        assertEquals(1_709_000_000_000L, results[0].startEpochMillis)
        assertEquals(1_709_001_500_000L, results[0].completedEpochMillis)
    }

    @Test
    fun queryInRange_excludesSessionsOutsideRange() = runTest {
        val inside = FlowSession(
            startEpochMillis = 1_709_000_000_000L,
            durationMinutes = 25,
            completedEpochMillis = 1_709_001_500_000L
        )
        val outside = FlowSession(
            startEpochMillis = 1_700_000_000_000L,
            durationMinutes = 30,
            completedEpochMillis = 1_700_001_800_000L
        )
        dao.insert(inside)
        dao.insert(outside)

        val results = dao.sessionsInRange(
            startMillis = 1_709_000_000_000L,
            endMillis = 1_709_002_000_000L
        ).first()

        assertEquals(1, results.size)
        assertEquals(25, results[0].durationMinutes)
    }

    @Test
    fun sessionsInRange_orderedByCompletedEpochAsc() = runTest {
        val first = FlowSession(
            startEpochMillis = 1_709_000_000_000L,
            durationMinutes = 25,
            completedEpochMillis = 1_709_001_500_000L
        )
        val second = FlowSession(
            startEpochMillis = 1_709_002_000_000L,
            durationMinutes = 30,
            completedEpochMillis = 1_709_003_800_000L
        )
        // Insert in reverse order
        dao.insert(second)
        dao.insert(first)

        val results = dao.sessionsInRange(
            startMillis = 1_709_000_000_000L,
            endMillis = 1_709_004_000_000L
        ).first()

        assertEquals(2, results.size)
        assertTrue(results[0].completedEpochMillis < results[1].completedEpochMillis)
    }

    @Test
    fun emptyDatabase_returnsEmptyList() = runTest {
        val results = dao.sessionsInRange(
            startMillis = 0L,
            endMillis = Long.MAX_VALUE
        ).first()

        assertTrue(results.isEmpty())
    }

    // ── Daily Aggregation ───────────────────────────────────────────────

    @Test
    fun dailyAggregates_sumsDurationsOnSameDay() = runTest {
        // Two sessions completed on the same day
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        dao.insert(
            FlowSession(
                startEpochMillis = startOfDay,
                durationMinutes = 25,
                completedEpochMillis = startOfDay + 25 * 60_000L
            )
        )
        dao.insert(
            FlowSession(
                startEpochMillis = startOfDay + 30 * 60_000L,
                durationMinutes = 15,
                completedEpochMillis = startOfDay + 45 * 60_000L
            )
        )

        val aggregates = dao.dailyAggregates(
            startMillis = startOfDay,
            endMillis = startOfDay + 24 * 60 * 60_000L
        ).first()

        assertEquals(1, aggregates.size)
        assertEquals(40, aggregates[0].totalMinutes) // 25 + 15
        assertEquals(2, aggregates[0].sessionCount)
    }

    @Test
    fun dailyAggregates_separatesDifferentDays() = runTest {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val todayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val yesterdayStart = yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val rangeEnd = todayStart + 24 * 60 * 60_000L

        dao.insert(
            FlowSession(
                startEpochMillis = yesterdayStart,
                durationMinutes = 30,
                completedEpochMillis = yesterdayStart + 30 * 60_000L
            )
        )
        dao.insert(
            FlowSession(
                startEpochMillis = todayStart,
                durationMinutes = 25,
                completedEpochMillis = todayStart + 25 * 60_000L
            )
        )

        val aggregates = dao.dailyAggregates(
            startMillis = yesterdayStart,
            endMillis = rangeEnd
        ).first()

        assertEquals(2, aggregates.size)
        // Ordered by day ASC
        assertEquals(30, aggregates[0].totalMinutes)
        assertEquals(25, aggregates[1].totalMinutes)
    }

    @Test
    fun dailyAggregates_emptyDatabase_returnsEmptyList() = runTest {
        val aggregates = dao.dailyAggregates(
            startMillis = 0L,
            endMillis = Long.MAX_VALUE
        ).first()

        assertTrue(aggregates.isEmpty())
    }

    @Test
    fun autoGeneratedId_incrementsAcrossInserts() = runTest {
        val session1 = FlowSession(
            startEpochMillis = 1_709_000_000_000L,
            durationMinutes = 25,
            completedEpochMillis = 1_709_001_500_000L
        )
        val session2 = FlowSession(
            startEpochMillis = 1_709_002_000_000L,
            durationMinutes = 30,
            completedEpochMillis = 1_709_003_800_000L
        )
        dao.insert(session1)
        dao.insert(session2)

        val results = dao.sessionsInRange(
            startMillis = 1_709_000_000_000L,
            endMillis = 1_709_004_000_000L
        ).first()

        assertEquals(2, results.size)
        assertTrue(results[0].id != results[1].id)
        assertTrue(results[0].id > 0)
        assertTrue(results[1].id > 0)
    }
}
