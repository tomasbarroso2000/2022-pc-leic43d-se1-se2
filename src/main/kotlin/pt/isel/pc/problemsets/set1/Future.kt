package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
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

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = lock.withLock {
        if (state != State.ACTIVE) return false

        state = State.CANCELLED

        if (mayInterruptIfRunning) thread?.interrupt()

        setError(CancellationException(), State.CANCELLED)
        true
    }

    private fun set(value: V) = lock.withLock {
        check(state == State.ACTIVE)
        this.value = value
        state = State.COMPLETED
        condition.signalAll()
    }

    private fun setError(e: Exception, state: State) = lock.withLock {
        error = e
        this.state = state
        condition.signalAll()
    }

    private fun start() = lock.withLock {
        check(state == State.ACTIVE)
        thread = thread {
            try {
                set(callable.call())
            } catch (ee: ExecutionException) {
                setError(ee, State.ERROR)
            } catch (ie: InterruptedException) {
                if (state == State.CANCELLED) setError(CancellationException(), State.CANCELLED)
                else setError(ie, State.ERROR)
            } catch (e: Exception) {
                setError(e, State.ERROR)
            }
        }
    }

    override fun isCancelled(): Boolean = lock.withLock {
        return state == State.CANCELLED
    }

    override fun isDone(): Boolean = lock.withLock {
        return state != State.ACTIVE
    }

    private fun get(timeout : Duration): V {
        lock.withLock {
            //fast path
            if (state == State.COMPLETED)
                return value ?: throw error as Throwable

            if (state == State.CANCELLED)
                throw CancellationException()

            //wait path
            var remainingTime = timeout.inWholeNanoseconds

            while (true) {
                try {
                    remainingTime = condition.awaitNanos(remainingTime)

                    if (state == State.COMPLETED)
                        return value ?: throw error as Throwable

                    if (state == State.CANCELLED || state == State.ERROR)
                        throw error as Throwable

                    if (remainingTime <= 0)
                        throw TimeoutException("Timeout")

                } catch (e: InterruptedException) {
                    if (state == State.COMPLETED) {
                        Thread.currentThread().interrupt()
                        return value ?: throw error as Throwable
                    }

                    if (state == State.CANCELLED || state == State.ERROR)
                        throw error as Throwable

                    throw e
                }
            }
        }
    }

    override fun get(timeout: Long, unit: TimeUnit): V =
        get(unit.toMillis(timeout).toDuration(DurationUnit.MILLISECONDS))

    override fun get(): V = get(Duration.INFINITE)

}
