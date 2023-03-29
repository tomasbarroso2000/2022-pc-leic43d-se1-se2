package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

const val MIN_GROUP_SIZE_VALUE = 2

class NAryExchanger<T>(private val groupSize: Int) {
    private val log = LoggerFactory.getLogger(NAryExchanger::class.java)
    private val mLock: Lock = ReentrantLock()
    private val requests: MutableList<Request<T>> = mutableListOf()

    private class Request<T>(
        val condition: Condition,
        val value: T,
    ) {
        var isDone: Boolean = false
        var values: MutableList<T> = mutableListOf()
    }

    @Throws(InterruptedException::class)
    fun exchange(value: T, timeout: Duration): List<T>? {

        mLock.withLock {

            if (groupSize <= MIN_GROUP_SIZE_VALUE ||
                timeout.isZero
            ) return null

            //fast path
            if (requests.size == groupSize - 1) {
                val values = computeValues(value)
                (0 until groupSize - 1).forEach { index ->
                    requests[index].values = values
                    requests[index].condition.signal()
                }
                return values
            }

            val myRequest: Request<T> = Request(mLock.newCondition(), value)
            requests.add(myRequest)

            var remainingTime: Long = timeout.inWholeNanoseconds

            while (true) {
                try {
                    remainingTime = myRequest.condition.awaitNanos(remainingTime)

                    if (myRequest.isDone) {
                        requests.remove(myRequest)
                        return myRequest.values
                    }

                    if (remainingTime <= 0) {
                        // giving-up
                        requests.remove(myRequest)
                        return null
                    }

                } catch (e: InterruptedException) {
                    if (myRequest.isDone) {
                        Thread.currentThread().interrupt()
                        requests.remove(myRequest)
                        requests.firstOrNull()?.condition?.signal()
                        return myRequest.values
                    }

                    requests.firstOrNull()?.condition?.signal()
                    throw e
                }
            }
        }

    }

    private fun computeValues(value: T): MutableList<T> =
        (0 until groupSize - 1)
            .map { index ->
                requests[index].isDone = true
                requests[index].value
            }
            .toMutableList()
            .also { list -> list.add(value) }

}