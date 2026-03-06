package com.ashutosh.flowtimer

import platform.Foundation.NSDate
import platform.Foundation.NSProcessInfo

actual fun currentEpochMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1_000.0).toLong()

actual fun elapsedRealtimeMillis(): Long =
    (NSProcessInfo.processInfo.systemUptime * 1_000.0).toLong()
