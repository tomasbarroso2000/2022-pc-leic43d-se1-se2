package pt.isel.pc.problemsets.set2

import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

private val lock = ReentrantLock()

private val log = LoggerFactory.getLogger(CompletableFuture::class.java)

private class Data<T>(val fut: CompletableFuture<T>, val index: Int) {
    var result: T? = null
    var exception: Exception? = null
}

fun <T> any(futures: List<CompletableFuture<T>>): CompletableFuture<T> {
    require(futures.isNotEmpty()) { "list cannot be empty" }

    val data: AtomicReference<List<Data<T>>> = AtomicReference(makeList(futures))

    while (true) {
        val observedData = data.get()
        val futureDone = observedData.firstOrNull { it.result != null }
        val futuresWithError = observedData.filter { it.exception != null }
        if (observedData == data.get() && futureDone != null) {
            return CompletableFuture.completedFuture(futureDone.result)
        }
        if (observedData == data.get() && futuresWithError.size == observedData.size) {
            observedData.map { it.exception }.forEach {
                log.info("$it")
            }
            return completeWithExceptions(observedData)
        }
    }
}

private fun <T> makeList(futures: List<CompletableFuture<T>>): List<Data<T>> =
    (futures.indices).map { index ->
        val data = Data(futures[index], index)
        thread {
            try {
                val result = data.fut.get()
                data.result = result
            } catch (e: Exception) {
                data.exception = e
            }
        }
        data
    }

private fun <T> completeWithExceptions(list: List<Data<T>>): CompletableFuture<T> {
    val exceptions: List<Exception?> = list.map { it.exception }
    return CompletableFuture.completedFuture(Unit)
        .handle { _, throwable ->
            throw CompletionException(exceptions.joinToString(", "), throwable)
        }
}