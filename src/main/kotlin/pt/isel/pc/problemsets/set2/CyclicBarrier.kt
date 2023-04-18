package pt.isel.pc.problemsets.set2

import pt.isel.pc.problemsets.set1.Future
import java.util.LinkedList
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class CyclicBarrier(private val initialParties: Int) {

    init {
        require(initialParties > 0) { "parties must be higher than 0" }
    }

    private val lock = ReentrantLock()
    private val requests = LinkedList<Request>()
    private class Request(val condition: Condition) { var isDone: Boolean = false }
    private var state = State.ACTIVE

    private enum class State { ACTIVE, COMPLETED, BROKEN }

    fun await(): Int {
        TODO()
    }

    fun await(timeout: Long, unit: TimeUnit): Int {
        TODO()
    }

    fun getNumberWaiting(): Int = lock.withLock { requests.size }

    fun getParties(): Int = initialParties

    fun isBroken(): Boolean =  lock.withLock { state == State.BROKEN }

    fun reset() {
        if (state == State.COMPLETED) {

        }
    }

}