package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.*
import java.util.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

const val MIN_GROUP_SIZE_VALUE = 2


class NAryExchanger<T>(private val groupSize: Int) {

    init {
        require(groupSize >= MIN_GROUP_SIZE_VALUE) {"Number of group size must be greater or equal than two" }
    }

    companion object {
        private val log = LoggerFactory.getLogger(NAryExchanger::class.java)
    }

    private val mLock: Lock = ReentrantLock()
    private val requests: LinkedList<Request<T>> = LinkedList()

    private class Request<T>(val condition: Condition, val value: T) {
        var isDone: Boolean = false
        var values: List<T> = listOf()
    }

    @Throws(InterruptedException::class)
    fun exchange(value: T, timeout: Duration): List<T>? {
        mLock.withLock {
            require(!timeout.isZero) {"Timeout must be higher than zero"}

            val myRequest: Request<T> = Request(mLock.newCondition(), value)
            requests.add(myRequest)

            //fast path
            if (requests.size == groupSize) {
                val values = computeValues()
                repeat(groupSize - 1) {
                    val request = requests.poll()
                    request.values = values
                    request.isDone = true
                    request.condition.signal()
                }
                requests.remove(myRequest)
                return values
            }

            var remainingTime: Long = timeout.inWholeNanoseconds

            while (true) {
                try {
                    remainingTime = myRequest.condition.awaitNanos(remainingTime)

                    if (myRequest.isDone) return myRequest.values

                    if (remainingTime <= 0) {
                        // giving-up
                        requests.remove(myRequest)
                        return null
                    }

                } catch (e: InterruptedException) {
                    if (myRequest.isDone) {
                        Thread.currentThread().interrupt()
                        return myRequest.values
                    }
                    requests.remove(myRequest)
                    throw e
                }
            }
        }

    }

    private fun computeValues(): List<T> =
        (0 until groupSize).map { index -> requests[index].value }

}
