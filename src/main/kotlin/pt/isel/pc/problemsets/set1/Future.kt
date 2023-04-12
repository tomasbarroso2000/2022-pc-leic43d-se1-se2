package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.isZero
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class Future<V>(private val callable : Callable<V>) : Future<V> {

    companion object {
        fun <V> execute(callable: Callable<V>): pt.isel.pc.problemsets.set1.Future<V> {
            val fut = Future(callable)
            fut.start()
            return fut
        }

        private val log = LoggerFactory.getLogger(Future::class.java)
    }

    private enum class State { ACTIVE, COMPLETED, CANCELLED, ERROR }

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    private var thread : Thread? = null
    private var value : V? = null
    private var error : Exception? = null
    private var state = State.ACTIVE

    private fun set(value: V) = lock.withLock {
        check(state == State.ACTIVE)
        this.value = value
        state = State.COMPLETED
        condition.signalAll()
    }

    private fun setError(e: Exception) = lock.withLock {
        error = e
        this.state = State.ERROR
        condition.signalAll()
    }

    private fun start() {
        thread = Thread {
            try {
                set(callable.call())
            } catch (e: Exception) {
                setError(e)
            }
        }
    }

    private fun checkState(timeout: Long) = lock.withLock {
        if (state == State.ERROR) throw ExecutionException(error)

        if (state == State.CANCELLED) throw CancellationException()

        if (timeout <= 0) throw TimeoutException()
    }

    private fun get(timeout : Duration): V {
        lock.withLock {
            //fast path
            if (state == State.COMPLETED) return value ?: throw error as Throwable

            checkState(timeout.inWholeNanoseconds)

            //wait path
            var remainingTime = timeout.inWholeNanoseconds

            while (true) {
                try {
                    thread?.start()

                    remainingTime = condition.awaitNanos(remainingTime)

                    if (state == State.COMPLETED) return value ?: throw error as Throwable

                    checkState(remainingTime)

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
