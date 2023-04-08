package pt.isel.pc.problemsets.set1

import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class Future<V>(val callable : Callable<V>) : Future<V> {
    companion object {
        fun <V> execute(callable : Callable<V>) : Future<V> {
            val fut = Future(callable)
            fut.start()
            return fut
        }
    }

    private val mLock = ReentrantLock()
    private val futureCondition = mLock.newCondition()

    private var thread : Thread? = null
    private var value : V? = null
    private var error : Exception? = null

    private enum class State { ACTIVE, COMPLETED, CANCELLED, ERROR }

    private var state = State.ACTIVE

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        mLock.withLock {
            if (state == State.ACTIVE && mayInterruptIfRunning) {
                state = State.CANCELLED
                futureCondition.signal()
            } else if(state == State.ACTIVE && mayInterruptIfRunning)
        }
    }

    private fun set(value : V) {
        mLock.withLock {
            TODO()
        }
    }

    private fun setError(e : Exception) {
        mLock.withLock {
            TODO()
        }
    }

    private fun start() {
        TODO()
    }

    override fun isCancelled(): Boolean {
        mLock.withLock {
            return state == State.CANCELLED
        }
    }

    override fun isDone(): Boolean {
        mLock.withLock {
            return state != State.ACTIVE
        }
    }

    private fun get(timeout : Duration) : V {
        mLock.withLock {
            var remainingTime = timeout.inWholeNanoseconds
            while (true) {
                try {
                    remainingTime = futureCondition.awaitNanos(remainingTime)

                    val currValue = value

                    if ((state == State.COMPLETED || state == State.CANCELLED) && currValue != null) {
                        return currValue
                    }

                    if (remainingTime <= 0) throw TimeoutException("future timed out")
                } catch (e: InterruptedException) {
                    TODO()
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