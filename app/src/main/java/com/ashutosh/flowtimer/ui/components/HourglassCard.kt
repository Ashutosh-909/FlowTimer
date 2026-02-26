package com.ashutosh.flowtimer.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ashutosh.flowtimer.ui.theme.FlowTimerTheme
import com.ashutosh.flowtimer.ui.theme.GlowBlue
import com.ashutosh.flowtimer.ui.theme.GlowCyan
import com.ashutosh.flowtimer.ui.theme.GlowGold
import com.ashutosh.flowtimer.ui.theme.PixelTextDim
import com.ashutosh.flowtimer.ui.theme.SpaceBackground

/**
 * Visual state of the hourglass, determining glow color, overlay, and text.
 */
enum class HourglassVisualState {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED
}

/**
 * The central interactive element of the main screen.
 *
 * Contains a pixel-art hourglass inside a [GlowContainer], with state-dependent
 * overlays and text. The entire card is the tap target:
 * - **Tap** → start / pause / resume (cycles through states)
 * - **Long-press** → reset to persisted duration
 *
 * No visible buttons. Interaction is purely gesture-driven.
 *
 * @param visualState Current visual state determining glow, overlay, and text.
 * @param sandProgress 0.0 (start) to 1.0 (complete). Drives hourglass sand animation.
 * @param onTapHourglass Callback for tap: start / pause / resume.
 * @param onLongPressReset Callback for long-press: reset timer.
 * @param accessibilityStateDescription TalkBack state description (e.g. "Running, 24 minutes remaining").
 * @param modifier Modifier to apply.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HourglassCard(
    modifier: Modifier = Modifier,
    visualState: HourglassVisualState = HourglassVisualState.IDLE,
    sandProgress: Float = 0f,
    onTapHourglass: () -> Unit = {},
    onLongPressReset: () -> Unit = {},
    accessibilityStateDescription: String = ""
) {
    val glowColor: Color
    val glowAlpha: Float
    val overlayText: String?

    when (visualState) {
        HourglassVisualState.IDLE -> {
            glowColor = GlowBlue
            glowAlpha = 0.2f
            overlayText = "TAP TO START!"
        }
        HourglassVisualState.RUNNING -> {
            glowColor = GlowCyan
            glowAlpha = 0.6f
            overlayText = null
        }
        HourglassVisualState.PAUSED -> {
            glowColor = GlowBlue
            glowAlpha = 0.15f
            overlayText = null
        }
        HourglassVisualState.FINISHED -> {
            glowColor = GlowGold
            glowAlpha = 0.7f
            overlayText = "TAP!!"
        }
    }

    val a11yDescription = when (visualState) {
        HourglassVisualState.IDLE -> "Flow timer. Double-tap to start. Long press to reset."
        HourglassVisualState.RUNNING -> "Flow timer running. Double-tap to pause. Long press to reset."
        HourglassVisualState.PAUSED -> "Flow timer paused. Double-tap to resume. Long press to reset."
        HourglassVisualState.FINISHED -> "Flow time complete. Double-tap to dismiss. Long press to reset."
    }

    GlowContainer(
        glowColor = glowColor,
        glowAlpha = glowAlpha,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .semantics(mergeDescendants = true) {
                    contentDescription = a11yDescription
                    stateDescription = accessibilityStateDescription
                    role = Role.Button
                }
                .combinedClickable(
                    onClick = onTapHourglass,
                    onLongClick = onLongPressReset
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Overlay text above hourglass (Idle / Finished states)
            if (overlayText != null) {
                Text(
                    text = overlayText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = PixelTextDim,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Hourglass (placeholder Canvas until real pixel art is provided)
            PlaceholderHourglass(
                sandProgress = sandProgress,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Play icon overlay for Idle / Paused states
            if (visualState == HourglassVisualState.IDLE || visualState == HourglassVisualState.PAUSED) {
                Text(
                    text = "▶",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xCCFFFFFF),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

// ── Previews (one per visual state) ──

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D1B2A,
    widthDp = 320,
    heightDp = 420,
    name = "HourglassCard – Idle"
)
@Composable
private fun HourglassCardIdlePreview() {
    FlowTimerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceBackground),
            contentAlignment = Alignment.Center
        ) {
            HourglassCard(
                visualState = HourglassVisualState.IDLE,
                sandProgress = 0f
            )
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D1B2A,
    widthDp = 320,
    heightDp = 420,
    name = "HourglassCard – Running"
)
@Composable
private fun HourglassCardRunningPreview() {
    FlowTimerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceBackground),
            contentAlignment = Alignment.Center
        ) {
            HourglassCard(
                visualState = HourglassVisualState.RUNNING,
                sandProgress = 0.35f
            )
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D1B2A,
    widthDp = 320,
    heightDp = 420,
    name = "HourglassCard – Paused"
)
@Composable
private fun HourglassCardPausedPreview() {
    FlowTimerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceBackground),
            contentAlignment = Alignment.Center
        ) {
            HourglassCard(
                visualState = HourglassVisualState.PAUSED,
                sandProgress = 0.6f
            )
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D1B2A,
    widthDp = 320,
    heightDp = 420,
    name = "HourglassCard – Finished"
)
@Composable
private fun HourglassCardFinishedPreview() {
    FlowTimerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceBackground),
            contentAlignment = Alignment.Center
        ) {
            HourglassCard(
                visualState = HourglassVisualState.FINISHED,
                sandProgress = 1f
            )
        }
    }
}
