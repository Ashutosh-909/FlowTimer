package com.ashutosh.flowtimer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ashutosh.flowtimer.ui.theme.CardSurface
import com.ashutosh.flowtimer.ui.theme.FlowTimerTheme
import com.ashutosh.flowtimer.ui.theme.GlowBlue
import com.ashutosh.flowtimer.ui.theme.GlowCyan
import com.ashutosh.flowtimer.ui.theme.GlowGold

/**
 * Rounded-rect container with a configurable glow effect behind it.
 *
 * The glow is painted via [Modifier.drawBehind] using a radial gradient that extends
 * beyond the container border. Compatible with API 29+ (no RenderEffect).
 *
 * @param glowColor The primary glow color. Animated when changed.
 * @param glowAlpha Intensity of the glow (0.0 = invisible, 1.0 = full brightness).
 * @param glowRadius How far beyond the container the glow extends.
 * @param borderColor Border color of the container. Defaults to glowColor.
 * @param cornerRadius Corner radius matching the design system.
 * @param modifier Modifier to apply.
 * @param content Content inside the container.
 */
@Composable
fun GlowContainer(
    modifier: Modifier = Modifier,
    glowColor: Color = GlowBlue,
    glowAlpha: Float = 0.4f,
    glowRadius: Dp = 12.dp,
    borderColor: Color = glowColor,
    cornerRadius: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val animatedGlowColor by animateColorAsState(
        targetValue = glowColor,
        animationSpec = tween(durationMillis = 600),
        label = "glowColor"
    )
    val animatedGlowAlpha by animateFloatAsState(
        targetValue = glowAlpha,
        animationSpec = tween(durationMillis = 600),
        label = "glowAlpha"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = borderColor,
        animationSpec = tween(durationMillis = 600),
        label = "borderColor"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clearAndSetSemantics { } // decorative wrapper — child provides semantics
            .drawBehind {
                val glowPx = glowRadius.toPx()
                val cornerPx = cornerRadius.toPx()
                val expandedSize = Size(
                    size.width + glowPx * 2,
                    size.height + glowPx * 2
                )
                val topLeft = Offset(-glowPx, -glowPx)
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxDimension = maxOf(expandedSize.width, expandedSize.height) / 2f

                // Draw radial glow behind the container
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedGlowColor.copy(alpha = animatedGlowAlpha),
                            animatedGlowColor.copy(alpha = animatedGlowAlpha * 0.3f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxDimension
                    ),
                    topLeft = topLeft,
                    size = expandedSize,
                    cornerRadius = CornerRadius(cornerPx + glowPx)
                )

                // Draw the container fill
                drawRoundRect(
                    color = CardSurface.copy(alpha = 0.85f),
                    cornerRadius = CornerRadius(cornerPx)
                )
            }
            .border(
                width = 1.dp,
                color = animatedBorderColor.copy(alpha = 0.6f),
                shape = shape
            )
    ) {
        content()
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D1B2A,
    name = "GlowContainer – Idle"
)
@Composable
private fun GlowContainerIdlePreview() {
    FlowTimerTheme {
        Box(
            modifier = Modifier.padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            GlowContainer(
                glowColor = GlowBlue,
                glowAlpha = 0.2f
            ) {
                Text(
                    text = "IDLE",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(48.dp)
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D1B2A,
    name = "GlowContainer – Running"
)
@Composable
private fun GlowContainerRunningPreview() {
    FlowTimerTheme {
        Box(
            modifier = Modifier.padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            GlowContainer(
                glowColor = GlowCyan,
                glowAlpha = 0.6f
            ) {
                Text(
                    text = "RUNNING",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(48.dp)
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D1B2A,
    name = "GlowContainer – Complete"
)
@Composable
private fun GlowContainerCompletePreview() {
    FlowTimerTheme {
        Box(
            modifier = Modifier.padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            GlowContainer(
                glowColor = GlowGold,
                glowAlpha = 0.7f
            ) {
                Box(modifier = Modifier.size(200.dp, 280.dp))
            }
        }
    }
}
