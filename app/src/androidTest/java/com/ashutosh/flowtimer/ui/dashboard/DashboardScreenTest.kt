package com.ashutosh.flowtimer.ui.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ashutosh.flowtimer.ui.theme.FlowTimerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Instrumented Compose UI tests for the dashboard screen and its components.
 *
 * Validates rendering, accessibility, and interaction for:
 * - [DashboardScreenContent] (full assembly)
 * - [TimeframeSelector]
 * - [FlowBarChart]
 * - [CalendarHeatmap]
 * - [ShareButton]
 */
@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── Helper data ──────────────────────────────────────────────────────

    private fun sampleBarData() = listOf(
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
        val start = today.minusDays(30)
        return (0..30L).map { offset ->
            DashboardViewModel.HeatmapDay(
                date = start.plusDays(offset),
                totalMinutes = (offset * 3).toInt()
            )
        }
    }

    private fun sampleUiState(
        timeframe: DashboardViewModel.Timeframe = DashboardViewModel.Timeframe.WEEK,
        totalMinutes: Int = 265
    ) = DashboardViewModel.UiState(
        timeframe = timeframe,
        totalFocusMinutes = totalMinutes,
        barData = sampleBarData(),
        heatmapData = sampleHeatmapDays(),
        isLoading = false
    )

    // ── DashboardScreenContent ──────────────────────────────────────────

    @Test
    fun dashboardScreen_displaysFlowTimeTitle() {
        composeTestRule.setContent {
            FlowTimerTheme {
                DashboardScreenContent(uiState = sampleUiState())
            }
        }

        composeTestRule.onNodeWithText("FLOW TIME").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_displaysTotalFocusTime() {
        composeTestRule.setContent {
            FlowTimerTheme {
                DashboardScreenContent(
                    uiState = sampleUiState(totalMinutes = 155)
                )
            }
        }

        // "2 h 35 min" from 155 minutes
        composeTestRule.onNodeWithText("2 h 35 min").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_displaysZeroMinutesWhenEmpty() {
        composeTestRule.setContent {
            FlowTimerTheme {
                DashboardScreenContent(
                    uiState = sampleUiState(totalMinutes = 0)
                )
            }
        }

        composeTestRule.onNodeWithText("0 min").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_displaysAllTimeframeChips() {
        composeTestRule.setContent {
            FlowTimerTheme {
                DashboardScreenContent(uiState = sampleUiState())
            }
        }

        composeTestRule.onNodeWithText("WEEK").assertIsDisplayed()
        composeTestRule.onNodeWithText("MONTH").assertIsDisplayed()
        composeTestRule.onNodeWithText("YEAR").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_displaysShareButton() {
        composeTestRule.setContent {
            FlowTimerTheme {
                DashboardScreenContent(uiState = sampleUiState())
            }
        }

        composeTestRule.onNodeWithText("SHARE STATS").assertIsDisplayed()
    }

    // ── TimeframeSelector interaction ──────────────────────────────────

    @Test
    fun timeframeSelector_tappingChipInvokesCallback() {
        var selected: DashboardViewModel.Timeframe? = null

        composeTestRule.setContent {
            FlowTimerTheme {
                DashboardScreenContent(
                    uiState = sampleUiState(timeframe = DashboardViewModel.Timeframe.WEEK),
                    onTimeframeSelected = { selected = it }
                )
            }
        }

        composeTestRule.onNodeWithText("MONTH").performClick()
        assertEquals(DashboardViewModel.Timeframe.MONTH, selected)
    }

    @Test
    fun timeframeSelector_tappingYearInvokesCallback() {
        var selected: DashboardViewModel.Timeframe? = null

        composeTestRule.setContent {
            FlowTimerTheme {
                DashboardScreenContent(
                    uiState = sampleUiState(timeframe = DashboardViewModel.Timeframe.WEEK),
                    onTimeframeSelected = { selected = it }
                )
            }
        }

        composeTestRule.onNodeWithText("YEAR").performClick()
        assertEquals(DashboardViewModel.Timeframe.YEAR, selected)
    }

    // ── Accessibility ────────────────────────────────────────────────────

    @Test
    fun timeframeChips_haveContentDescriptions() {
        composeTestRule.setContent {
            FlowTimerTheme {
                DashboardScreenContent(uiState = sampleUiState())
            }
        }

        composeTestRule.onNodeWithContentDescription("Show weekly stats")
            .assertExists()
        composeTestRule.onNodeWithContentDescription("Show monthly stats")
            .assertExists()
        composeTestRule.onNodeWithContentDescription("Show yearly stats")
            .assertExists()
    }

    @Test
    fun shareButton_hasContentDescription() {
        composeTestRule.setContent {
            FlowTimerTheme {
                DashboardScreenContent(uiState = sampleUiState())
            }
        }

        composeTestRule.onNodeWithContentDescription("Share your flow stats")
            .assertExists()
    }

    @Test
    fun barChart_hasContentDescription() {
        composeTestRule.setContent {
            FlowTimerTheme {
                DashboardScreenContent(uiState = sampleUiState())
            }
        }

        // Bar chart total: 45+25+0+60+30+90+15 = 265
        composeTestRule.onNode(
            hasContentDescription("Bar chart showing 265 total minutes across 7 bars")
        ).assertExists()
    }

    @Test
    fun calendarHeatmap_hasContentDescription() {
        composeTestRule.setContent {
            FlowTimerTheme {
                DashboardScreenContent(uiState = sampleUiState())
            }
        }

        // Heatmap has 31 days, first day (offset 0) has 0 minutes, so 30 active
        composeTestRule.onNode(
            hasContentDescription(value = "Calendar heatmap", substring = true)
        ).assertExists()
    }

    @Test
    fun hourglassImage_hasContentDescription() {
        composeTestRule.setContent {
            FlowTimerTheme {
                DashboardScreenContent(uiState = sampleUiState())
            }
        }

        composeTestRule.onNodeWithContentDescription("Hourglass").assertExists()
    }

    // ── ShareButton interaction ─────────────────────────────────────────

    @Test
    fun shareButton_tappingInvokesCallback() {
        var shared = false

        composeTestRule.setContent {
            FlowTimerTheme {
                DashboardScreenContent(
                    uiState = sampleUiState(),
                    onShare = { shared = true }
                )
            }
        }

        composeTestRule.onNodeWithText("SHARE STATS").performClick()
        assertEquals(true, shared)
    }
}
