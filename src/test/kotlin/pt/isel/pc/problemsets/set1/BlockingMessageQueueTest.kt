package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class BlockingMessageQueueTest {

    private val log = LoggerFactory.getLogger(BlockingMessageQueueTest::class.java)

    @Test
    fun `initial tests`() {
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
    }
}