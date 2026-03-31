package com.ashutosh.flowtimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ashutosh.flowtimer.ui.dashboard.DashboardViewModel
import com.ashutosh.flowtimer.ui.theme.CardSurface
import com.ashutosh.flowtimer.ui.theme.FlowTimerTheme
import com.ashutosh.flowtimer.ui.theme.GlowBlue
import com.ashutosh.flowtimer.ui.theme.PixelText
import com.ashutosh.flowtimer.ui.theme.PixelTextDim
import com.ashutosh.flowtimer.ui.theme.SpaceBackground
import com.ashutosh.flowtimer.ui.theme.TimerPillBorder

/**
 * Segmented row of three toggle chips: WEEK · MONTH · YEAR.
 *
 * Selected chip uses [GlowBlue] background with [PixelText] label.
 * Unselected chips use [CardSurface] background with a [TimerPillBorder] border.
 *
 * @param modifier Modifier to apply.
 * @param selected Currently active timeframe.
 * @param onSelected Callback when a chip is tapped.
 */
@Composable
fun TimeframeSelector(
    modifier: Modifier = Modifier,
    selected: DashboardViewModel.Timeframe,
    onSelected: (DashboardViewModel.Timeframe) -> Unit
) {
    val chipShape = RoundedCornerShape(16.dp)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        DashboardViewModel.Timeframe.entries.forEach { timeframe ->
            val isSelected = timeframe == selected
            val accessibilityLabel = when (timeframe) {
                DashboardViewModel.Timeframe.WEEK -> "Show weekly stats"
                DashboardViewModel.Timeframe.MONTH -> "Show monthly stats"
                DashboardViewModel.Timeframe.YEAR -> "Show yearly stats"
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics {
                        role = Role.Tab
                        contentDescription = accessibilityLabel
                    }
                    .clip(chipShape)
                    .then(
                        if (isSelected) {
                            Modifier.background(GlowBlue)
                        } else {
                            Modifier
                                .background(CardSurface)
                                .border(1.dp, TimerPillBorder, chipShape)
                        }
                    )
                    .clickable { onSelected(timeframe) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = timeframe.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) PixelText else PixelTextDim
                )
            }
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF0D1B2A)
@Composable
private fun TimeframeSelectorWeekPreview() {
    FlowTimerTheme {
        Box(modifier = Modifier.background(SpaceBackground).padding(16.dp)) {
            TimeframeSelector(
                selected = DashboardViewModel.Timeframe.WEEK,
                onSelected = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1B2A)
@Composable
private fun TimeframeSelectorMonthPreview() {
    FlowTimerTheme {
        Box(modifier = Modifier.background(SpaceBackground).padding(16.dp)) {
            TimeframeSelector(
                selected = DashboardViewModel.Timeframe.MONTH,
                onSelected = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1B2A)
@Composable
private fun TimeframeSelectorYearPreview() {
    FlowTimerTheme {
        Box(modifier = Modifier.background(SpaceBackground).padding(16.dp)) {
            TimeframeSelector(
                selected = DashboardViewModel.Timeframe.YEAR,
                onSelected = {}
            )
        }
    }
}
