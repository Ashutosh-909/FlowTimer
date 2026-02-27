package com.ashutosh.flowtimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ashutosh.flowtimer.ui.home.HomeScreen
import com.ashutosh.flowtimer.ui.theme.FlowTimerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlowTimerTheme {
                HomeScreen()
            }
        }
    }
}