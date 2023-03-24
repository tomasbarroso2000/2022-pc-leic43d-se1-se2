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

    private class Request<T>(
        var isDone: Boolean = false,
        val value: T
    )

    private val requests = mutableListOf<Request<T>>()
    private val values = mutableListOf<T>()

    @Throws(InterruptedException::class)
    fun exchange(value: T, timeout: Duration): List<T>? {
        mLock.withLock {

            if (availableGroupSize <= MIN_GROUP_SIZE_VALUE) return null

            if (timeout.isZero) {
                return null
            }

            val request = Request(value = value)
            requests.add(request)
            values.add(value)
            //fast-path
            if (requests.size >= availableGroupSize) {

                repeat(availableGroupSize) { index ->
                    values[index] = requests[index].value
                    requests[index].isDone = true
                }

                mCondition.signalAll()
                requests.remove(request)
                return values
            }

            val dueTime = timeout.dueTime()

            while (true) {
                try {
                    mCondition.await(dueTime)

                    if(request.isDone) {
                        return values
                    }

                    if (dueTime.isPast) return null
                } catch(e: InterruptedException) {
                    if(requests.isNotEmpty()) mCondition.signalAll()
                    throw e
                }

            }

        }
    }
}