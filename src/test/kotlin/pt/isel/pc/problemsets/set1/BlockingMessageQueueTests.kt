package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.threadsCreate
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class BlockingMessageQueueTests {

    private val log = LoggerFactory.getLogger(BlockingMessageQueueTests::class.java)

    @Test
    fun `invalid constructor parameter`() {
        assertFailsWith<IllegalArgumentException> {
            val nOfThreads = 4
            val blockingMessageQueue = BlockingMessageQueue<Int>(0)
            val solutions = AtomicInteger(0)

            threadsCreate(nOfThreads) { index ->
                blockingMessageQueue.tryEnqueue(index, 5.seconds)
            }
        }
    }

    @Test
    fun `invalid timeout parameter in tryEnqueue`() {
        val nOfThreads = 4
        val blockingMessageQueue = BlockingMessageQueue<Int>(nOfThreads)
        val solutions = AtomicInteger(0)

        threadsCreate(nOfThreads) { index ->
            assertFailsWith<IllegalArgumentException> {
                blockingMessageQueue.tryEnqueue(index, 0.seconds)
            }
        }
    }

    @Test
    fun `invalid timeout parameter in tryDequeue`() {
        val nOfThreads = 4
        val blockingMessageQueue = BlockingMessageQueue<Int>(nOfThreads)
        val solutions = AtomicInteger(0)

        threadsCreate(nOfThreads) { index ->
            assertFailsWith<IllegalArgumentException> {
                blockingMessageQueue.tryDequeue(1, 0.seconds)
            }
        }
    }

    @Test
    fun `invalid nOfMessages parameter`() {
        val nOfThreads = 5
        val nOfMessages = 0
        val blockingMessageQueue = BlockingMessageQueue<Int>(nOfThreads)
        val solutions = AtomicReference<List<Int>?>(emptyList())

        threadsCreate(nOfThreads) { index ->
            assertFailsWith<IllegalArgumentException> {
                blockingMessageQueue.tryDequeue(nOfMessages, 5.seconds)
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

        threadsCreate(nOfThreads) { index ->
            if (index < nOfMessages)
                blockingMessageQueue.tryEnqueue(index, 5.seconds).let {
                    if (it) solutionsEnqueue.incrementAndGet()
                }
            else
                blockingMessageQueue.tryDequeue(nOfMessages, 5.seconds).let {
                    solutionsDequeue.set(it)
                }
        }

        assertEquals(nOfMessages, solutionsEnqueue.get())
        assertEquals(nOfMessages, solutionsDequeue.get().size)
        assertEquals(true, solutionsDequeue.get().containsAll(listOf(0,1,2,3)))
    }

    @Test
    fun `enqueue ends with timeout`() {
        val nOfThreads = 5
        val blockingMessageQueue = BlockingMessageQueue<Int>(1)
        val solutions = AtomicInteger(0)

        threadsCreate(nOfThreads) { index ->
            blockingMessageQueue.tryEnqueue(index, 5.seconds).let {
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
        val threads = mutableListOf<Thread>()
        val solutions = AtomicReference<List<Int>?>()

        threadsCreate(nOfThreads) { index ->
            if(index == nOfThreads - 1) {
                blockingMessageQueue.tryDequeue(nOfMessages, 5.seconds).let {
                    solutions.set(it)
                }
            } else
                blockingMessageQueue.tryEnqueue(index, 5.seconds)
        }

        assertNull(solutions.get())
    }
}