package com.ashutosh.flowtimer

/** Returns current wall-clock time in milliseconds since Unix epoch. */
expect fun currentEpochMillis(): Long

/** Returns monotonic elapsed time in milliseconds (for drift-proof timer anchoring). */
expect fun elapsedRealtimeMillis(): Long
