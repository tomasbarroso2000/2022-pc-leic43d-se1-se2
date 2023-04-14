package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.threadsCreate
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class BlockingMessageQueueTests {

    companion object {
        private val log = LoggerFactory.getLogger(NAryExchangerTests::class.java)
    }

    @Test
    fun `invalid constructor parameter`() {
        assertFailsWith<IllegalArgumentException> {
            val nOfThreads = 4
            val blockingMessageQueue = BlockingMessageQueue<Int>(0)
            val timeout = 5.seconds

            threadsCreate(nOfThreads) { index ->
                blockingMessageQueue.tryEnqueue(index, timeout)
            }
        }
    }

    @Test
    fun `invalid timeout parameter in tryEnqueue`() {
        val nOfThreads = 4
        val blockingMessageQueue = BlockingMessageQueue<Int>(nOfThreads)
        val timeout = 0.seconds

        threadsCreate(nOfThreads) { index ->
            assertFailsWith<IllegalArgumentException> {
                blockingMessageQueue.tryEnqueue(index, timeout)
            }
        }
    }

    @Test
    fun `invalid timeout parameter in tryDequeue`() {
        val nOfThreads = 4
        val blockingMessageQueue = BlockingMessageQueue<Int>(nOfThreads)
        val timeout = 0.seconds

        threadsCreate(nOfThreads) {
            assertFailsWith<IllegalArgumentException> {
                blockingMessageQueue.tryDequeue(1, timeout)
            }
        }
    }

    @Test
    fun `invalid nOfMessages parameter`() {
        val nOfThreads = 5
        val nOfMessages = 0
        val blockingMessageQueue = BlockingMessageQueue<Int>(nOfThreads)
        val timeout = 5.seconds

        threadsCreate(nOfThreads) {
            assertFailsWith<IllegalArgumentException> {
                blockingMessageQueue.tryDequeue(nOfMessages, timeout)
            }
        }
    }

    @Test
    fun `simple test`() {
        val nOfThreads = 5
        val nOfMessages = 4
        val blockingMessageQueue = BlockingMessageQueue<Int>(nOfMessages)
        val solutionsEnqueue = AtomicInteger(0)
        val solutionsDequeue = AtomicReference<List<Int>>(emptyList())
        val timeout = 5.seconds

        threadsCreate(nOfThreads) { index ->
            if (index < nOfThreads - 1)
                blockingMessageQueue.tryEnqueue(index, timeout).let {
                    if (it) solutionsEnqueue.incrementAndGet()
                }
            else
                blockingMessageQueue.tryDequeue(nOfMessages, timeout).let {
                    solutionsDequeue.set(it)
                }
        }

        assertEquals(nOfMessages, solutionsEnqueue.get())
        assertEquals(nOfMessages, solutionsDequeue.get().size)
        assertEquals(true, solutionsDequeue.get().containsAll(listOf(0,1,2,3)))
        assertEquals(false, solutionsDequeue.get().containsAll(listOf(0,1,2,3,4)))
    }

    @Test
    fun `enqueue ends with timeout`() {
        val nOfThreads = 5
        val nOfMessages = 1
        val blockingMessageQueue = BlockingMessageQueue<Int>(nOfMessages)
        val solutions = AtomicInteger(0)
        val timeout = 5.seconds

        threadsCreate(nOfThreads) { index ->
            blockingMessageQueue.tryEnqueue(index, timeout).let {
                if (!it) solutions.incrementAndGet()
            }
        }

        assertEquals(nOfThreads - 1, solutions.get())
        assertNotSame(nOfThreads, solutions.get())
    }

    @Test
    fun `dequeue ends with timeout`() {
        val nOfThreads = 4
        val nOfMessages = 4
        val blockingMessageQueue = BlockingMessageQueue<Int>(nOfThreads)
        val solutions = AtomicReference<List<Int>?>()
        val timeout = 5.seconds

        threadsCreate(nOfThreads) { index ->
            if(index == nOfThreads - 1) {
                blockingMessageQueue.tryDequeue(nOfMessages, timeout).let {
                    solutions.set(it)
                }
            } else
                blockingMessageQueue.tryEnqueue(index, timeout)
        }

        assertNull(solutions.get())
    }

    @Test
    fun `test Enqueue and Dequeue`() {
        val nThreads = 1
        val nOfMessages = 2
        val blockingMessageQueue = BlockingMessageQueue<String>(nOfMessages)
        val timeout = 1.seconds

        threadsCreate(nThreads) {
            assert(blockingMessageQueue.tryEnqueue("message 1", timeout))
            assert(blockingMessageQueue.tryEnqueue("message 2", timeout))
        }

        threadsCreate(nThreads) {
            val messages = blockingMessageQueue.tryDequeue(nOfMessages, timeout)
            assertNotNull(messages)
            assertEquals(nOfMessages, messages.size)
            assertEquals("message 1", messages[0])
            assertEquals("message 2", messages[1])
        }

    }

    @Test
    fun `test Enqueue Timeout`() {
        val nThreads = 1
        val blockingMessageQueue = BlockingMessageQueue<String>(1)
        val timeout = 1.seconds

        threadsCreate(nThreads) {
            assert(blockingMessageQueue.tryEnqueue("message 1", timeout))
        }

        threadsCreate(nThreads) {
            assertFalse(blockingMessageQueue.tryEnqueue("message 2", timeout))
        }
    }

    @Test
    fun `test Dequeue Timeout`() {
        val blockingMessageQueue = BlockingMessageQueue<String>(1)
        val nThreads = 1
        val nOfMessages = 1
        val timeout = 100.milliseconds

        threadsCreate(nThreads) {
            val messages = blockingMessageQueue.tryDequeue(nOfMessages, timeout)
            assertNull(messages)
        }
    }

    @Test
    fun `test Interrupt`() {
        val nThreads = 1
        val nOfMessages = 1
        val blockingMessageQueue = BlockingMessageQueue<String>(nOfMessages)
        val timeout = 1.seconds

        threadsCreate(nThreads) {
            Thread.currentThread().interrupt()
            assertFailsWith<InterruptedException> {
                blockingMessageQueue.tryDequeue(nOfMessages, timeout)
            }
        }
    }

    @Test
    fun `stress test`() {
        val nThreads = 100
        val nOfMessages = 100
        val blockingMessageQueue = BlockingMessageQueue<Int>(nOfMessages)
        val timeout = INFINITE
        val solutions = (0 until nThreads).toList()

        threadsCreate(nThreads) { index ->
            assert(blockingMessageQueue.tryEnqueue(index, timeout))
        }

        threadsCreate(nThreads / 2) {
            val messages = blockingMessageQueue.tryDequeue(2, timeout)
            log.info("messages: $messages")
            assert(messages?.size == 2 && messages.let { solutions.containsAll(it) })
        }
    }
}