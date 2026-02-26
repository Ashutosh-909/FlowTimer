package com.ashutosh.flowtimer.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ashutosh.flowtimer.ui.theme.SpaceBackground
import com.ashutosh.flowtimer.ui.theme.StarCyan
import com.ashutosh.flowtimer.ui.theme.StarGold
import com.ashutosh.flowtimer.ui.theme.StarPink
import com.ashutosh.flowtimer.ui.theme.StarPurple
import com.ashutosh.flowtimer.ui.theme.StarWhite
import kotlin.random.Random

private val STAR_COLORS = listOf(
    StarWhite,
    StarWhite,
    StarWhite,
    StarWhite,
    StarCyan,
    StarCyan,
    StarPink,
    StarPurple,
    StarGold
)

private data class Star(
    val xFraction: Float,
    val yFraction: Float,
    val color: Color,
    val radiusDp: Float,
    val isCross: Boolean,
    val twinklePhaseOffset: Float
)

/**
 * Procedural starfield background filling the entire composable area.
 *
 * Draws 40–80 pixel-dot stars at deterministic positions with multi-color sparkle.
 * A subset of stars twinkle via [infiniteTransition] alpha animation.
 * Purely decorative — cleared from the semantics tree.
 *
 * @param modifier Modifier to apply.
 * @param starCount Number of stars to draw.
 * @param seed Random seed for deterministic star positions (survives recomposition).
 */
@Composable
fun StarField(
    modifier: Modifier = Modifier,
    starCount: Int = 60,
    seed: Long = 42L
) {
    val stars = remember(starCount, seed) {
        val rng = Random(seed)
        List(starCount) {
            Star(
                xFraction = rng.nextFloat(),
                yFraction = rng.nextFloat(),
                color = STAR_COLORS[rng.nextInt(STAR_COLORS.size)],
                radiusDp = 0.5f + rng.nextFloat() * 1.5f, // 0.5–2.0 dp
                isCross = rng.nextFloat() < 0.12f, // ~12% are sparkle crosses
                twinklePhaseOffset = rng.nextFloat() * 1000f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "starTwinkle")
    val twinkleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkleAlpha"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics { } // decorative — hidden from TalkBack
    ) {
        val w = size.width
        val h = size.height

        stars.forEach { star ->
            val x = star.xFraction * w
            val y = star.yFraction * h
            val radius = star.radiusDp.dp.toPx()

            // Alternate alpha for twinkle effect (phase-offset per star)
            val phase = ((twinkleAlpha + star.twinklePhaseOffset) % 1.0f)
                .coerceIn(0.3f, 1.0f)
            val color = star.color.copy(alpha = phase)

            if (star.isCross) {
                // Draw a 4-point pixel cross (sparkle)
                val arm = radius * 1.5f
                drawLine(color, Offset(x - arm, y), Offset(x + arm, y), strokeWidth = 1.dp.toPx())
                drawLine(color, Offset(x, y - arm), Offset(x, y + arm), strokeWidth = 1.dp.toPx())
            } else {
                drawCircle(color = color, radius = radius, center = Offset(x, y))
            }
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D1B2A,
    widthDp = 360,
    heightDp = 800,
    name = "StarField"
)
@Composable
private fun StarFieldPreview() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(SpaceBackground)
    }
    StarField()
}
