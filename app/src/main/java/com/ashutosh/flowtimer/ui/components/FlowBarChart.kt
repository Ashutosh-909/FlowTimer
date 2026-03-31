package com.ashutosh.flowtimer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashutosh.flowtimer.ui.dashboard.DashboardViewModel
import com.ashutosh.flowtimer.ui.theme.CardSurface
import com.ashutosh.flowtimer.ui.theme.FlowTimerTheme
import com.ashutosh.flowtimer.ui.theme.GlowBlue
import com.ashutosh.flowtimer.ui.theme.GlowCyan
import com.ashutosh.flowtimer.ui.theme.PixelFontFamily
import com.ashutosh.flowtimer.ui.theme.PixelText
import com.ashutosh.flowtimer.ui.theme.PixelTextDim
import com.ashutosh.flowtimer.ui.theme.SpaceBackground
import com.ashutosh.flowtimer.ui.theme.SpaceMid

/**
 * Manhattan-style bar chart drawn entirely on [Canvas].
 *
 * Bars use a [GlowBlue] → [GlowCyan] vertical gradient. X-axis labels appear
 * below the bars, and subtle grid lines are drawn at 25 %, 50 %, and 75 % of the
 * max value. Tapping a bar highlights it and shows a small tooltip with the
 * exact minute value.
 *
 * Bars animate from zero height to their actual height on first composition
 * and whenever [entries] changes.
 *
 * @param modifier Modifier to apply (should include width and height).
 * @param entries Label + value pairs to render as bars.
 */
