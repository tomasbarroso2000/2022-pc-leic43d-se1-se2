package pt.isel.pc.problemsets.set2

import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.ThreadPoolExecutor
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.jvm.Throws
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class CyclicBarrier(
    private val parties: Int,
    private val barrierAction: Runnable? = null
) {
    init { require(parties > 0) { "parties must be higher than 0" } }

    private var totalCyclicItems = 0

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    private enum class State { ACTIVE, COMPLETED, BROKEN }
    private var state = State.ACTIVE

    @Throws(InterruptedException::class, BrokenBarrierException::class, TimeoutException::class)
    fun await(timeout: Long, unit: TimeUnit): Int = lock.withLock {
        await(unit.toMillis(timeout).toDuration(DurationUnit.MILLISECONDS))
    }

    @Throws(InterruptedException::class, BrokenBarrierException::class)
    fun await(): Int = lock.withLock {
        await(Duration.INFINITE)
    }

    val getNumberWaiting: Int
        get() = lock.withLock { totalCyclicItems }

    val getParties: Int
        get() = parties

    val isBroken: Boolean
        get() = lock.withLock { state == State.BROKEN }

    fun reset() = lock.withLock {
        if (state == State.ACTIVE) {
            state = State.BROKEN
            condition.signalAll()
        } else {
            totalCyclicItems = 0
            state = State.ACTIVE
        }
    }

    private fun await(timeout: Duration): Int {
        lock.withLock {
            //fast path
            if (state == State.BROKEN) throw BrokenBarrierException()

            val id = parties - totalCyclicItems - 1
            var remainingTime = timeout.inWholeNanoseconds

            if (remainingTime <= 0) {
                barrierAction?.let { safeRun(it) }
                return id
            }

            if (totalCyclicItems == parties - 1) {
                state = State.COMPLETED
                condition.signalAll()
                totalCyclicItems -= parties - 1
                barrierAction?.let { safeRun(it) }
                return id
            }

            //wait path

            totalCyclicItems++

            while (true) {
                try {
                    remainingTime = condition.awaitNanos(remainingTime)

                    if (state == State.COMPLETED) return id
                    if (state == State.BROKEN) throw BrokenBarrierException()
                    if (remainingTime <= 0) throw TimeoutException()

                } catch (ie: InterruptedException) {
                    if (state == State.COMPLETED){
                        Thread.currentThread().interrupt()
                        return id
                    }
                    throw ie
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThreadPoolExecutor::class.java)

        private fun safeRun(runnable: Runnable) {
            try {
                runnable.run()
            } catch (ex: Throwable) {
                logger.warn("Unexpected exception while running work item, ignoring it")
            }
        }
    }
}