package com.ashutosh.flowtimer

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Minimal Wear OS activity required for Play Store listing.
 * Primary user interaction is via the Flow Time Tile, not this activity.
 */
class WearMainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val label = TextView(this).apply {
            text = "Add the Flow Time\ntile to your watch face"
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(32, 0, 32, 0)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF0D1B2A.toInt())
            addView(label)
        }

        setContentView(root)
    }
}
