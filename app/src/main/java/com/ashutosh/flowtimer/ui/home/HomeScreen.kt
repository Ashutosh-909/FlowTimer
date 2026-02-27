package com.ashutosh.flowtimer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ashutosh.flowtimer.core.timer.TimerState
import com.ashutosh.flowtimer.ui.components.HourglassCard
import com.ashutosh.flowtimer.ui.components.HourglassVisualState
import com.ashutosh.flowtimer.ui.components.PixelDurationPicker
import com.ashutosh.flowtimer.ui.components.StarField
import com.ashutosh.flowtimer.ui.components.TimerReadout
import com.ashutosh.flowtimer.ui.theme.FlowTimerTheme
import com.ashutosh.flowtimer.ui.theme.SpaceBackground

/**
 * Root composable for the home screen.
 *
 * Assembles the full layout: StarField background → "FLOW TIME" title →
 * HourglassCard (center) → TimerReadout (bottom).
 *
 * All interaction is gesture-driven via the [HourglassCard]:
 * - Tap → start / pause / resume
 * - Long-press → reset
 *
 * @param modifier Modifier to apply.
 * @param viewModel The [HomeViewModel] providing UI state and actions.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showDurationPicker by viewModel.showDurationPicker.collectAsState()

    HomeScreenContent(
        modifier = modifier,
        uiState = uiState,
        onTapHourglass = viewModel::onTapHourglass,
        onLongPressReset = viewModel::onLongPressReset,
        onTapSetDuration = viewModel::onTapSetDuration
    )

    if (showDurationPicker) {
        PixelDurationPicker(
            currentMinutes = uiState.durationMinutes,
            onConfirm = viewModel::onConfirmDuration,
            onDismiss = viewModel::onDismissDurationPicker
        )
    }
}

/**
 * Stateless content composable for the home screen, enabling previews
 * without a ViewModel.
 */
@Composable
internal fun HomeScreenContent(
    modifier: Modifier = Modifier,
    uiState: HomeViewModel.UiState = HomeViewModel.UiState(),
    onTapHourglass: () -> Unit = {},
    onLongPressReset: () -> Unit = {},
    onTapSetDuration: () -> Unit = {}
) {
    val visualState = when (uiState.timerState) {
        is TimerState.Idle -> HourglassVisualState.IDLE
        is TimerState.Running -> HourglassVisualState.RUNNING
        is TimerState.Paused -> HourglassVisualState.PAUSED
        is TimerState.Finished -> HourglassVisualState.FINISHED
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBackground)
    ) {
        // Layer 1: Animated starfield background
        StarField()

        // Layer 2: Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Top: Title ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "FLOW",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "TIME",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }

            // ── Center: Hourglass card ──
            HourglassCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                visualState = visualState,
                sandProgress = uiState.sandProgress,
                onTapHourglass = onTapHourglass,
                onLongPressReset = onLongPressReset,
                accessibilityStateDescription = uiState.accessibilityStateDescription
            )

            // ── Bottom: Timer readout ──
            TimerReadout(
                modifier = Modifier.padding(bottom = 48.dp),
                formattedTime = uiState.formattedTime,
                isIdle = uiState.isIdle,
                onTapSetDuration = onTapSetDuration,
                accessibilityTimeDescription = uiState.accessibilityTimeDescription
            )
        }
    }
}

// ── Previews (one per visual state) ──────────────────────────────────────

@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "HomeScreen – Idle"
)
@Composable
private fun HomeScreenIdlePreview() {
    FlowTimerTheme {
        HomeScreenContent(
            uiState = HomeViewModel.UiState(
                timerState = TimerState.Idle,
                displayMillis = 25 * 60_000L,
                durationMinutes = 25,
                sandProgress = 0f
            )
        )
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "HomeScreen – Running"
)
@Composable
private fun HomeScreenRunningPreview() {
    FlowTimerTheme {
        HomeScreenContent(
            uiState = HomeViewModel.UiState(
                timerState = TimerState.Running,
                displayMillis = 18 * 60_000L + 30_000L,
                durationMinutes = 25,
                sandProgress = 0.26f
            )
        )
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "HomeScreen – Paused"
)
@Composable
private fun HomeScreenPausedPreview() {
    FlowTimerTheme {
        HomeScreenContent(
            uiState = HomeViewModel.UiState(
                timerState = TimerState.Paused,
                displayMillis = 12 * 60_000L,
                durationMinutes = 25,
                sandProgress = 0.52f
            )
        )
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "HomeScreen – Finished"
)
@Composable
private fun HomeScreenFinishedPreview() {
    FlowTimerTheme {
        HomeScreenContent(
            uiState = HomeViewModel.UiState(
                timerState = TimerState.Finished,
                displayMillis = 0L,
                durationMinutes = 25,
                sandProgress = 1f
            )
        )
    }
}
