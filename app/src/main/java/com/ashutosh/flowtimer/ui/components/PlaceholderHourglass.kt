package com.ashutosh.flowtimer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ashutosh.flowtimer.ui.theme.HourglassGlassWhite
import com.ashutosh.flowtimer.ui.theme.HourglassGray
import com.ashutosh.flowtimer.ui.theme.SandGold
import com.ashutosh.flowtimer.ui.theme.SandLight
import com.ashutosh.flowtimer.ui.theme.SpaceBackground

/**
 * Canvas-drawn placeholder hourglass.
 *
 * This is a temporary asset until the final pixel-art sprite is provided.
 * Draws a simplified hourglass shape with sand inside.
 *
 * @param sandProgress 0.0 = all sand at top (timer just started), 1.0 = all sand at bottom (complete).
 * @param modifier Modifier to apply.
 */
@Composable
internal fun PlaceholderHourglass(
    modifier: Modifier = Modifier,
    sandProgress: Float = 0.5f
) {
    Canvas(
        modifier = modifier
            .size(160.dp, 220.dp)
            .clearAndSetSemantics { } // parent HourglassCard provides semantics
    ) {
        val w = size.width
        val h = size.height
        val frameWidth = w * 0.08f

        // Frame top & bottom bars
        drawRect(
            color = HourglassGray,
            topLeft = Offset(w * 0.1f, 0f),
            size = androidx.compose.ui.geometry.Size(w * 0.8f, frameWidth)
        )
        drawRect(
            color = HourglassGray,
            topLeft = Offset(w * 0.1f, h - frameWidth),
            size = androidx.compose.ui.geometry.Size(w * 0.8f, frameWidth)
        )

        // Glass body (two triangles meeting at center)
        val glassPath = Path().apply {
            // Top triangle (wide at top, narrow at center)
            moveTo(w * 0.15f, frameWidth)
            lineTo(w * 0.85f, frameWidth)
            lineTo(w * 0.5f, h * 0.48f)
            close()

            // Bottom triangle (narrow at center, wide at bottom)
            moveTo(w * 0.5f, h * 0.52f)
            lineTo(w * 0.85f, h - frameWidth)
            lineTo(w * 0.15f, h - frameWidth)
            close()
        }
        drawPath(glassPath, color = HourglassGlassWhite.copy(alpha = 0.3f), style = Fill)

        // Sand in bottom (grows with progress)
        val bottomSandHeight = (h * 0.42f) * sandProgress
        if (bottomSandHeight > 0f) {
            drawBottomSand(w, h, frameWidth, bottomSandHeight)
        }

        // Sand in top (shrinks with progress)
        val topSandHeight = (h * 0.42f) * (1f - sandProgress)
        if (topSandHeight > 0f) {
            drawTopSand(w, h, frameWidth, topSandHeight)
        }

        // Sand stream (thin line flowing through neck when in progress)
        if (sandProgress > 0.01f && sandProgress < 0.99f) {
            drawLine(
                color = SandGold,
                start = Offset(w * 0.5f, h * 0.48f),
                end = Offset(w * 0.5f, h * 0.52f),
                strokeWidth = 3f
            )
        }

        // Frame side pillars
        drawRect(
            color = HourglassGray,
            topLeft = Offset(w * 0.1f, 0f),
            size = androidx.compose.ui.geometry.Size(frameWidth * 0.6f, h)
        )
        drawRect(
            color = HourglassGray,
            topLeft = Offset(w * 0.9f - frameWidth * 0.6f, 0f),
            size = androidx.compose.ui.geometry.Size(frameWidth * 0.6f, h)
        )
    }
}

private fun DrawScope.drawBottomSand(w: Float, h: Float, frameWidth: Float, sandHeight: Float) {
    val bottomY = h - frameWidth
    val topY = bottomY - sandHeight
    // Width at each y follows the triangle slope
    val widthAtBottom = w * 0.7f
    val widthAtTop = widthAtBottom * (1f - sandHeight / (h * 0.42f)) * 0.3f + widthAtBottom * 0.1f
    val sandPath = Path().apply {
        moveTo(w * 0.5f - widthAtBottom / 2f, bottomY)
        lineTo(w * 0.5f + widthAtBottom / 2f, bottomY)
        lineTo(w * 0.5f + widthAtTop / 2f, topY)
        lineTo(w * 0.5f - widthAtTop / 2f, topY)
        close()
    }
    drawPath(sandPath, color = SandGold, style = Fill)
    // Highlight on top of sand pile
    val highlightPath = Path().apply {
        moveTo(w * 0.5f - widthAtTop / 2f, topY)
        lineTo(w * 0.5f + widthAtTop / 2f, topY)
        lineTo(w * 0.5f, topY - sandHeight * 0.1f)
        close()
    }
    drawPath(highlightPath, color = SandLight.copy(alpha = 0.6f), style = Fill)
}

private fun DrawScope.drawTopSand(w: Float, h: Float, frameWidth: Float, sandHeight: Float) {
    val topY = frameWidth
    val bottomY = topY + sandHeight
    val widthAtTop = w * 0.7f
    val widthAtBottom = widthAtTop * (1f - sandHeight / (h * 0.42f)) * 0.3f + widthAtTop * 0.1f
    val sandPath = Path().apply {
        moveTo(w * 0.5f - widthAtTop / 2f, topY)
        lineTo(w * 0.5f + widthAtTop / 2f, topY)
        lineTo(w * 0.5f + widthAtBottom / 2f, bottomY)
        lineTo(w * 0.5f - widthAtBottom / 2f, bottomY)
        close()
    }
    drawPath(sandPath, color = SandGold, style = Fill)
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D1B2A,
    name = "Hourglass – 0% (Start)"
)
@Composable
private fun HourglassStartPreview() {
    PlaceholderHourglass(sandProgress = 0.0f)
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D1B2A,
    name = "Hourglass – 50% (Running)"
)
@Composable
private fun HourglassMidPreview() {
    PlaceholderHourglass(sandProgress = 0.5f)
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D1B2A,
    name = "Hourglass – 100% (Complete)"
)
@Composable
private fun HourglassCompletePreview() {
    PlaceholderHourglass(sandProgress = 1.0f)
}
