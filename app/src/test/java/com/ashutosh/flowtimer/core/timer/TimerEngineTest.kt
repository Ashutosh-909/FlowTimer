package com.ashutosh.flowtimer.core.timer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerEngineTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    /** Simulated monotonic clock that advances in lockstep with test time. */
    private var fakeElapsedRealtime = 0L
    private val clockProvider: () -> Long = { fakeElapsedRealtime }

    private lateinit var engine: TimerEngine

    @Before
    fun setup() {
        fakeElapsedRealtime = 10_000L // arbitrary start
        engine = TimerEngine(
            scope = testScope.backgroundScope, // backgroundScope tolerates uncompleted coroutines
            elapsedRealtimeProvider = clockProvider
        )
    }

    @After
    fun tearDown() {
        engine.cancel()
    }

    // ── Initial State ──

    @Test
    fun `initial state is Idle`() {
        assertEquals(TimerState.Idle, engine.state.value)
    }

    @Test
    fun `initial remaining millis is 0`() {
        assertEquals(0L, engine.remainingMillis.value)
    }

    // ── Start ──

    @Test
    fun `start transitions to Running`() = testScope.runTest {
        engine.start(25)
        assertEquals(TimerState.Running, engine.state.value)
    }

    @Test
    fun `start sets remaining millis to duration`() = testScope.runTest {
        engine.start(25)
        assertEquals(25 * 60_000L, engine.remainingMillis.value)
    }

    @Test
    fun `start is no-op when already Running`() = testScope.runTest {
        engine.start(25)
        fakeElapsedRealtime += 5_000L
        advanceTimeBy(5_000L)

        val remainingBefore = engine.remainingMillis.value

        engine.start(10) // should be ignored
        assertEquals(TimerState.Running, engine.state.value)
        // remaining should not have jumped to 10 min
        assertTrue(engine.remainingMillis.value <= remainingBefore)
    }

    // ── Ticking ──

    @Test
    fun `timer ticks down each second`() = testScope.runTest {
        engine.start(1) // 1 minute = 60,000 ms

        // Advance 3 seconds
        fakeElapsedRealtime += 3_000L
        advanceTimeBy(3_000L)

        val remaining = engine.remainingMillis.value
        // Should be around 57,000 ms (60000 - 3000)
        assertTrue("Expected ~57000, got $remaining", remaining in 56_000L..58_000L)
    }

    @Test
    fun `timer finishes after full duration`() = testScope.runTest {
        engine.start(1) // 60 seconds

        fakeElapsedRealtime += 60_000L
        advanceTimeBy(60_000L)

        advanceUntilIdle()
        assertEquals(TimerState.Finished, engine.state.value)
        assertEquals(0L, engine.remainingMillis.value)
    }

    @Test
    fun `remaining never goes negative`() = testScope.runTest {
        engine.start(1) // 60 seconds

        fakeElapsedRealtime += 90_000L // overshoot
        advanceTimeBy(90_000L)

        advanceUntilIdle()
        assertEquals(0L, engine.remainingMillis.value)
    }

    // ── Reset ──

    @Test
    fun `reset from Running returns to Idle`() = testScope.runTest {
        engine.start(25)
        fakeElapsedRealtime += 5_000L
        advanceTimeBy(5_000L)

        engine.reset(25)
        assertEquals(TimerState.Idle, engine.state.value)
        assertEquals(25 * 60_000L, engine.remainingMillis.value)
    }

    @Test
    fun `reset from Finished returns to Idle`() = testScope.runTest {
        engine.start(1)
        fakeElapsedRealtime += 60_000L
        advanceTimeBy(60_000L)
        advanceUntilIdle()

        assertEquals(TimerState.Finished, engine.state.value)

        engine.reset(25)
        assertEquals(TimerState.Idle, engine.state.value)
        assertEquals(25 * 60_000L, engine.remainingMillis.value)
    }

    @Test
    fun `reset with different duration updates remaining`() = testScope.runTest {
        engine.start(25)
        engine.reset(45)
        assertEquals(45 * 60_000L, engine.remainingMillis.value)
    }

    // ── TimerState serialization ──

    @Test
    fun `TimerState name serialization roundtrips`() {
        val states = listOf(TimerState.Idle, TimerState.Running, TimerState.Finished)
        states.forEach { state ->
            assertEquals(state, TimerState.fromName(state.name))
        }
    }

    @Test
    fun `TimerState fromName with unknown string returns Idle`() {
        assertEquals(TimerState.Idle, TimerState.fromName("UNKNOWN"))
    }

    // ── Restore ──

    @Test
    fun `restore to Running starts tick loop`() = testScope.runTest {
        engine.restore(
            timerState = TimerState.Running,
            remainingMs = 30_000L,
            totalDurationMs = 60_000L,
            lastStartEpoch = 0L
        )
        assertEquals(TimerState.Running, engine.state.value)
        assertEquals(30_000L, engine.remainingMillis.value)

        // Advance 5s and check it ticks
        fakeElapsedRealtime += 5_000L
        advanceTimeBy(5_000L)

        val remaining = engine.remainingMillis.value
        assertTrue("Expected ~25000, got $remaining", remaining in 24_000L..26_000L)
    }

    @Test
    fun `restore to Idle sets state and remaining`() = testScope.runTest {
        engine.restore(
            timerState = TimerState.Idle,
            remainingMs = 60_000L,
            totalDurationMs = 60_000L,
            lastStartEpoch = 0L
        )
        assertEquals(TimerState.Idle, engine.state.value)
        assertEquals(60_000L, engine.remainingMillis.value)
    }
}
