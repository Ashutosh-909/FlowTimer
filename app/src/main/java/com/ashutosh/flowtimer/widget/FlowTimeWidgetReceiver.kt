package com.ashutosh.flowtimer.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Broadcast receiver that registers [FlowTimeWidget] with the system.
 *
 * Declared in `AndroidManifest.xml` with the `appwidget-provider` metadata
 * pointing to `res/xml/flow_time_widget_info.xml`.
 */
class FlowTimeWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = FlowTimeWidget()
}
