package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class Future<V> : Future<V> {

    companion object {
        private val log = LoggerFactory.getLogger(Future::class.java)
    }

    private enum class State { ACTIVE, COMPLETED, CANCELLED, ERROR }

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    private var thread : Thread? = null
    private var value : V? = null
    private var error : Exception? = null
    private var state = State.ACTIVE

    internal fun set(value: V) = lock.withLock {
        if (state == State.ACTIVE) {
            this.value = value
            state = State.COMPLETED
            condition.signalAll()
        }
    }

    internal fun setError(e: Exception) = lock.withLock {
        if (state == State.ACTIVE) {
            this.error = e
            this.state = State.ERROR
            condition.signalAll()
        }
    }

    private fun tryGetResult(timeout: Long) : V? {
        if (state == State.COMPLETED) return value
        if (state === State.ERROR) throw ExecutionException(error)
        if (state == State.CANCELLED) throw CancellationException()
        if (timeout <= 0) throw TimeoutException()
        return null
    }

    private fun get(timeout: Duration): V {
        lock.withLock {
            //fast path
            val res = tryGetResult(timeout.inWholeNanoseconds)
            if (res != null) return res

            //wait path
            var remainingTime = timeout.inWholeNanoseconds

            while (true) {
                try {
                    remainingTime = condition.awaitNanos(remainingTime)

                    val res = tryGetResult(remainingTime)

                    if (res != null) return res

                } catch (e: InterruptedException) {
                    if (state == State.COMPLETED) {
                        Thread.currentThread().interrupt()
                        return value ?: throw error as Throwable
                    }
                    throw e
                }
            }
        }
    }

    override fun isCancelled(): Boolean = lock.withLock {
        return state == State.CANCELLED
    }

    override fun isDone(): Boolean = lock.withLock {
        return state != State.ACTIVE
    }

    override fun get(timeout: Long, unit: TimeUnit): V =
        get(unit.toMillis(timeout).toDuration(DurationUnit.MILLISECONDS))

    override fun get(): V = get(Duration.INFINITE)

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = lock.withLock {
        if (state != State.ACTIVE) return false

        state = State.CANCELLED

        if (mayInterruptIfRunning) thread?.interrupt()

        condition.signal()
        true
    }

}
