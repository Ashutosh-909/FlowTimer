package com.ashutosh.flowtimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ashutosh.flowtimer.ui.theme.FlowTimerTheme
import com.ashutosh.flowtimer.ui.theme.SpaceBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlowTimerTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SpaceBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "FLOW",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "TIME",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "25:00",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D1B2A,
    name = "Flow Time – Theme Preview"
)
@Composable
fun FlowTimeThemePreview() {
    FlowTimerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "FLOW",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "TIME",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "25:00",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 32.dp)
                )
                Text(
                    text = "TAP TO START!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}