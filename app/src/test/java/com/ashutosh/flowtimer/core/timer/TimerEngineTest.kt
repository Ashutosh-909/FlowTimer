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

    // ── Pause / Resume ──

    @Test
    fun `pause transitions from Running to Paused`() = testScope.runTest {
        engine.start(25)
        fakeElapsedRealtime += 5_000L
        advanceTimeBy(5_000L)

        engine.pause()
        assertEquals(TimerState.Paused, engine.state.value)
    }

    @Test
    fun `pause preserves remaining time`() = testScope.runTest {
        engine.start(1) // 60,000 ms

        fakeElapsedRealtime += 10_000L
        advanceTimeBy(10_000L)

        engine.pause()
        val pausedRemaining = engine.remainingMillis.value

        // Time should not change while paused
        fakeElapsedRealtime += 30_000L
        advanceTimeBy(30_000L)

        assertEquals(pausedRemaining, engine.remainingMillis.value)
    }

    @Test
    fun `pause is no-op when not Running`() = testScope.runTest {
        engine.pause() // idle → should stay idle
        assertEquals(TimerState.Idle, engine.state.value)
    }

    @Test
    fun `resume transitions from Paused to Running`() = testScope.runTest {
        engine.start(25)
        fakeElapsedRealtime += 5_000L
        advanceTimeBy(5_000L)

        engine.pause()
        engine.resume()
        assertEquals(TimerState.Running, engine.state.value)
    }

    @Test
    fun `resume continues countdown from paused position`() = testScope.runTest {
        engine.start(1) // 60 seconds

        // Run 10 seconds
        fakeElapsedRealtime += 10_000L
        advanceTimeBy(10_000L)

        engine.pause()
        val pausedRemaining = engine.remainingMillis.value // ~50,000

        // Wait 20 seconds while paused (should not count)
        fakeElapsedRealtime += 20_000L
        advanceTimeBy(20_000L)

        engine.resume()

        // Run 5 more seconds after resume
        fakeElapsedRealtime += 5_000L
        advanceTimeBy(5_000L)

        val remaining = engine.remainingMillis.value
        // Should be ~45,000 (50,000 - 5,000), not affected by paused 20s
        assertTrue("Expected ~45000, got $remaining", remaining in 44_000L..46_000L)
    }

    @Test
    fun `resume is no-op when not Paused`() = testScope.runTest {
        engine.resume() // idle → should stay idle
        assertEquals(TimerState.Idle, engine.state.value)
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
    fun `reset from Paused returns to Idle`() = testScope.runTest {
        engine.start(25)
        engine.pause()
        engine.reset(30)
        assertEquals(TimerState.Idle, engine.state.value)
        assertEquals(30 * 60_000L, engine.remainingMillis.value)
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
        val states = listOf(TimerState.Idle, TimerState.Running, TimerState.Paused, TimerState.Finished)
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
    fun `restore to Paused does not tick`() = testScope.runTest {
        engine.restore(
            timerState = TimerState.Paused,
            remainingMs = 30_000L,
            totalDurationMs = 60_000L,
            lastStartEpoch = 0L
        )
        assertEquals(TimerState.Paused, engine.state.value)

        fakeElapsedRealtime += 10_000L
        advanceTimeBy(10_000L)

        assertEquals(30_000L, engine.remainingMillis.value) // unchanged
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

    // ── Multiple pause/resume cycles ──

    @Test
    fun `multiple pause resume cycles accumulate correctly`() = testScope.runTest {
        engine.start(1) // 60 seconds

        // Run 10s
        fakeElapsedRealtime += 10_000L
        advanceTimeBy(10_000L)
        engine.pause() // remaining ~50,000

        // Paused 5s (doesn't count)
        fakeElapsedRealtime += 5_000L
        advanceTimeBy(5_000L)
        engine.resume()

        // Run 10s more
        fakeElapsedRealtime += 10_000L
        advanceTimeBy(10_000L)
        engine.pause() // remaining ~40,000

        // Paused 100s (doesn't count)
        fakeElapsedRealtime += 100_000L
        advanceTimeBy(100_000L)
        engine.resume()

        // Run 10s more
        fakeElapsedRealtime += 10_000L
        advanceTimeBy(10_000L)

        val remaining = engine.remainingMillis.value
        // Total running time: 10+10+10=30s, so remaining ~30,000
        assertTrue("Expected ~30000, got $remaining", remaining in 29_000L..31_000L)
    }
}
