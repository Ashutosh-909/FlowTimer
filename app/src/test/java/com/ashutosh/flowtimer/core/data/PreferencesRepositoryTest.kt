package com.ashutosh.flowtimer.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: PreferencesRepository

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(testDispatcher + Job())
        ) {
            tmpFolder.newFile("test_prefs.preferences_pb")
        }
        repository = PreferencesRepository(dataStore)
    }

    // ── Flow Duration ──

    @Test
    fun `flowDurationMinutes returns default 25 when not set`() = testScope.runTest {
        val result = repository.flowDurationMinutes.first()
        assertEquals(PreferencesRepository.DEFAULT_FLOW_DURATION_MINUTES, result)
    }

    @Test
    fun `setFlowDurationMinutes writes and reads back`() = testScope.runTest {
        repository.setFlowDurationMinutes(45)
        val result = repository.flowDurationMinutes.first()
        assertEquals(45, result)
    }

    @Test
    fun `setFlowDurationMinutes overwrites previous value`() = testScope.runTest {
        repository.setFlowDurationMinutes(10)
        repository.setFlowDurationMinutes(60)
        val result = repository.flowDurationMinutes.first()
        assertEquals(60, result)
    }

    // ── Timer State ──

    @Test
    fun `timerState returns default IDLE when not set`() = testScope.runTest {
        val result = repository.timerState.first()
        assertEquals(PreferencesRepository.DEFAULT_TIMER_STATE, result)
    }

    @Test
    fun `setTimerState writes and reads back`() = testScope.runTest {
        repository.setTimerState("RUNNING")
        val result = repository.timerState.first()
        assertEquals("RUNNING", result)
    }

    @Test
    fun `setTimerState cycles through all states`() = testScope.runTest {
        val states = listOf("IDLE", "RUNNING", "PAUSED", "FINISHED")
        states.forEach { state ->
            repository.setTimerState(state)
            assertEquals(state, repository.timerState.first())
        }
    }

    // ── Remaining Millis ──

    @Test
    fun `remainingMillis returns default 0 when not set`() = testScope.runTest {
        val result = repository.remainingMillis.first()
        assertEquals(PreferencesRepository.DEFAULT_REMAINING_MILLIS, result)
    }

    @Test
    fun `setRemainingMillis writes and reads back`() = testScope.runTest {
        repository.setRemainingMillis(1_500_000L)
        val result = repository.remainingMillis.first()
        assertEquals(1_500_000L, result)
    }

    // ── Last Start Epoch ──

    @Test
    fun `lastStartEpoch returns default 0 when not set`() = testScope.runTest {
        val result = repository.lastStartEpoch.first()
        assertEquals(PreferencesRepository.DEFAULT_LAST_START_EPOCH, result)
    }

    @Test
    fun `setLastStartEpoch writes and reads back`() = testScope.runTest {
        val epoch = 1709000000000L
        repository.setLastStartEpoch(epoch)
        val result = repository.lastStartEpoch.first()
        assertEquals(epoch, result)
    }

    // ── Reset Timer State ──

    @Test
    fun `resetTimerState clears timer state but preserves duration`() = testScope.runTest {
        // Set all values
        repository.setFlowDurationMinutes(30)
        repository.setTimerState("RUNNING")
        repository.setRemainingMillis(500_000L)
        repository.setLastStartEpoch(1709000000000L)

        // Reset
        repository.resetTimerState()

        // Duration preserved
        assertEquals(30, repository.flowDurationMinutes.first())

        // Timer state reset to defaults
        assertEquals(PreferencesRepository.DEFAULT_TIMER_STATE, repository.timerState.first())
        assertEquals(PreferencesRepository.DEFAULT_REMAINING_MILLIS, repository.remainingMillis.first())
        assertEquals(PreferencesRepository.DEFAULT_LAST_START_EPOCH, repository.lastStartEpoch.first())
    }
}
