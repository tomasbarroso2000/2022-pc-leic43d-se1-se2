package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFails
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
        val threads = mutableListOf<Thread>()
        val solutions = AtomicInteger(0)

        repeat(nOfThreads) { index ->
            threads.add(
                Thread {
                    blockingMessageQueue.tryEnqueue(index, 10L.toDuration(DurationUnit.SECONDS)).let {
                        if(!it) solutions.incrementAndGet()
                    }
                }.apply { start() }
            )
        }

        threads.forEach { thread -> thread.join() }

        assertEquals(nOfThreads, solutions.get())
        assertNotSame(nOfThreads+1, solutions.get())
    }

    @Test
    fun `invalid timeout parameter`() {
        val nOfThreads = 4
        val blockingMessageQueue = BlockingMessageQueue<Int>(nOfThreads)
        val threads = mutableListOf<Thread>()
        val solutions = AtomicInteger(0)

        repeat(nOfThreads) { index ->
            threads.add(
                Thread {
                    blockingMessageQueue.tryEnqueue(index, 0.toDuration(DurationUnit.SECONDS)).let {
                        if(!it) solutions.incrementAndGet()
                    }
                }.apply { start() }
            )
        }

        threads.forEach { thread -> thread.join() }

        assertEquals(nOfThreads, solutions.get())
        assertNotSame(nOfThreads+1, solutions.get())
    }

    @Test
    fun `invalid nOfMessages parameter`() {
        val nOfThreads = 5
        val nOfMessages = 4
        val blockingMessageQueue = BlockingMessageQueue<Int>(nOfThreads)
        val threads = mutableListOf<Thread>()
        val solutions = AtomicReference<List<Int>?>(emptyList())

        repeat(nOfThreads) { index ->
            threads.add(
                Thread {
                    if(index == nOfThreads - 1) {
                        blockingMessageQueue.tryDequeue(0, 10L.toDuration(DurationUnit.SECONDS)).let {
                            solutions.set(it)
                        }

                    } else
                        blockingMessageQueue.tryEnqueue(index, 10L.toDuration(DurationUnit.SECONDS))
                }.apply { start() }
            )
        }

        threads.forEach { thread -> thread.join() }

        assertNull(solutions.get())
    }

    @Test
    fun `simple test`() {
        val nOfThreads = 5
        val nOfMessages = 4
        val blockingMessageQueue = BlockingMessageQueue<Int>(nOfThreads)
        val threads = mutableListOf<Thread>()
        val solutions = AtomicReference<List<Int>?>(emptyList())

        repeat(nOfThreads) { index ->
            threads.add(
                Thread {
                    if(index == nOfThreads - 1) {
                        blockingMessageQueue.tryDequeue(nOfMessages, 10L.toDuration(DurationUnit.SECONDS)).let {
                            solutions.set(it)
                        }

                    } else
                        blockingMessageQueue.tryEnqueue(index, 10L.toDuration(DurationUnit.SECONDS))
                }.apply { start() }
            )
        }

        threads.forEach { thread -> thread.join() }

        assertEquals(nOfMessages, solutions.get()?.size)
        assertEquals(true, solutions.get()?.containsAll(listOf(0,1,2,3)))
        assertEquals(false, solutions.get()?.containsAll(listOf(0,1,2,3,4)))
    }

    @Test
    fun `enqueue ends with timeout`() {
        val nOfThreads = 5
        val nOfMessages = 4
        val blockingMessageQueue = BlockingMessageQueue<Int>(1)
        val threads = mutableListOf<Thread>()
        val solutions = AtomicInteger(0)

        repeat(nOfThreads) { index ->
            threads.add(
                Thread {
                    blockingMessageQueue.tryEnqueue(index, 10L.toDuration(DurationUnit.SECONDS)).let {
                        if (!it) solutions.incrementAndGet()
                    }
                }.apply { start() }
            )
        }

        threads.forEach { thread -> thread.join() }

        assertEquals(nOfThreads-1, solutions.get())
        assertNotSame(nOfThreads, solutions.get())
    }

    @Test
    fun `dequeue ends with timeout`() {
        val nOfThreads = 4
        val nOfMessages = 4
        val blockingMessageQueue = BlockingMessageQueue<Int>(nOfThreads)
        val threads = mutableListOf<Thread>()
        val solutions = AtomicReference<List<Int>?>(emptyList())

        repeat(nOfThreads) { index ->
            threads.add(
                Thread {
                    if(index == nOfThreads - 1) {
                        blockingMessageQueue.tryDequeue(nOfMessages, 10L.toDuration(DurationUnit.SECONDS)).let {
                            solutions.set(it)
                        }

                    } else
                        blockingMessageQueue.tryEnqueue(index, 10L.toDuration(DurationUnit.SECONDS))
                }.apply { start() }
            )
        }

        threads.forEach { thread -> thread.join() }
        
        assertNull(solutions.get())
    }
}