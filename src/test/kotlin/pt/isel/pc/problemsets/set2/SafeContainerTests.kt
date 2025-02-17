package pt.isel.pc.problemsets.set2

import org.junit.jupiter.api.Test
import pt.isel.pc.problemsets.set1.utils.threadsCreate
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

class SafeContainerTests {
    @Test
    fun `simple showcase`() {
        val players: Array<SafeValue<String>> = arrayOf(SafeValue("isel", 3), SafeValue("pc", 4))
        val safeContainer = SafeContainer(players)
        threadsCreate(7) {
            println(safeContainer.consume())
        }
    }

    @Test
    fun `simple test`() {
        val players: Array<SafeValue<String>> = arrayOf(SafeValue("isel", 3), SafeValue("pc", 4))
        val safeContainer = SafeContainer(players)
        val nrValues = 7
        val solutions = AtomicInteger(0)
        val errors = AtomicInteger(0)
        val values = mutableListOf<String>()
        threadsCreate(nrValues) {
            val result = safeContainer.consume()
            println(result)
            if (result == null) errors.incrementAndGet()
            else {
                solutions.incrementAndGet()
                values.add(result)
            }
        }
        assertEquals(nrValues, solutions.get())
        assert ( values.filter { it == "isel" }.size == 3 && values.filter { it == "pc" }.size == 4)
    }

    @Test
    fun `simple test with null`() {
        val players: Array<SafeValue<String>> = arrayOf(SafeValue("isel", 3), SafeValue("pc", 4))
        val safeContainer = SafeContainer(players)
        val nrValues = 7
        val nrErrors = 1
        val solutions = AtomicInteger(0)
        val errors = AtomicInteger(0)
        val values = mutableListOf<String>()
        threadsCreate(nrValues + nrErrors) {
            val result = safeContainer.consume()
            println(result)
            if (result == null) errors.incrementAndGet()
            else {
                solutions.incrementAndGet()
                values.add(result)
            }
        }
        assertEquals(nrValues, solutions.get())
        assert ( values.filter { it == "isel" }.size == 3 && values.filter { it == "pc" }.size == 4)
        assertEquals(1, errors.get())
    }

    @Test
    fun `stress test`() {
        val nThreads = 100
        val nLives = 3
        val solutions = AtomicInteger(0)
        val errors = AtomicInteger(0)
        val values = mutableListOf<String>()
        val content: Array<SafeValue<String>> = (0 until nThreads).map {
            SafeValue("isel", nLives)
        }.toTypedArray()
        val safeContainer = SafeContainer(content)

        threadsCreate(nThreads * nLives) {
            val result = safeContainer.consume()
            println(result)
            if (result == null) errors.incrementAndGet()
            else {
                solutions.incrementAndGet()
                values.add(result)
            }
        }

        assertEquals(nThreads * nLives, solutions.get())
        assert ( values.filter { it == "isel" }.size == nThreads * nLives)
        assertEquals(0, errors.get())
    }

    @Test
    fun `test with one value`() {
        val players: Array<SafeValue<String>> = arrayOf(SafeValue("isel", 3))
        val safeContainer = SafeContainer(players)
        val nrValues = 2
        val solutions = AtomicInteger(0)
        val values = mutableListOf<String>()
        threadsCreate(nrValues) {
            val result = safeContainer.consume()
            println(result)
            if (result != null) {
                solutions.incrementAndGet()
                values.add(result)
            }
        }
        assertEquals(nrValues, solutions.get())
        assertEquals("isel", values.first())
    }

    @Test
    fun `test with no lives`() {
        val players: Array<SafeValue<String>> = arrayOf(SafeValue("isel", 0), SafeValue("pc", 0))
        val safeContainer = SafeContainer(players)
        val nrValues = 2
        val solutions = AtomicInteger(0)
        val errors = AtomicInteger(0)
        threadsCreate(nrValues) {
            val result = safeContainer.consume()
            println(result)
            if (result == null) errors.incrementAndGet()
            else solutions.incrementAndGet()
        }
        assertEquals(0, solutions.get())
        assertEquals(2, errors.get())
    }


    @Test
    fun `test with multiple threads and values with different lives`() {
        val players: Array<SafeValue<String>> = arrayOf(SafeValue("isel", 3), SafeValue("pc", 2))
        val safeContainer = SafeContainer(players)
        val nrValues = 10
        val solutions = AtomicInteger(0)
        val values = mutableListOf<String>()
        threadsCreate(nrValues) {
            val result = safeContainer.consume()
            println(result)
            if (result != null) {
                solutions.incrementAndGet()
                values.add(result)
            } else {
                solutions.decrementAndGet()
            }
        }
        assertEquals(0, solutions.get())
        assert(values.filter { it == "isel" }.size == 3)
        assert(values.filter { it == "pc" }.size == 2)
    }
}