package com.ashutosh.flowtimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ashutosh.flowtimer.ui.dashboard.DashboardScreen
import com.ashutosh.flowtimer.ui.home.HomeScreen
import com.ashutosh.flowtimer.ui.theme.FlowTimerTheme
import com.ashutosh.flowtimer.ui.theme.GlowBlue
import com.ashutosh.flowtimer.ui.theme.PixelTextDim

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlowTimerTheme {
                MainContent()
            }
        }
    }
}

/**
 * Root content with [HorizontalPager]: page 0 = Dashboard, page 1 = Home.
 * Starts on page 1 (Home). Swiping right reveals the Dashboard.
 * A subtle dot indicator at the top hints at the swipeable pages.
 */
@Composable
private fun MainContent() {
    val pagerState = rememberPagerState(initialPage = 1) { 2 }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> DashboardScreen()
                1 -> HomeScreen()
            }
        }

        // ── Page indicator dots ──
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
                .clearAndSetSemantics { }
        ) {
            repeat(2) { page ->
                val color = if (pagerState.currentPage == page) {
                    GlowBlue
                } else {
                    PixelTextDim.copy(alpha = 0.5f)
                }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(6.dp)
                        .background(color, CircleShape)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainContentPreview() {
    FlowTimerTheme {
        MainContent()
    }
}