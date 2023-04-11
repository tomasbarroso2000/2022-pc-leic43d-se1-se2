package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.threadsCreate
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
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

            threadsCreate(nOfThreads) { index ->
                nAryExchanger.exchange(index, 5.seconds)?.let {
                    solutions.incrementAndGet()
                }
            }
        }
    }

    @Test
    fun `invalid NAryExchanger exchanger timeout`() {
        val nOfThreads = 4
        val nAryExchanger = NAryExchanger<Int>(nOfThreads)
        val solutions = AtomicInteger(0)

        threadsCreate(nOfThreads) { index ->
            assertFailsWith<IllegalArgumentException> {
                nAryExchanger.exchange(index, 0.seconds)?.let {
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
        val solutionsList = AtomicReference<List<Int>>()
        val checkList1: List<Int> = (0 until nOfThreads).toList()

        threadsCreate(nOfThreads) { index ->
            nAryExchanger.exchange(index, 5.seconds)?.let {
                solutionsList.set(it)
            }
        }

        assertEquals(nExchangerUnits, solutionsList.get().size)
        assertTrue(solutionsList.get().containsAll(checkList1))
    }

    @Test
    fun `complex test with more threads than group units`() {
        val nOfThreads = 4
        val nExchangerUnits = 4
        val nAryExchanger = NAryExchanger<Int>(nExchangerUnits)
        val solutionsList1 = AtomicReference<List<Int>>(emptyList())
        val solutionsList2 = AtomicReference<List<Int>>()
        val checkList1: List<Int> = (0 until nOfThreads).toList()
        val checkList2: List<Int> = (nOfThreads until nOfThreads * 2).toList()

        threadsCreate(nOfThreads) { index ->
            nAryExchanger.exchange(index, 5.seconds)?.let {
                solutionsList1.set(it)
            }
        }

        threadsCreate(nOfThreads) { index ->
            nAryExchanger.exchange(nExchangerUnits + index, 5.seconds)?.let {
                solutionsList2.set(it)
            }
        }

        assertEquals(solutionsList1.get().size, nExchangerUnits)
        assert(solutionsList1.get().containsAll(checkList1))
        assertEquals(solutionsList2.get().size, nExchangerUnits)
        assert(solutionsList2.get().containsAll(checkList2))
    }
}