package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.threadsCreate
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class BlockingMessageQueueTest {

    private val log = LoggerFactory.getLogger(BlockingMessageQueueTest::class.java)

    @Test
    fun `invalid constructor parameter`() {
        val nOfThreads = 4
        val blockingMessageQueue = BlockingMessageQueue<Int>(0)
        val solutions = AtomicInteger(0)

        threadsCreate(nOfThreads) { index ->
            blockingMessageQueue.tryEnqueue(index, 10L.toDuration(DurationUnit.SECONDS)).let {
                if(!it) solutions.incrementAndGet()
            }
        }.forEach{ it.join() }

        assertEquals(nOfThreads, solutions.get())
        assertNotSame(nOfThreads + 1, solutions.get())
    }

    @Test
    fun `invalid timeout parameter`() {
        val nOfThreads = 4
        val blockingMessageQueue = BlockingMessageQueue<Int>(nOfThreads)
        val solutions = AtomicInteger(0)

        threadsCreate(nOfThreads) { index ->
            blockingMessageQueue.tryEnqueue(index, 0.toDuration(DurationUnit.SECONDS)).let {
                if(!it) solutions.incrementAndGet()
            }
        }.forEach{ it.join() }

        assertEquals(nOfThreads, solutions.get())
        assertNotSame(nOfThreads+1, solutions.get())
    }

    @Test
    fun `invalid nOfMessages parameter`() {
        val nOfThreads = 5
        val nOfMessages = 0
        val blockingMessageQueue = BlockingMessageQueue<Int>(nOfThreads)
        val solutions = AtomicReference<List<Int>?>(emptyList())

        threadsCreate(nOfThreads) { index ->
            if(index == nOfThreads - 1) {
                blockingMessageQueue.tryDequeue(nOfMessages, 10L.toDuration(DurationUnit.SECONDS)).let {
                    solutions.set(it)
                }
            } else
                blockingMessageQueue.tryEnqueue(index, 10L.toDuration(DurationUnit.SECONDS))
        }.forEach{ it.join() }

        assertNull(solutions.get())
    }

    @Test
    fun `simple test`() {
        val nOfThreads = 5
        val nOfMessages = 4
        val blockingMessageQueue = BlockingMessageQueue<Int>(nOfMessages)
        val solutionsEnqueue = AtomicInteger(0)
        val solutionsDequeue = AtomicReference<List<Int>>(emptyList())

        threadsCreate(nOfThreads) { index ->
            if (index < nOfThreads - 1)
                blockingMessageQueue.tryEnqueue(index, 10L.toDuration(DurationUnit.SECONDS)).let {
                    if (it) solutionsEnqueue.incrementAndGet()
                }
            else
                blockingMessageQueue.tryDequeue(nOfMessages, 10L.toDuration(DurationUnit.SECONDS)).let {
                    solutionsDequeue.set(it)
                }
        }.forEach{ it.join() }

        assertEquals(nOfMessages, solutionsEnqueue.get())
        assertEquals(nOfMessages, solutionsDequeue.get().size)
        assertEquals(true, solutionsDequeue.get().containsAll(listOf(0,1,2,3)))
        assertEquals(false, solutionsDequeue.get().containsAll(listOf(0,1,2,3,4)))
    }

    @Test
    fun `enqueue ends with timeout`() {
        val nOfThreads = 5
        val blockingMessageQueue = BlockingMessageQueue<Int>(1)
        val solutions = AtomicInteger(0)

        threadsCreate(nOfThreads) { index ->
            blockingMessageQueue.tryEnqueue(index, 10L.toDuration(DurationUnit.SECONDS)).let {
                if (!it) solutions.incrementAndGet()
            }
        }.forEach{ it.join() }

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
                blockingMessageQueue.tryDequeue(nOfMessages, 10L.toDuration(DurationUnit.SECONDS)).let {
                    solutions.set(it)
                }
            } else
                blockingMessageQueue.tryEnqueue(index, 10L.toDuration(DurationUnit.SECONDS))
        }.forEach{ it.join() }

        assertNull(solutions.get())
    }
}