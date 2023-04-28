package pt.isel.pc.problemsets.set2

import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.ThreadPoolExecutor
import java.util.LinkedList
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.jvm.Throws
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class CyclicBarrier(private val parties: Int, private val barrierAction: Runnable? = null) {

    init { require(parties > 0) { "parties must be higher than 0" } }

    private val lock = ReentrantLock()
    private val requests = LinkedList<Request>()
    private class Request(
        val id: Int,
        val condition: Condition,
        val runnable: Runnable? = null
    ) {
        var isDone: Boolean = false
        var isBroken: Boolean = false
    }
    private var state = State.ACTIVE

    private enum class State { ACTIVE, COMPLETED, BROKEN }

    @Throws(InterruptedException::class, BrokenBarrierException::class, TimeoutException::class)
    fun await(timeout: Long, unit: TimeUnit): Int = lock.withLock {
        await(unit.toMillis(timeout).toDuration(DurationUnit.MILLISECONDS))
    }

    @Throws(InterruptedException::class, BrokenBarrierException::class)
    fun await(): Int = lock.withLock {
        await(Duration.INFINITE)
    }

    fun getNumberWaiting(): Int = lock.withLock { requests.size }

    fun getParties(): Int = parties

    fun isBroken(): Boolean =  lock.withLock { state == State.BROKEN }

    fun reset() = lock.withLock {
        if (state == State.ACTIVE) {
            requests.forEach {
                it.isBroken = true
                it.condition.signal()
            }
        }
        requests.clear()
        state = State.ACTIVE
    }

    private fun await(timeout: Duration): Int {
        lock.withLock {
            //fast path
            if (state == State.BROKEN) throw BrokenBarrierException()

            val id = parties - requests.size - 1
            var remainingTime = timeout.inWholeNanoseconds

            if (remainingTime <= 0 ) {
                barrierAction?.let { safeRun(it) }
                return id
            }

            if (requests.size == parties - 1) {
                state = State.COMPLETED
                signalAll()
                reset()
                barrierAction?.let { safeRun(it) }
                return id
            }

            //wait path

            val myRequest = Request(id, lock.newCondition(), barrierAction)
            requests.add(myRequest)

            while (true) {
                try {
                    remainingTime = myRequest.condition.awaitNanos(remainingTime)

                    if (myRequest.isDone) return myRequest.id
                    if (myRequest.isBroken) throw BrokenBarrierException()
                    if (remainingTime <= 0) throw TimeoutException()

                } catch (ie: InterruptedException) {
                    if (myRequest.isDone) return myRequest.id
                    throw ie
                }
            }
        }
    }

    private fun signalAll() = lock.withLock {
        (0 until parties - 1).forEach { index ->
            val req = requests[index]
            req.isDone = true
            req.condition.signal()
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