package com.ashutosh.flowtimer.ui.dashboard

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ashutosh.flowtimer.ui.components.CalendarHeatmap
import com.ashutosh.flowtimer.ui.components.FlowBarChart
import com.ashutosh.flowtimer.ui.components.ShareButton
import com.ashutosh.flowtimer.ui.components.TimeframeSelector
import com.ashutosh.flowtimer.ui.theme.FlowTimerTheme
import com.ashutosh.flowtimer.ui.theme.GlowBlue
import com.ashutosh.flowtimer.ui.theme.GlowCyan
import com.ashutosh.flowtimer.ui.theme.PixelText
import com.ashutosh.flowtimer.ui.theme.PixelTextDim
import com.ashutosh.flowtimer.ui.theme.SandGold
import com.ashutosh.flowtimer.ui.theme.SpaceBackground
import java.io.File
import java.time.LocalDate

/**
 * Root composable for the dashboard screen.
 *
 * Assembles: title → [TimeframeSelector] → total focus text →
 * [FlowBarChart] → [CalendarHeatmap] → [ShareButton].
 *
 * The entire screen is vertically scrollable. Connected to
 * [DashboardViewModel] for data.
 *
 * @param modifier Modifier to apply.
 * @param viewModel The [DashboardViewModel] providing UI state and actions.
 */
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current

    DashboardScreenContent(
        modifier = modifier,
        uiState = uiState,
        onTimeframeSelected = viewModel::onTimeframeSelected,
        onShare = {
            shareDashboardCapture(view, context, uiState.totalFocusMinutes, uiState.timeframe)
        }
    )
}

/**
 * Stateless content composable for the dashboard screen, enabling previews
 * without a ViewModel.
 */
@Composable
internal fun DashboardScreenContent(
    modifier: Modifier = Modifier,
    uiState: DashboardViewModel.UiState = DashboardViewModel.UiState(),
    onTimeframeSelected: (DashboardViewModel.Timeframe) -> Unit = {},
    onShare: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // ── Title ──
        Text(
            text = "DASHBOARD",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Timeframe selector ──
        TimeframeSelector(
            selected = uiState.timeframe,
            onSelected = onTimeframeSelected
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Total focus time headline ──
        TotalFocusText(totalMinutes = uiState.totalFocusMinutes)

        Spacer(modifier = Modifier.height(24.dp))

        // ── Bar chart ──
        FlowBarChart(
            entries = uiState.barData,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Calendar heatmap ──
        CalendarHeatmap(
            days = uiState.heatmapData,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Share button ──
        ShareButton(onShare = onShare)

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ── Total focus text ────────────────────────────────────────────────────

/**
 * Large headline showing the total focus time for the selected timeframe.
 *
 * Displays hours and minutes separately: number in [PixelText], unit in
 * [PixelTextDim]. Falls back to "0 min" for zero.
 */
@Composable
private fun TotalFocusText(
    modifier: Modifier = Modifier,
    totalMinutes: Int
) {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    val displayText = when {
        hours > 0 && minutes > 0 -> "$hours h $minutes min"
        hours > 0 -> "$hours h"
        else -> "$minutes min"
    }

    Text(
        text = displayText,
        style = MaterialTheme.typography.displayMedium,
        color = PixelText,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
}

// ── Share helper ────────────────────────────────────────────────────────

/**
 * Captures the current view as a bitmap, saves it to the app cache, and
 * launches the system share sheet with the image and a text summary.
 */
private fun shareDashboardCapture(
    view: View,
    context: Context,
    totalMinutes: Int,
    timeframe: DashboardViewModel.Timeframe
) {
    val bitmap = view.drawToBitmap(Bitmap.Config.ARGB_8888)

    val file = File(context.cacheDir, "flow_stats.png")
    file.outputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    bitmap.recycle()

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val timeframeLabel = timeframe.name.lowercase()
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    val timeText = when {
        hours > 0 && mins > 0 -> "$hours hours and $mins minutes"
        hours > 0 -> "$hours hours"
        else -> "$mins minutes"
    }

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(
            Intent.EXTRA_TEXT,
            "I focused for $timeText this $timeframeLabel with Flow Time! \uD83D\uDD25"
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(shareIntent, null))
}

// ── Previews ─────────────────────────────────────────────────────────────

private fun sampleBarEntries() = listOf(
    DashboardViewModel.BarEntry("MON", 45),
    DashboardViewModel.BarEntry("TUE", 25),
    DashboardViewModel.BarEntry("WED", 0),
    DashboardViewModel.BarEntry("THU", 60),
    DashboardViewModel.BarEntry("FRI", 30),
    DashboardViewModel.BarEntry("SAT", 90),
    DashboardViewModel.BarEntry("SUN", 15)
)

private fun sampleHeatmapDays(): List<DashboardViewModel.HeatmapDay> {
    val today = LocalDate.now()
    val start = today.minusMonths(6)
    val days = mutableListOf<DashboardViewModel.HeatmapDay>()
    var d = start
    var i = 0
    while (!d.isAfter(today)) {
        val m = when {
            i % 7 == 0 -> 0; i % 5 == 0 -> 100; i % 3 == 0 -> 50
            i % 2 == 0 -> 20; else -> 5
        }
        days.add(DashboardViewModel.HeatmapDay(d, m))
        d = d.plusDays(1)
        i++
    }
    return days
}

@Preview(showBackground = true, showSystemUi = true, name = "Dashboard – Week")
@Composable
private fun DashboardScreenWeekPreview() {
    FlowTimerTheme {
        DashboardScreenContent(
            uiState = DashboardViewModel.UiState(
                timeframe = DashboardViewModel.Timeframe.WEEK,
                totalFocusMinutes = 265,
                barData = sampleBarEntries(),
                heatmapData = sampleHeatmapDays(),
                isLoading = false
            )
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Dashboard – Empty")
@Composable
private fun DashboardScreenEmptyPreview() {
    FlowTimerTheme {
        DashboardScreenContent(
            uiState = DashboardViewModel.UiState(
                timeframe = DashboardViewModel.Timeframe.WEEK,
                totalFocusMinutes = 0,
                barData = listOf(
                    DashboardViewModel.BarEntry("MON", 0),
                    DashboardViewModel.BarEntry("TUE", 0),
                    DashboardViewModel.BarEntry("WED", 0),
                    DashboardViewModel.BarEntry("THU", 0),
                    DashboardViewModel.BarEntry("FRI", 0),
                    DashboardViewModel.BarEntry("SAT", 0),
                    DashboardViewModel.BarEntry("SUN", 0)
                ),
                heatmapData = emptyList(),
                isLoading = false
            )
        )
    }
}
