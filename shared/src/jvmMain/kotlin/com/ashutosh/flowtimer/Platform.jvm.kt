package com.ashutosh.flowtimer

actual fun currentEpochMillis(): Long = System.currentTimeMillis()

/** JVM monotonic clock. On Android the service uses SystemClock directly; this is only for compilation. */
actual fun elapsedRealtimeMillis(): Long = System.nanoTime() / 1_000_000L
