package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.threadsCreate
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class NAryExchangerTest {

    private val log = LoggerFactory.getLogger(NAryExchangerTest::class.java)

    @Test
    fun `invalid constructor parameters`() {
        val nOfThreads = 4
        val nAryExchanger = NAryExchanger<Int>(0)
        val solutions = AtomicInteger(0)

        threadsCreate(nOfThreads) { index ->
            nAryExchanger.exchange(index, 10L.toDuration(DurationUnit.SECONDS))?.let {
                solutions.incrementAndGet()
            }
        }.forEach{it.join()}

        assertEquals(0, solutions.get())
    }

    @Test
    fun `simple test`() {
        val nOfThreads = 4
        val nExchangerUnits = 4
        val nAryExchanger = NAryExchanger<Int>(nExchangerUnits)
        val solutionsList = AtomicReference<List<Int>>()

        threadsCreate(nOfThreads) { index ->
            nAryExchanger.exchange(index, 10L.toDuration(DurationUnit.SECONDS))?.let {
                solutionsList.set(it)
            }
        }.forEach{ it.join() }

        assertEquals(nExchangerUnits, solutionsList.get().size)
        assertTrue(solutionsList.get().containsAll((0..3).toList()))
    }

    @Test
    fun `complex test with more threads than group units`() {
        val nOfThreads = 4
        val nExchangerUnits = 4
        val nAryExchanger = NAryExchanger<Int>(nExchangerUnits)
        val threads = mutableListOf<Thread>()
        val solutions = AtomicInteger(0)
        val solutionsList1 = AtomicReference<List<Int>>()
        val solutionsList2 = AtomicReference<List<Int>>()

        threadsCreate(nOfThreads) { index ->
            nAryExchanger.exchange(index, 10L.toDuration(DurationUnit.SECONDS))?.let {
                solutions.incrementAndGet()
                solutionsList1.set(it)
            }
        }.forEach{ it.join() }

        threadsCreate(nOfThreads) { index ->
            nAryExchanger.exchange(nExchangerUnits + index, 10L.toDuration(DurationUnit.SECONDS))?.let {
                solutions.incrementAndGet()
                solutionsList2.set(it)
            }
        }.forEach{ it.join() }

        assertEquals(solutionsList1.get().size, nExchangerUnits)
        assert(solutionsList1.get().containsAll((0..3).toList()))
        assertEquals(solutionsList2.get().size, nExchangerUnits)
        assert(solutionsList2.get().containsAll((4..7).toList()))
    }

    @Test
    fun `test thread timeout`() {
        val nOfThreads = 3
        val nExchangerUnits = 4
        val nAryExchanger = NAryExchanger<Int>(nExchangerUnits)
        val threads = mutableListOf<Thread>()
        val solutions = AtomicInteger(0)

        threadsCreate(nOfThreads) { index ->
            nAryExchanger.exchange(index, 10L.toDuration(DurationUnit.SECONDS)).let {
                if (it == null)
                    solutions.incrementAndGet()
            }
        }.forEach{ it.join() }

        assertEquals(nOfThreads, solutions.get())
    }
}