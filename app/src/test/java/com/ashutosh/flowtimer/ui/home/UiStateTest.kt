package com.ashutosh.flowtimer.ui.home

import com.ashutosh.flowtimer.core.timer.TimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [HomeViewModel.UiState] computed properties.
 */
class UiStateTest {

    // ── formattedTime ──

    @Test
    fun `formattedTime for 25 minutes shows 25 colon 00`() {
        val state = HomeViewModel.UiState(displayMillis = 25 * 60_000L)
        assertEquals("25:00", state.formattedTime)
    }

    @Test
    fun `formattedTime for 0 millis shows 00 colon 00`() {
        val state = HomeViewModel.UiState(displayMillis = 0L)
        assertEquals("00:00", state.formattedTime)
    }

    @Test
    fun `formattedTime for 90 seconds shows 01 colon 30`() {
        val state = HomeViewModel.UiState(displayMillis = 90_000L)
        assertEquals("01:30", state.formattedTime)
    }

    @Test
    fun `formattedTime for 120 minutes shows 120 colon 00`() {
        val state = HomeViewModel.UiState(displayMillis = 120 * 60_000L)
        assertEquals("120:00", state.formattedTime)
    }

    @Test
    fun `formattedTime truncates sub-second precision`() {
        // 61,999 ms = 1 min 1.999 seconds → should show 01:01
        val state = HomeViewModel.UiState(displayMillis = 61_999L)
        assertEquals("01:01", state.formattedTime)
    }

    // ── accessibilityTimeDescription ──

    @Test
    fun `accessibilityTimeDescription for 25 minutes idle`() {
        val state = HomeViewModel.UiState(
            timerState = TimerState.Idle,
            displayMillis = 25 * 60_000L
        )
        assertEquals("25 minutes", state.accessibilityTimeDescription)
    }

    @Test
    fun `accessibilityTimeDescription for 1 minute 30 seconds`() {
        val state = HomeViewModel.UiState(
            timerState = TimerState.Running,
            displayMillis = 90_000L
        )
        assertEquals("1 minute, 30 seconds", state.accessibilityTimeDescription)
    }

    @Test
    fun `accessibilityTimeDescription for 0 seconds shows 0 seconds`() {
        val state = HomeViewModel.UiState(
            timerState = TimerState.Idle,
            displayMillis = 0L
        )
        assertEquals("0 seconds", state.accessibilityTimeDescription)
    }

    @Test
    fun `accessibilityTimeDescription for finished state appends complete`() {
        val state = HomeViewModel.UiState(
            timerState = TimerState.Finished,
            displayMillis = 0L
        )
        assertTrue(state.accessibilityTimeDescription.contains("Timer complete."))
    }

    @Test
    fun `accessibilityTimeDescription singular minute`() {
        val state = HomeViewModel.UiState(
            timerState = TimerState.Running,
            displayMillis = 60_000L
        )
        assertEquals("1 minute", state.accessibilityTimeDescription)
    }

    @Test
    fun `accessibilityTimeDescription singular second`() {
        val state = HomeViewModel.UiState(
            timerState = TimerState.Running,
            displayMillis = 1_000L
        )
        assertEquals("1 second", state.accessibilityTimeDescription)
    }

    // ── accessibilityStateDescription ──

    @Test
    fun `accessibilityStateDescription for Idle`() {
        val state = HomeViewModel.UiState(
            timerState = TimerState.Idle,
            displayMillis = 25 * 60_000L
        )
        assertEquals("Idle, 25:00", state.accessibilityStateDescription)
    }

    @Test
    fun `accessibilityStateDescription for Running`() {
        val state = HomeViewModel.UiState(
            timerState = TimerState.Running,
            displayMillis = 90_000L
        )
        assertEquals("Running, 1 minute, 30 seconds remaining", state.accessibilityStateDescription)
    }

    @Test
    fun `accessibilityStateDescription for Finished`() {
        val state = HomeViewModel.UiState(
            timerState = TimerState.Finished,
            displayMillis = 0L
        )
        assertEquals("Complete", state.accessibilityStateDescription)
    }

    // ── isIdle ──

    @Test
    fun `isIdle returns true for Idle state`() {
        val state = HomeViewModel.UiState(timerState = TimerState.Idle)
        assertTrue(state.isIdle)
    }

    @Test
    fun `isIdle returns false for Running state`() {
        val state = HomeViewModel.UiState(timerState = TimerState.Running)
        assertFalse(state.isIdle)
    }

    @Test
    fun `isIdle returns false for Finished state`() {
        val state = HomeViewModel.UiState(timerState = TimerState.Finished)
        assertFalse(state.isIdle)
    }

    // ── Default values ──

    @Test
    fun `default UiState has correct values`() {
        val state = HomeViewModel.UiState()
        assertEquals(TimerState.Idle, state.timerState)
        assertEquals(25 * 60_000L, state.displayMillis)
        assertEquals(25, state.durationMinutes)
        assertEquals(0f, state.sandProgress, 0.001f)
        assertTrue(state.isIdle)
    }
}
