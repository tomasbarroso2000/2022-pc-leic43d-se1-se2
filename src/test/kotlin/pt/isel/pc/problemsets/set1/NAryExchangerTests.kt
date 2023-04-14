package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.threadsCreate
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.seconds

class NAryExchangerTests {

    companion object {
        private val log = LoggerFactory.getLogger(NAryExchangerTests::class.java)
    }

    @Test
    fun `invalid NAryExchanger constructor`() {
        assertFailsWith<IllegalArgumentException> {
            val nOfThreads = 4
            val nAryExchanger = NAryExchanger<Int>(0)
            val solutions = AtomicInteger(0)
            val timeout = 5.seconds

            threadsCreate(nOfThreads) { index ->
                nAryExchanger.exchange(index, timeout)?.let {
                    solutions.incrementAndGet()
                }
            }
        }
    }

    @Test
    fun `invalid timeout parameter`() {
        val nOfThreads = 4
        val nAryExchanger = NAryExchanger<Int>(nOfThreads)
        val solutions = AtomicInteger(0)
        val timeout = 0.seconds

        threadsCreate(nOfThreads) { index ->
            assertFailsWith<IllegalArgumentException> {
                nAryExchanger.exchange(index, timeout)?.let {
                    solutions.incrementAndGet()
                }
            }
        }
    }

    @Test
    fun `simple test`() {
        val nOfThreads = 4
        val nExchangerUnits = 4
        val nAryExchanger = NAryExchanger<Int>(nExchangerUnits)
        val solutionsList = AtomicReference<List<Int>>(emptyList())
        val solutions = AtomicInteger(0)
        val timeout = 5.seconds

        threadsCreate(nOfThreads) { index ->
            nAryExchanger.exchange(index, timeout)?.let {
                if (solutionsList.get().isEmpty()) solutionsList.set(it)

                if (solutionsList.get().isNotEmpty() &&
                    solutionsList.get().containsAll(it))
                    solutions.incrementAndGet()
            }
        }

        assertEquals(nExchangerUnits, solutionsList.get().size)
        assertEquals(nExchangerUnits, solutions.get())
    }

    @Test
    fun `exchange calls from different groups`() {
        val nOfThreads = 4
        val nExchangerUnits = 4
        val nAryExchanger = NAryExchanger<Int>(nExchangerUnits)
        val solutionsList1 = AtomicReference<List<Int>>(emptyList())
        val solutionsList2 = AtomicReference<List<Int>>(emptyList())
        val checkList1: List<Int> = (0 until nOfThreads).toList()
        val checkList2: List<Int> = (nOfThreads until nOfThreads * 2).toList()
        val timeout = 5.seconds

        threadsCreate(nOfThreads) { index ->
            nAryExchanger.exchange(index, timeout)?.let {
                solutionsList1.set(it)
            }
        }

        threadsCreate(nOfThreads) { index ->
            nAryExchanger.exchange(nExchangerUnits + index, timeout)?.let {
                solutionsList2.set(it)
            }
        }

        assertEquals(solutionsList1.get().size, nExchangerUnits)
        assert(solutionsList1.get().containsAll(checkList1))
        assertEquals(solutionsList2.get().size, nExchangerUnits)
        assert(solutionsList2.get().containsAll(checkList2))
    }

    @Test
    fun `exchange calls from different groups second version`() {
        val nOfThreads = 8
        val nExchangerUnits = 4
        val nAryExchanger = NAryExchanger<Int>(nExchangerUnits)
        val results = AtomicReference(listOf<List<Int>>().toMutableList())
        val checkList: List<Int> = (0 until nOfThreads).toList()
        val solutions = AtomicInteger(0)
        val timeout = 5.seconds

        threadsCreate(nOfThreads) { index ->
            nAryExchanger.exchange(index, timeout)?.let {
                results.get().add(it)
                solutions.incrementAndGet()
            }
        }

        log.info("${results.get().groupingBy { it }.eachCount().filter { it.value >= 1 }}")

        val endResult = results.get().distinct()
        assertEquals(nOfThreads / nExchangerUnits, endResult.size)
        assert(results.get().flatten().containsAll(checkList))
        assertEquals(nOfThreads, solutions.get())
    }

    @Test
    fun `Interrupting exchange call with exchange unsuccessful`() {
        val nExchangerUnits = 3
        val nAryExchanger = NAryExchanger<Int>(nExchangerUnits)
        val values = AtomicReference<List<Int>?>()
        val threads = mutableListOf<Thread>()
        val timeout = 5.seconds

        val th1 = Thread {
            assertThrows<InterruptedException> {
                nAryExchanger.exchange(1, INFINITE)
            }
        }

        val th2 = Thread {
            values.set(nAryExchanger.exchange(2, timeout))
        }

        threads.add(th1)
        threads.add(th2)

        threads.forEach { it.start() }
        th1.interrupt()
        threads.forEach { it.join() }

        assertEquals(null, values.get())
    }

    @Test
    fun `stress test`() {
        val nOfThreads = 100
        val nExchangerUnits = 5
        val nAryExchanger = NAryExchanger<Int>(nExchangerUnits)
        val results = AtomicReference(listOf<List<Int>>().toMutableList())
        val checkList: List<Int> = (0 until nOfThreads).toList()
        val solutions = AtomicInteger(0)
        val timeout = INFINITE

        threadsCreate(nOfThreads) { index ->
            nAryExchanger.exchange(index, timeout)?.let {
                results.get().add(it)
                solutions.incrementAndGet()
            }
        }

        log.info("${results.get().groupingBy { it }.eachCount().filter { it.value >= 1 }}")

        val endResult = results.get().distinct()
        assertEquals(nOfThreads / nExchangerUnits, endResult.size)
        assert(results.get().flatten().containsAll(checkList))
        assertEquals(nOfThreads, solutions.get())
    }
}