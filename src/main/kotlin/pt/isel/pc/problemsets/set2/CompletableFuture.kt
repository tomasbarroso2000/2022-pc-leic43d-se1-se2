package pt.isel.pc.problemsets.set2

import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

class CustomException(val exceptions: List<Throwable>): Exception()

private class Data<T>(
    val success: CompletableFuture<T>? = null,
    val failures: List<Throwable> = mutableListOf()
)

fun <T> any(futures: List<CompletableFuture<T>>): CompletableFuture<T> {
    require(futures.isNotEmpty()) { "futures list cannot be empty" }

    val cf = CompletableFuture<T>()
    val data: AtomicReference<Data<T>> = AtomicReference(Data())

    futures.forEach { fut ->
        fut.whenCompleteAsync { v, e ->
            do {
                val observedData = data.get()
                val newErrors = observedData.failures + e
                if (e != null && data.compareAndSet(observedData, Data(null, newErrors))) {
                    if (newErrors.size == futures.size)
                        cf.completeExceptionally(CustomException(newErrors))
                    break
                }
                if (v != null && observedData.success == null && data.compareAndSet(observedData, Data(success = fut))) {
                    cf.complete(v)
                    futures.filter { it != fut }.forEach { it.cancel(true) }
                    break
                }
            } while (!Thread.currentThread().isInterrupted && !data.compareAndSet(observedData, observedData))
        }
    }

    if (Thread.currentThread().isInterrupted) {
        futures.forEach { it.cancel(true) }
        throw InterruptedException("Thread interrupted while waiting for futures to complete")
    }

    return cf
}