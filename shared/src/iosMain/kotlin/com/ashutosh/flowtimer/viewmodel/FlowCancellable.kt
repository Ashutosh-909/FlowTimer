package com.ashutosh.flowtimer.viewmodel

import kotlinx.coroutines.Job

/** Handle returned from observation methods so Swift can cancel the subscription. */
class FlowCancellable(private val job: Job) {
    fun cancel() = job.cancel()
}
