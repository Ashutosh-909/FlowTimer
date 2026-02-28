package com.ashutosh.flowtimer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import com.ashutosh.flowtimer.R

/**
 * Pixel-art hourglass rendered from a PNG resource.
 *
 * When [isRunning] is `true`, the hourglass flips 180° once to indicate
 * the timer has started. When it stops, it animates back to 0°.
 *
 * Uses [FilterQuality.None] for crisp nearest-neighbor scaling.
 *
 * @param isRunning Whether the timer is currently active (triggers 180° flip).
 * @param modifier Modifier to apply.
 */
@Composable
internal fun PlaceholderHourglass(
    modifier: Modifier = Modifier,
    isRunning: Boolean = false,
    sandProgress: Float = 0f // kept for API compat, unused for now
) {
    val rotation by animateFloatAsState(
        targetValue = if (isRunning) 180f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "hourglassFlip"
    )

    val context = LocalContext.current
    val imageBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.hourglass).asImageBitmap()
    }

    Image(
        bitmap = imageBitmap,
        contentDescription = null, // parent HourglassCard provides semantics
        filterQuality = FilterQuality.None,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(160.dp, 220.dp)
            .clearAndSetSemantics { }
            .graphicsLayer { rotationZ = rotation }
    )
}

// ── Previews ──

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D1B2A,
    name = "Hourglass – Idle"
)
@Composable
private fun HourglassIdlePreview() {
    PlaceholderHourglass(isRunning = false)
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D1B2A,
    name = "Hourglass – Running"
)
@Composable
private fun HourglassRunningPreview() {
    PlaceholderHourglass(isRunning = true)
}
