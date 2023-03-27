package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.NodeLinkedList
import pt.isel.pc.problemsets.set1.utils.isZero
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class BlockingMessageQueue<T>(private val capacity: Int) {
    private val log = LoggerFactory.getLogger(BlockingMessageQueue::class.java)
    private val mLock = ReentrantLock()
    private val mCondition = mLock.newCondition()

    private class Request(
        var isDone: Boolean = false
    )

    private val enqueue: NodeLinkedList<Request> = NodeLinkedList()
    private val dequeue: NodeLinkedList<Request> = NodeLinkedList()
    private val messages: NodeLinkedList<T> = NodeLinkedList()

    @Throws(InterruptedException::class)
    fun tryEnqueue(message: T, timeout: Duration): Boolean {
        mLock.withLock {
            if (timeout.isZero || capacity <= 0) {
                return false
            }

            //fast path
            if (enqueue.empty && messages.count < capacity) {
                messages.enqueue(message)
                mCondition.signalAll()
                return true
            }

            //wait path
            val myRequest = dequeue.enqueue(Request())
            var remainingNanos: Long = timeout.inWholeNanoseconds

            while (true) {
                try {
                    remainingNanos = mCondition.awaitNanos(remainingNanos)

                    if (enqueue.headNode == myRequest && messages.count < capacity) {
                        messages.enqueue(message)
                        myRequest.value.isDone = true
                    }

                    if (myRequest.value.isDone) {
                        enqueue.pull()
                        mCondition.signalAll()
                        return true
                    }

                    if (remainingNanos <= 0) {
                        // giving-up
                        enqueue.remove(myRequest)
                        return false
                    }

                } catch (e: InterruptedException) {
                    if (myRequest.value.isDone) {
                        Thread.currentThread().interrupt()
                        enqueue.remove(myRequest)
                        mCondition.signalAll()
                        return true
                    }
                    enqueue.remove(myRequest)
                    mCondition.signalAll()
                    throw e
                }
            }
        }
    }

    @Throws(InterruptedException::class)
    fun tryDequeue(nOfMessages: Int, timeout: Duration): List<T>? {
        mLock.withLock {
            if (timeout.isZero || nOfMessages <= 0 || capacity <= 0) {
                return null
            }

            //fast-path
            if (dequeue.empty && messages.count >= nOfMessages) {
                val temp = mutableListOf<T>()

                repeat(nOfMessages) {
                    temp.add(messages.pull().value)
                }

                mCondition.signalAll()
                return temp
            }

            //wait path
            val myRequest = dequeue.enqueue(Request())
            var remainingNanos: Long = timeout.inWholeNanoseconds

            while (true) {
                try {
                    remainingNanos = mCondition.awaitNanos(remainingNanos)

                    if (dequeue.headNode == myRequest && messages.count >= nOfMessages) {
                        myRequest.value.isDone = true
                    }

                    if (myRequest.value.isDone) {
                        val temp = mutableListOf<T>()

                        repeat(nOfMessages) {
                            temp.add(messages.pull().value)
                        }

                        dequeue.pull()
                        mCondition.signalAll()
                        return temp
                    }

                    if (remainingNanos <= 0) {
                        // giving-up
                        dequeue.remove(myRequest)
                        return null
                    }
                } catch (e: InterruptedException) {
                    if (myRequest.value.isDone) {
                        Thread.currentThread().interrupt()
                        dequeue.remove(myRequest)

                        val temp = mutableListOf<T>()

                        repeat(nOfMessages) {
                            temp.add(messages.pull().value)
                        }

                        mCondition.signalAll()
                        return temp
                    }
                    dequeue.remove(myRequest)
                    mCondition.signalAll()
                    throw e
                }
            }
        }
    }
}