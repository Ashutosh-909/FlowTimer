package com.ashutosh.flowtimer.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ashutosh.flowtimer.model.TimerUiState
import com.ashutosh.flowtimer.timer.TimerState
import com.ashutosh.flowtimer.ui.components.HourglassCard
import com.ashutosh.flowtimer.ui.components.HourglassVisualState
import com.ashutosh.flowtimer.ui.components.PixelDurationPicker
import com.ashutosh.flowtimer.ui.components.StarField
import com.ashutosh.flowtimer.ui.components.TimerReadout
import com.ashutosh.flowtimer.ui.theme.CardSurface
import com.ashutosh.flowtimer.ui.theme.FlowTimerTheme
import com.ashutosh.flowtimer.ui.theme.GlowBlue
import com.ashutosh.flowtimer.ui.theme.GlowCyan
import com.ashutosh.flowtimer.ui.theme.PixelTextDim
import com.ashutosh.flowtimer.ui.theme.SpaceBackground
import com.ashutosh.flowtimer.ui.theme.TimerPillBorder

/**
 * Root composable for the home screen.
 *
 * Assembles the full layout: StarField background → "FLOW TIME" title →
 * HourglassCard (center) → TimerReadout (bottom).
 *
 * All interaction is gesture-driven via the [HourglassCard]:
 * - Tap → start or stop/reset
 * - Long-press → reset
 *
 * Handles `POST_NOTIFICATIONS` runtime permission (API 33+) before the
 * first timer start. Shows a rationale dialog if the system indicates
 * the user previously denied the permission.
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
    val context = LocalContext.current

    // ── Notification permission (API 33+) ────────────────────────────────

    var showNotificationRationale by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Whether granted or denied, proceed with the timer start.
        // The timer works without notifications — graceful degradation.
        viewModel.onTapHourglass()
    }

    /**
     * Wraps the hourglass tap: if the timer is idle and we need the notification
     * permission, request it first (with rationale when appropriate). Otherwise
     * delegates directly to the ViewModel.
     */
    val onTapHourglassWithPermission: () -> Unit = {
        if (uiState.timerState is TimerState.Idle && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            when {
                ContextCompat.checkSelfPermission(context, permission) ==
                        PackageManager.PERMISSION_GRANTED -> {
                    // Already granted
                    viewModel.onTapHourglass()
                }
                (context as? android.app.Activity)?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
                } == true -> {
                    // Show rationale dialog before requesting
                    showNotificationRationale = true
                }
                else -> {
                    // First ask or "don't ask again" — launch the system dialog
                    notificationPermissionLauncher.launch(permission)
                }
            }
        } else {
            // API < 33 or timer not idle — no permission needed
            viewModel.onTapHourglass()
        }
    }

    // ── Screen content ───────────────────────────────────────────────────

    HomeScreenContent(
        modifier = modifier,
        uiState = uiState,
        onTapHourglass = onTapHourglassWithPermission,
        onLongPressReset = viewModel::onLongPressReset,
        onTapSetDuration = viewModel::onTapSetDuration
    )

    // ── Duration picker dialog ───────────────────────────────────────────

    if (showDurationPicker) {
        PixelDurationPicker(
            currentMinutes = uiState.durationMinutes,
            onConfirm = viewModel::onConfirmDuration,
            onDismiss = viewModel::onDismissDurationPicker
        )
    }

    // ── Notification rationale dialog ────────────────────────────────────

    if (showNotificationRationale) {
        NotificationRationaleDialog(
            onConfirm = {
                showNotificationRationale = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDismiss = {
                showNotificationRationale = false
                // Start timer anyway — graceful degradation
                viewModel.onTapHourglass()
            }
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
    uiState: TimerUiState = TimerUiState(),
    onTapHourglass: () -> Unit = {},
    onLongPressReset: () -> Unit = {},
    onTapSetDuration: () -> Unit = {}
) {
    val visualState = when (uiState.timerState) {
        is TimerState.Idle -> HourglassVisualState.IDLE
        is TimerState.Running -> HourglassVisualState.RUNNING
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
            // ── Top: Title + session count ──
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
                if (uiState.completedSessionCount > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${uiState.completedSessionCount} flow session${if (uiState.completedSessionCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PixelTextDim,
                        textAlign = TextAlign.Center
                    )
                }
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

// ── Notification Rationale Dialog ────────────────────────────────────────

/**
 * Pixel-styled dialog explaining why notification permission is needed.
 *
 * Shown when `shouldShowRequestPermissionRationale` returns true (i.e. the
 * user previously denied the permission). Offers "ALLOW" to re-request or
 * "SKIP" to start the timer without notifications.
 *
 * @param onConfirm Called when the user taps "ALLOW" — triggers the system permission dialog.
 * @param onDismiss Called when the user taps "SKIP" or back-presses — timer starts anyway.
 */
@Composable
internal fun NotificationRationaleDialog(
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val containerShape = RoundedCornerShape(16.dp)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clip(containerShape)
                .background(CardSurface)
                .border(
                    width = 1.dp,
                    color = TimerPillBorder,
                    shape = containerShape
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NOTIFICATIONS",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Flow Time needs notification permission to show a timer countdown and alert you when your session is complete.",
                style = MaterialTheme.typography.bodyMedium,
                color = PixelTextDim,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                // Skip
                Box(
                    modifier = Modifier
                        .semantics {
                            role = Role.Button
                            contentDescription = "Skip notification permission"
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(SpaceBackground)
                        .border(
                            width = 1.dp,
                            color = TimerPillBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SKIP",
                        style = MaterialTheme.typography.labelMedium,
                        color = PixelTextDim
                    )
                }

                // Allow
                Box(
                    modifier = Modifier
                        .semantics {
                            role = Role.Button
                            contentDescription = "Allow notifications"
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlowBlue.copy(alpha = 0.3f))
                        .border(
                            width = 1.dp,
                            color = GlowCyan,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(onClick = onConfirm)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ALLOW",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
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
            uiState = TimerUiState(
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
            uiState = TimerUiState(
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
    name = "HomeScreen – Finished"
)
@Composable
private fun HomeScreenFinishedPreview() {
    FlowTimerTheme {
        HomeScreenContent(
            uiState = TimerUiState(
                timerState = TimerState.Finished,
                displayMillis = 0L,
                durationMinutes = 25,
                sandProgress = 1f
            )
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D1B2A,
    widthDp = 360,
    name = "Notification Rationale Dialog"
)
@Composable
private fun NotificationRationaleDialogPreview() {
    FlowTimerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceBackground),
            contentAlignment = Alignment.Center
        ) {
            // Direct content without Dialog wrapper for preview
            val containerShape = RoundedCornerShape(16.dp)
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .clip(containerShape)
                    .background(CardSurface)
                    .border(
                        width = 1.dp,
                        color = TimerPillBorder,
                        shape = containerShape
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "NOTIFICATIONS",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Flow Time needs notification permission to show a timer countdown and alert you when your session is complete.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PixelTextDim,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        12.dp,
                        Alignment.CenterHorizontally
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpaceBackground)
                            .border(1.dp, TimerPillBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SKIP",
                            style = MaterialTheme.typography.labelMedium,
                            color = PixelTextDim
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlowBlue.copy(alpha = 0.3f))
                            .border(1.dp, GlowCyan, RoundedCornerShape(12.dp))
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ALLOW",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}
