package com.ashutosh.flowtimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ashutosh.flowtimer.ui.theme.FlowTimerTheme
import com.ashutosh.flowtimer.ui.theme.PixelTextDim
import com.ashutosh.flowtimer.ui.theme.SpaceBackground
import com.ashutosh.flowtimer.ui.theme.TimerPillBorder
import com.ashutosh.flowtimer.ui.theme.TimerPillFill

/**
 * Timer readout pill showing `MM:SS` in pixel font.
 *
 * Displays a bordered rounded-rect pill with the formatted remaining time.
 * When the timer is idle, shows a "Set your focus time" hint below that
 * opens the duration picker on tap.
 *
 * @param formattedTime The time string to display (e.g., "25:00").
 * @param isIdle Whether the timer is in idle state (enables tap-to-set and shows hint).
 * @param onTapSetDuration Callback when user taps to set duration. Only active when [isIdle].
 * @param accessibilityTimeDescription Readable description for TalkBack (e.g., "25 minutes, 0 seconds").
 * @param modifier Modifier to apply.
 */
@Composable
fun TimerReadout(
    modifier: Modifier = Modifier,
    formattedTime: String = "25:00",
    isIdle: Boolean = true,
    onTapSetDuration: () -> Unit = {},
    accessibilityTimeDescription: String = "25 minutes, 0 seconds"
) {
    val pillShape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Timer pill
        Box(
            modifier = Modifier
                .semantics(mergeDescendants = true) {
                    contentDescription = accessibilityTimeDescription
                    liveRegion = LiveRegionMode.Polite
                }
                .background(
                    color = TimerPillFill,
                    shape = pillShape
                )
                .border(
                    width = 1.dp,
                    color = TimerPillBorder,
                    shape = pillShape
                )
                .padding(horizontal = 32.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }

        // "Set your focus time" hint — only when idle
        if (isIdle) {
            Text(
                text = "Set your focus time",
                style = MaterialTheme.typography.bodyMedium,
                color = PixelTextDim,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .semantics {
                        role = Role.Button
                        contentDescription = "Set your focus time"
                    }
                    .clickable(onClick = onTapSetDuration)
                    .padding(8.dp)
            )
        }
    }
}

// ── Previews ──

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D1B2A,
    widthDp = 360,
    name = "TimerReadout – Idle (25:00)"
)
@Composable
private fun TimerReadoutIdlePreview() {
    FlowTimerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceBackground),
            contentAlignment = Alignment.Center
        ) {
            TimerReadout(
                formattedTime = "25:00",
                isIdle = true
            )
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D1B2A,
    widthDp = 360,
    name = "TimerReadout – Running (24:30)"
)
@Composable
private fun TimerReadoutRunningPreview() {
    FlowTimerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceBackground),
            contentAlignment = Alignment.Center
        ) {
            TimerReadout(
                formattedTime = "24:30",
                isIdle = false,
                accessibilityTimeDescription = "24 minutes, 30 seconds"
            )
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D1B2A,
    widthDp = 360,
    name = "TimerReadout – Complete (00:00)"
)
@Composable
private fun TimerReadoutCompletePreview() {
    FlowTimerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceBackground),
            contentAlignment = Alignment.Center
        ) {
            TimerReadout(
                formattedTime = "00:00",
                isIdle = false,
                accessibilityTimeDescription = "0 minutes, 0 seconds. Timer complete."
            )
        }
    }
}
