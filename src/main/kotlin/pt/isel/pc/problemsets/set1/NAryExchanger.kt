package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

const val MIN_GROUP_SIZE_VALUE = 2

class NAryExchanger<T>(groupSize: Int) {

    private val log = LoggerFactory.getLogger(NAryExchanger::class.java)
    private val mLock: Lock = ReentrantLock()
    private val mCondition: Condition = mLock.newCondition()
    private var availableGroupSize: Int = groupSize
    private var requests = mutableListOf<Request>()
    private var values: MutableList<T> = mutableListOf()

    private class Request(
        var isDone: Boolean = false
    )

    @Throws(InterruptedException::class)
    fun exchange(value: T, timeout: Duration): List<T>? {
        mLock.withLock {

            if (availableGroupSize <= MIN_GROUP_SIZE_VALUE ||
                timeout.isZero
            ) return null

            val request = Request()
            requests.add(request)
            values.add(value)

            //fast-path
            if (requests.size == availableGroupSize) {

                repeat(availableGroupSize) { index ->
                    requests[index].isDone = true
                }

                requests.clear()

                mCondition.signalAll()
                return values
            }

            //waiting path
            var remainingNanos: Long = timeout.inWholeNanoseconds

            while (true) {
                try {
                    remainingNanos = mCondition.awaitNanos(remainingNanos)

                    if(request.isDone) {
                        return values
                    }

                    values = values.subList(availableGroupSize, values.size - 1)
                } catch(e: InterruptedException) {
                    requests.remove(request)
                    if(requests.isNotEmpty()) mCondition.signalAll()
                    throw e
                }

                if (remainingNanos <= 0) {
                    // giving-up
                    requests.remove(request)
                    return null
                }

            }

        }
    }
}