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
        var values: MutableList<T> = mutableListOf(),
        val value: T
    )

    private var requests = mutableListOf<Request<T>>()
    private val waiting = mutableListOf<Request<T>>()

    @Throws(InterruptedException::class)
    fun exchange(value: T, timeout: Duration): List<T>? {
        mLock.withLock {

            if (availableGroupSize <= MIN_GROUP_SIZE_VALUE) return null

            if (timeout.isZero) {
                return null
            }

            val request = Request(value = value)

            requests.add(request)

            log.info("${requests.size}")

            //fast-path
            if (requests.size == availableGroupSize) {
                log.info("fast-path")
                val temp = requests.map{it.value}.toMutableList()

                repeat(availableGroupSize) { index ->
                    requests[index].values = temp
                }

                repeat(availableGroupSize) { index ->
                    requests = mutableListOf<Request<T>>()
                }

                //requests.remove(request)
                mCondition.signalAll()
                return request.values
            }

            val dueTime = timeout.dueTime()

            while (true) {
                try {

                    mCondition.await()

                    if(request.values.size == availableGroupSize) {
                        //requests.remove(request)
                        log.info("${requests.size}")
                        //mCondition.signalAll()
                        return request.values
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
