package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.isZero
import java.util.LinkedList
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class BlockingMessageQueue<T>(private val capacity: Int) {

    init {
        require(capacity > 0) {"capacity must be higher than zero"}
    }

    companion object {
        private val log = LoggerFactory.getLogger(BlockingMessageQueue::class.java)
    }

    private val lock = ReentrantLock()

    private val enqueue: LinkedList<EnqueueRequest<T>> = LinkedList<EnqueueRequest<T>>()
    private val dequeue: LinkedList<DequeueRequest> = LinkedList<DequeueRequest>()
    private val messages: LinkedList<T> = LinkedList<T>()

    private class DequeueRequest(val condition: Condition, val nOfMessages: Int = 0) {
        var isDone: Boolean = false
    }

    private class EnqueueRequest<T>(val condition: Condition, val message: T) {
        var isDone: Boolean = false
    }

    @Throws(InterruptedException::class)
    fun tryEnqueue(message: T, timeout: Duration): Boolean {
        lock.withLock {
            require(!timeout.isZero) {"timeout must be higher than zero"}

            //fast path
            if (enqueue.isEmpty() && messages.size < capacity) {
                messages.add(message)
                signalDequeue()
                return true
            }

            //wait path
            val myRequest = EnqueueRequest(lock.newCondition(), message)
            enqueue.add(myRequest)

            var remainingNanos: Long = timeout.inWholeNanoseconds

            while (true) {
                try {
                    remainingNanos = myRequest.condition.awaitNanos(remainingNanos)

                    if (myRequest.isDone) return true

                    if (remainingNanos <= 0) {
                        // giving-up
                        enqueue.remove(myRequest)
                        return false
                    }

                } catch (e: InterruptedException) {
                    if (myRequest.isDone) {
                        Thread.currentThread().interrupt()
                        return true
                    }
                    enqueue.remove(myRequest)
                    signalDequeue()
                    throw e
                }
            }
        }
    }

    @Throws(InterruptedException::class)
    fun tryDequeue(nOfMessages: Int, timeout: Duration): List<T>? {
        lock.withLock {
            require(!timeout.isZero && nOfMessages > 0 && nOfMessages <= capacity) {"timeout must be higher than zero and number of messages must be higher than 0 and lower or equal than the capacity"}

            //fast-path
            if (dequeue.isEmpty() && messages.size >= nOfMessages) {
                signalEnqueue()
                return computeMessages(nOfMessages)
            }

            //wait path
            val myRequest = DequeueRequest(lock.newCondition(), nOfMessages)
            dequeue.add(myRequest)

            var remainingNanos: Long = timeout.inWholeNanoseconds

            while (true) {
                try {
                    remainingNanos = myRequest.condition.awaitNanos(remainingNanos)

                    if (myRequest.isDone) return computeMessages(nOfMessages)

                    if (remainingNanos <= 0) {
                        // giving-up
                        dequeue.remove(myRequest)
                        return null
                    }

                } catch (e: InterruptedException) {

                    if (myRequest.isDone) {
                        Thread.currentThread().interrupt()
                        return computeMessages(nOfMessages)
                    }
                    dequeue.remove(myRequest)
                    signalEnqueue()
                    throw e
                }
            }
        }
    }

    private fun signalDequeue() = lock.withLock {
        dequeue.peekFirst()?.let {
            if (it.nOfMessages <= messages.size) {
                val request = dequeue.poll()
                request.isDone = true
                request.condition.signal()
            }
        }
    }

    private fun signalEnqueue() = lock.withLock {
        enqueue.peekFirst()?.let {
            if (messages.size < capacity) {
                messages.add(it.message)
                val request = enqueue.poll()
                request.isDone = true
                request.condition.signal()
            }
        }
    }

    private fun computeMessages(nOfMessages: Int): List<T> = lock.withLock {
        (0 until nOfMessages).map { messages.poll() }
    }
}