package com.example.cyclistweather.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Holds a single coroutine [Job], cancelling any previous one before starting the next.
 * Replaces the hand-rolled `private var job: Job?` + `job?.cancel()` pattern.
 */
class LatestJob {
    private var job: Job? = null

    fun launch(scope: CoroutineScope, block: suspend CoroutineScope.() -> Unit) {
        job?.cancel()
        job = scope.launch(block = block)
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
