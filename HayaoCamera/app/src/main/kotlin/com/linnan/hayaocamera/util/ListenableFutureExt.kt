package com.linnan.hayaocamera.util

import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val directExecutor = Executor { it.run() }

suspend fun <T> ListenableFuture<T>.awaitFuture(): T = suspendCancellableCoroutine { cont ->
    addListener(
        {
            try {
                cont.resume(get())
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        },
        directExecutor
    )
    cont.invokeOnCancellation { cancel(false) }
}
