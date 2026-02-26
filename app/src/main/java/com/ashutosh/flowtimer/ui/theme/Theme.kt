package com.ashutosh.flowtimer.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PixelColorScheme = darkColorScheme(
    primary = GlowBlue,
    onPrimary = PixelText,
    primaryContainer = CardSurface,
    onPrimaryContainer = PixelText,
    secondary = GlowCyan,
    onSecondary = SpaceBackground,
    secondaryContainer = SpaceMid,
    onSecondaryContainer = PixelText,
    tertiary = SandGold,
    onTertiary = SpaceBackground,
    background = SpaceBackground,
    onBackground = PixelText,
    surface = SpaceBackground,
    onSurface = PixelText,
    surfaceVariant = CardSurface,
    onSurfaceVariant = PixelTextDim,
    outline = TimerPillBorder,
    outlineVariant = GlowBlue,
    error = NotifPink,
    onError = SpaceBackground,
    scrim = Color.Black
)

/**
 * Always-dark pixel-art theme for Flow Time.
 * No light variant — the space aesthetic is the identity.
 */
@Composable
fun FlowTimerTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = PixelColorScheme,
        typography = PixelTypography,
        shapes = PixelShapes,
        content = content
    )
}