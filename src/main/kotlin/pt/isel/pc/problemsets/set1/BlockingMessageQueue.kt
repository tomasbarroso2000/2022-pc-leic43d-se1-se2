package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.isZero
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class BlockingMessageQueue<T>(private val capacity: Int) {
    private val log = LoggerFactory.getLogger(BlockingMessageQueue::class.java)
    private val mLock = ReentrantLock()

    private val enqueue: MutableList<EnqueueRequest<T>> = mutableListOf()
    private val dequeue: MutableList<DequeueRequest> = mutableListOf()
    private val messages: MutableList<T> = mutableListOf()

    private class DequeueRequest(
        val condition: Condition,
        val nOfMessages: Int = 0,
    ) { var isDone: Boolean = false }

    private class EnqueueRequest<T>(
        val condition: Condition,
        val message: T,
    ) { var isDone: Boolean = false }

    @Throws(InterruptedException::class)
    fun tryEnqueue(message: T, timeout: Duration): Boolean {
        mLock.withLock {
            if (timeout.isZero || capacity <= 0) return false

            //fast path
            if (enqueue.isEmpty() && messages.size < capacity) {
                messages.add(message)
                signalDequeue()
                return true
            }

            //wait path
            val myRequest = EnqueueRequest(mLock.newCondition(), message)
            enqueue.add(myRequest)

            var remainingNanos: Long = timeout.inWholeNanoseconds

            while (true) {
                try {
                    remainingNanos = myRequest.condition.awaitNanos(remainingNanos)

                    if (myRequest.isDone) {
                        enqueue.remove(myRequest)
                        signalDequeue()
                        return true
                    }

                    if (remainingNanos <= 0) {
                        // giving-up
                        enqueue.remove(myRequest)
                        return false
                    }

                } catch (e: InterruptedException) {
                    enqueue.remove(myRequest)

                    if (myRequest.isDone) {
                        Thread.currentThread().interrupt()
                        signalDequeue()
                        return true
                    }

                    signalDequeue()
                    throw e
                }
            }
        }
    }

    @Throws(InterruptedException::class)
    fun tryDequeue(nOfMessages: Int, timeout: Duration): List<T>? {
        mLock.withLock {
            if (timeout.isZero || nOfMessages <= 0 || capacity <= 0) return null

            //fast-path
            if (dequeue.isEmpty() && messages.size >= nOfMessages) {
                signalEnqueue()
                return computeMessages(nOfMessages)
            }

            //wait path
            val myRequest = DequeueRequest(mLock.newCondition(), nOfMessages)
            dequeue.add(myRequest)

            var remainingNanos: Long = timeout.inWholeNanoseconds

            while (true) {
                try {
                    remainingNanos = myRequest.condition.awaitNanos(remainingNanos)

                    if (myRequest.isDone) {
                        dequeue.remove(myRequest)
                        signalEnqueue()
                        return computeMessages(nOfMessages)
                    }

                    if (remainingNanos <= 0) {
                        // giving-up
                        dequeue.remove(myRequest)
                        return null
                    }

                } catch (e: InterruptedException) {
                    dequeue.remove(myRequest)

                    if (myRequest.isDone) {
                        Thread.currentThread().interrupt()
                        signalEnqueue()
                        return computeMessages(nOfMessages)
                    }

                    signalEnqueue()
                    throw e
                }
            }
        }
    }

    private fun signalDequeue() {
        dequeue.firstOrNull()?.let { request ->
            if (request.nOfMessages <= messages.size) {
                request.isDone = true
                request.condition.signal()
            }
        }
    }

    private fun signalEnqueue() {
        enqueue.firstOrNull()?.let { request ->
            if (messages.size < capacity) {
                request.isDone = true
                messages.add(request.message)
                request.condition.signal()
            }
        }
    }

    private fun computeMessages(nOfMessages: Int): MutableList<T> {
        val temp: MutableList<T> = mutableListOf()
        repeat(nOfMessages) {
            messages.removeFirstOrNull()?.let { temp.add(it) }
        }
        return temp
    }
}