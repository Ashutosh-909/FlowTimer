package com.ashutosh.flowtimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ashutosh.flowtimer.ui.theme.FlowTimerTheme
import com.ashutosh.flowtimer.ui.theme.GlowBlue
import com.ashutosh.flowtimer.ui.theme.PixelText
import com.ashutosh.flowtimer.ui.theme.SpaceBackground

/**
 * Pixel-styled share button with a share icon and "SHARE STATS" label.
 *
 * [GlowBlue] background, 16 dp corner radius, 48 dp minimum height.
 *
 * @param modifier Modifier to apply.
 * @param onShare Callback when the button is tapped.
 */
@Composable
fun ShareButton(
    modifier: Modifier = Modifier,
    onShare: () -> Unit
) {
    val buttonShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics {
                role = Role.Button
                contentDescription = "Share your flow stats"
            }
            .clip(buttonShape)
            .background(GlowBlue)
            .clickable(onClick = onShare)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = PixelText,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SHARE STATS",
                style = MaterialTheme.typography.bodyMedium,
                color = PixelText
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF0D1B2A)
@Composable
private fun ShareButtonPreview() {
    FlowTimerTheme {
        Box(
            modifier = Modifier
                .background(SpaceBackground)
                .padding(16.dp)
        ) {
            ShareButton(onShare = {})
        }
    }
}