@Composable
fun FlowBarChart(
    modifier: Modifier = Modifier,
    entries: List<DashboardViewModel.BarEntry>
) {
    if (entries.isEmpty()) return

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(entries) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, tween(durationMillis = 600))
    }

    var selectedIndex by remember { mutableIntStateOf(-1) }
    val totalMinutes = entries.sumOf { it.minutes }
    val maxMinutes = (entries.maxOfOrNull { it.minutes } ?: 1).coerceAtLeast(1)

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontFamily = PixelFontFamily,
        fontSize = 8.sp,
        color = PixelTextDim,
        textAlign = TextAlign.Center
    )
    val tooltipStyle = TextStyle(
        fontFamily = PixelFontFamily,
        fontSize = 8.sp,
        color = PixelText,
        textAlign = TextAlign.Center
    )

    val density = LocalDensity.current
    val bottomPaddingPx = with(density) { 20.dp.toPx() }
    val gapPx = with(density) { 4.dp.toPx() }
    val barCornerRadiusPx = with(density) { 4.dp.toPx() }
    val tooltipPadHPx = with(density) { 4.dp.toPx() }
    val tooltipPadVPx = with(density) { 2.dp.toPx() }
    val tooltipCornerPx = with(density) { 4.dp.toPx() }
    val labelOffsetPx = with(density) { 4.dp.toPx() }
    val dotRadiusPx = with(density) { 2.dp.toPx() }

    val gridLineColor = SpaceMid.copy(alpha = 0.3f)
    val barGradient = Brush.verticalGradient(listOf(GlowCyan, GlowBlue))
    val selectedBarGradient = Brush.verticalGradient(
        listOf(GlowCyan.copy(alpha = 0.8f), GlowBlue.copy(alpha = 0.8f))
    )

    Canvas(
        modifier = modifier
            .semantics {
                contentDescription =
                    "Bar chart showing $totalMinutes total minutes across ${entries.size} bars"
            }
            .pointerInput(entries) {
                detectTapGestures { offset ->
                    val barTotalWidth = size.width.toFloat() / entries.size
                    val tappedIndex = (offset.x / barTotalWidth).toInt()
                        .coerceIn(0, entries.size - 1)
                    selectedIndex = if (selectedIndex == tappedIndex) -1 else tappedIndex
                }
            }
    ) {
        val chartHeight = size.height - bottomPaddingPx
        val barTotalWidth = size.width / entries.size

        // ── Grid lines at 25 %, 50 %, 75 % ──
        listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
            val y = chartHeight * (1f - fraction)
            drawLine(
                color = gridLineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        // ── Bars + labels ──
        entries.forEachIndexed { index, entry ->
            val barX = index * barTotalWidth + gapPx / 2
            val barWidth = barTotalWidth - gapPx
            val fraction = entry.minutes.toFloat() / maxMinutes
            val animatedFraction = fraction * animProgress.value
            val barHeight = (chartHeight * animatedFraction).coerceAtLeast(0f)

            if (entry.minutes > 0) {
                val barTop = chartHeight - barHeight
                val isSelected = index == selectedIndex
                drawRoundRect(
                    brush = if (isSelected) selectedBarGradient else barGradient,
                    topLeft = Offset(barX, barTop),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barCornerRadiusPx, barCornerRadiusPx)
                )
            } else {
                // Zero-value: render a small dot
                drawCircle(
                    color = GlowBlue.copy(alpha = 0.3f),
                    radius = dotRadiusPx,
                    center = Offset(barX + barWidth / 2, chartHeight - dotRadiusPx)
                )
            }

            // X-axis label
            val labelResult = textMeasurer.measure(entry.label, labelStyle)
            drawText(
                textLayoutResult = labelResult,
                topLeft = Offset(
                    barX + (barWidth - labelResult.size.width) / 2,
                    chartHeight + labelOffsetPx
                )
            )
        }

        // ── Tooltip on selected bar ──
        if (selectedIndex in entries.indices) {
            val entry = entries[selectedIndex]
            val tooltipText = "${entry.minutes} min"
            val tooltipResult = textMeasurer.measure(tooltipText, tooltipStyle)

            val barX = selectedIndex * barTotalWidth + gapPx / 2
            val barWidth = barTotalWidth - gapPx
            val fraction = entry.minutes.toFloat() / maxMinutes
            val barHeight = chartHeight * fraction * animProgress.value
            val barTop = chartHeight - barHeight

            val tooltipW = tooltipResult.size.width + tooltipPadHPx * 2
            val tooltipH = tooltipResult.size.height + tooltipPadVPx * 2
            val tooltipX = (barX + barWidth / 2 - tooltipW / 2)
                .coerceIn(0f, size.width - tooltipW)
            val tooltipY = (barTop - tooltipH - labelOffsetPx).coerceAtLeast(0f)

            // Background
            drawRoundRect(
                color = CardSurface,
                topLeft = Offset(tooltipX, tooltipY),
                size = Size(tooltipW, tooltipH),
                cornerRadius = CornerRadius(tooltipCornerPx)
            )
            // Text
            drawText(
                textLayoutResult = tooltipResult,
                topLeft = Offset(tooltipX + tooltipPadHPx, tooltipY + tooltipPadVPx)
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────

private val sampleWeekEntries = listOf(
    DashboardViewModel.BarEntry("MON", 45),
    DashboardViewModel.BarEntry("TUE", 25),
    DashboardViewModel.BarEntry("WED", 0),
    DashboardViewModel.BarEntry("THU", 60),
    DashboardViewModel.BarEntry("FRI", 30),
    DashboardViewModel.BarEntry("SAT", 90),
    DashboardViewModel.BarEntry("SUN", 15)
)

private val sampleMonthEntries = listOf(
    DashboardViewModel.BarEntry("W1", 120),
    DashboardViewModel.BarEntry("W2", 200),
    DashboardViewModel.BarEntry("W3", 80),
    DashboardViewModel.BarEntry("W4", 160),
    DashboardViewModel.BarEntry("W5", 50)
)

private val sampleYearEntries = listOf(
    DashboardViewModel.BarEntry("JAN", 300),
    DashboardViewModel.BarEntry("FEB", 250),
    DashboardViewModel.BarEntry("MAR", 400),
    DashboardViewModel.BarEntry("APR", 150),
    DashboardViewModel.BarEntry("MAY", 200),
    DashboardViewModel.BarEntry("JUN", 350),
    DashboardViewModel.BarEntry("JUL", 0),
    DashboardViewModel.BarEntry("AUG", 100),
    DashboardViewModel.BarEntry("SEP", 280),
    DashboardViewModel.BarEntry("OCT", 0),
    DashboardViewModel.BarEntry("NOV", 0),
    DashboardViewModel.BarEntry("DEC", 0)
)

@Preview(showBackground = true, backgroundColor = 0xFF0D1B2A)
@Composable
private fun FlowBarChartWeekPreview() {
    FlowTimerTheme {
        Box(modifier = Modifier.background(SpaceBackground).padding(16.dp)) {
            FlowBarChart(
                entries = sampleWeekEntries,
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1B2A)
@Composable
private fun FlowBarChartMonthPreview() {
    FlowTimerTheme {
        Box(modifier = Modifier.background(SpaceBackground).padding(16.dp)) {
            FlowBarChart(
                entries = sampleMonthEntries,
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1B2A)
@Composable
private fun FlowBarChartYearPreview() {
    FlowTimerTheme {
        Box(modifier = Modifier.background(SpaceBackground).padding(16.dp)) {
            FlowBarChart(
                entries = sampleYearEntries,
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
        }
    }
}
