package pt.isel.pc.problemsets.set2

import org.junit.jupiter.api.Test
import pt.isel.pc.problemsets.set1.utils.threadsCreate
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

class SafeContainerTests {
    @Test
    fun showcase() {
        val players: Array<SafeValue<String>> = arrayOf(SafeValue("isel", 3), SafeValue("pc", 4))
        val safeContainer = SafeContainer(players)
        threadsCreate(7) {
            println(safeContainer.consume())
        }
    }

    @Test
    fun simple_test() {
        val players: Array<SafeValue<String>> = arrayOf(SafeValue("isel", 3), SafeValue("pc", 4))
        val safeContainer = SafeContainer(players)
        val nrValues = 7
        val solutions = AtomicInteger(0)
        threadsCreate(nrValues) {
            val result = safeContainer.consume()
            println(result)
            if (result != null)
                solutions.incrementAndGet()
        }
        assertEquals(nrValues, solutions.get())
    }
}