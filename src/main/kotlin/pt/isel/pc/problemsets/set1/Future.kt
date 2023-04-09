package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private val log = LoggerFactory.getLogger(Future::class.java)

class Future<V>(val callable : Callable<V>) : Future<V> {
    companion object {
        fun <V> execute(callable : Callable<V>) : Future<V> {
            val fut = Future(callable)
            log.info("execute")
            fut.start()
            return fut
        }
    }

    private val monitor = ReentrantLock()
    private val done = monitor.newCondition()

    private var thread : Thread? = null
    private var value : V? = null
    private var error : Exception? = null

    private enum class State { ACTIVE, COMPLETED, CANCELLED, ERROR }

    private var state = State.ACTIVE

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        monitor.withLock {
            if (state == State.COMPLETED) return false
            if (mayInterruptIfRunning) {
                state = State.CANCELLED
                done.signalAll()
                return true
            } else {
                thread?.interrupt()
                return true
            }
        }
    }

    private fun set(value : V) {
        monitor.withLock {
            if (state == State.COMPLETED) throw IllegalStateException()
            this.value = value
            state = State.COMPLETED
            log.info("done and correct")
            done.signalAll()
        }
    }

    private fun setError(e : Exception) {
        monitor.withLock {
            if (state == State.COMPLETED) throw IllegalStateException()
            this.error = e
            state = State.ERROR
            done.signalAll()
        }
    }

    private fun start() {
        monitor.withLock {
            thread = Thread {
                try {
                    log.info("start")
                    set(callable.call())
                } catch (e: Exception) {
                    setError(e)
                }
            }.apply { start() }
        }
    }

    override fun isCancelled(): Boolean {
        monitor.withLock {
            return state == State.CANCELLED
        }
    }

    override fun isDone(): Boolean {
        monitor.withLock {
            return state != State.ACTIVE
        }
    }

    private fun get(timeout : Duration) : V {
        monitor.withLock {
            if (state == State.COMPLETED)
                return value ?: throw error as Throwable

            var remainingTime = timeout.inWholeNanoseconds
            while (true) {
                try {
                    remainingTime = done.awaitNanos(remainingTime)

                    if (state == State.COMPLETED)
                        return value ?: throw error as Throwable

                    if (remainingTime <= 0)
                        throw TimeoutException("Timeout")

                } catch (e: InterruptedException) {
                    if (state == State.COMPLETED) {
                        Thread.currentThread().interrupt()
                        return value ?: throw error as Throwable
                    }
                    setError(e)
                    throw e
                }
            }
        }
    }

    override fun get(timeout: Long, unit: TimeUnit): V {
        return get(unit.toMillis(timeout).toDuration(DurationUnit.MILLISECONDS))
    }

    override fun get(): V {
        return get(Duration.INFINITE)
    }

}
