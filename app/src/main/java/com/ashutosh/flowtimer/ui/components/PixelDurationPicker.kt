package com.ashutosh.flowtimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ashutosh.flowtimer.ui.theme.CardSurface
import com.ashutosh.flowtimer.ui.theme.FlowTimerTheme
import com.ashutosh.flowtimer.ui.theme.GlowBlue
import com.ashutosh.flowtimer.ui.theme.GlowCyan
import com.ashutosh.flowtimer.ui.theme.PixelTextDim
import com.ashutosh.flowtimer.ui.theme.SpaceBackground
import com.ashutosh.flowtimer.ui.theme.TimerPillBorder
import com.ashutosh.flowtimer.ui.theme.TimerPillFill

/**
 * Pixel-styled duration picker dialog for selecting flow time (1–120 minutes).
 *
 * Uses increment/decrement buttons with a central minute display,
 * styled to match the pixel-art space theme. Writes the selected
 * duration to DataStore on confirm.
 *
 * @param modifier Modifier to apply.
 * @param currentMinutes The currently set duration in minutes.
 * @param onConfirm Called with the selected minutes when the user confirms.
 * @param onDismiss Called when the user dismisses the dialog without saving.
 */
@Composable
fun PixelDurationPicker(
    modifier: Modifier = Modifier,
    currentMinutes: Int = 25,
    onConfirm: (Int) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    var selectedMinutes by remember(currentMinutes) { mutableIntStateOf(currentMinutes) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PixelDurationPickerContent(
            modifier = modifier,
            selectedMinutes = selectedMinutes,
            onMinutesChange = { selectedMinutes = it },
            onConfirm = { onConfirm(selectedMinutes) },
            onDismiss = onDismiss
        )
    }
}

/**
 * Stateless content of the duration picker, enabling previews without a Dialog wrapper.
 */
@Composable
internal fun PixelDurationPickerContent(
    modifier: Modifier = Modifier,
    selectedMinutes: Int = 25,
    onMinutesChange: (Int) -> Unit = {},
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val containerShape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .padding(horizontal = 40.dp)
            .clip(containerShape)
            .background(CardSurface)
            .border(
                width = 1.dp,
                color = TimerPillBorder,
                shape = containerShape
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Title ──
        Text(
            text = "SET FLOW TIME",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Minute selector: [ – ]  MM min  [ + ] ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Decrement button
            PickerButton(
                text = "–",
                contentDesc = "Decrease duration",
                enabled = selectedMinutes > MIN_DURATION,
                onClick = {
                    onMinutesChange((selectedMinutes - STEP_SIZE).coerceAtLeast(MIN_DURATION))
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Minutes display
            Box(
                modifier = Modifier
                    .semantics(mergeDescendants = true) {
                        contentDescription = "$selectedMinutes minutes"
                    }
                    .background(
                        color = TimerPillFill,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = TimerPillBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format("%d", selectedMinutes),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Increment button
            PickerButton(
                text = "+",
                contentDesc = "Increase duration",
                enabled = selectedMinutes < MAX_DURATION,
                onClick = {
                    onMinutesChange((selectedMinutes + STEP_SIZE).coerceAtMost(MAX_DURATION))
                }
            )
        }

        // Unit label
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "min",
            style = MaterialTheme.typography.bodyMedium,
            color = PixelTextDim,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Confirm / Cancel buttons ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            // Cancel
            Box(
                modifier = Modifier
                    .semantics {
                        role = Role.Button
                        contentDescription = "Cancel"
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpaceBackground)
                    .border(
                        width = 1.dp,
                        color = TimerPillBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CANCEL",
                    style = MaterialTheme.typography.labelMedium,
                    color = PixelTextDim
                )
            }

            // Confirm
            Box(
                modifier = Modifier
                    .semantics {
                        role = Role.Button
                        contentDescription = "Confirm duration of $selectedMinutes minutes"
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlowBlue.copy(alpha = 0.3f))
                    .border(
                        width = 1.dp,
                        color = GlowCyan,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(onClick = onConfirm)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "OK",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

/**
 * Circular increment/decrement button for the duration picker.
 */
@Composable
private fun PickerButton(
    text: String,
    contentDesc: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.3f

    Box(
        modifier = Modifier
            .size(48.dp)
            .semantics {
                role = Role.Button
                contentDescription = contentDesc
            }
            .clip(RoundedCornerShape(12.dp))
            .background(GlowBlue.copy(alpha = 0.2f * alpha))
            .border(
                width = 1.dp,
                color = GlowBlue.copy(alpha = alpha),
                shape = RoundedCornerShape(12.dp)
            )
            .then(
                if (enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha)
        )
    }
}

private const val MIN_DURATION = 1
private const val MAX_DURATION = 120
private const val STEP_SIZE = 5

// ── Previews ──

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D1B2A,
    widthDp = 360,
    name = "PixelDurationPicker – Default (25 min)"
)
@Composable
private fun PixelDurationPickerDefault() {
    FlowTimerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceBackground),
            contentAlignment = Alignment.Center
        ) {
            PixelDurationPickerContent(selectedMinutes = 25)
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D1B2A,
    widthDp = 360,
    name = "PixelDurationPicker – Min (1 min)"
)
@Composable
private fun PixelDurationPickerMin() {
    FlowTimerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceBackground),
            contentAlignment = Alignment.Center
        ) {
            PixelDurationPickerContent(selectedMinutes = 1)
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D1B2A,
    widthDp = 360,
    name = "PixelDurationPicker – Max (120 min)"
)
@Composable
private fun PixelDurationPickerMax() {
    FlowTimerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceBackground),
            contentAlignment = Alignment.Center
        ) {
            PixelDurationPickerContent(selectedMinutes = 120)
        }
    }
}
