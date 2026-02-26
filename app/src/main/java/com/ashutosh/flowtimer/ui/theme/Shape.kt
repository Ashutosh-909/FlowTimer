package com.ashutosh.flowtimer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Standard border radii for the pixel-art UI. */
val PixelShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),   // containers, hourglass card
    large = RoundedCornerShape(24.dp)     // timer readout pill
)

/** Grid unit — all spacing should be a multiple of this. */
const val GRID_UNIT_DP = 4
