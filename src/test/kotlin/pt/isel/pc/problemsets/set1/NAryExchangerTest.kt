package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class NAryExchangerTest {

    private val log = LoggerFactory.getLogger(NAryExchangerTest::class.java)

    @Test
    fun `invalid constructor parameters`() {
        val nOfThreads = 4
        val nExchangerUnits = 4
        val nAryExchanger = NAryExchanger<Int>(0)
        val threads = mutableListOf<Thread>()
        val solutions = AtomicInteger(0)

        repeat(nOfThreads) {
            threads.add(
                Thread {
                    nAryExchanger.exchange(it, 10L.toDuration(DurationUnit.SECONDS))?.let {
                        solutions.incrementAndGet()
                    }
                }.apply { start() }
            )
        }

        threads.forEach { thread -> thread.join() }

        assertEquals(0, solutions.get())
    }

    @Test
    fun `simple test`() {
        val nOfThreads = 4
        val nExchangerUnits = 4
        val nAryExchanger = NAryExchanger<Int>(nExchangerUnits)
        val threads = mutableListOf<Thread>()
        val solutions = AtomicInteger(0)

        repeat(nOfThreads) {
            threads.add(
                Thread {
                    nAryExchanger.exchange(it, 10L.toDuration(DurationUnit.SECONDS))?.let {
                        solutions.incrementAndGet()
                    }
                }.apply { start() }
            )
        }

        threads.forEach { thread -> thread.join() }

        assertEquals(nOfThreads, solutions.get())
    }

    @Test
    fun `complex test with more threads than group units`() {
        val nOfThreads = 8
        val nExchangerUnits = 4
        val nAryExchanger = NAryExchanger<Int>(nExchangerUnits)
        val threads = mutableListOf<Thread>()
        val solutions = AtomicInteger(0)

        repeat(nOfThreads) {
            threads.add(
                Thread {
                    nAryExchanger.exchange(it, 10L.toDuration(DurationUnit.SECONDS))?.let {
                        solutions.incrementAndGet()
                    }
                }.apply { start() }
            )
        }

        threads.forEach { thread -> thread.join() }
        assertEquals(nOfThreads, solutions.get())
    }

    @Test
    fun `test thread timeout`() {
        val nOfThreads = 5
        val nExchangerUnits = 4
        val nAryExchanger = NAryExchanger<Int>(nExchangerUnits)
        val threads = mutableListOf<Thread>()
        val solutions = AtomicInteger(0)

        repeat(nOfThreads) {
            threads.add(
                Thread {
                    nAryExchanger.exchange(it, 10L.toDuration(DurationUnit.SECONDS)).let {
                        if (it == null)
                            solutions.incrementAndGet()
                    }
                }.apply { start() }
            )
        }

        threads.forEach { thread -> thread.join() }

        assertEquals(1, solutions.get())
    }
}