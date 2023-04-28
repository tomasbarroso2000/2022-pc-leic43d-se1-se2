package pt.isel.pc.problemsets.set2

import org.junit.jupiter.api.Test
import pt.isel.pc.problemsets.set1.utils.threadsCreate
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class CyclicBarrierTests {
    @Test
    fun `test await`() {
        val barrier = CyclicBarrier(3)
        val results = mutableListOf<Int>()
        val nOfThreads = 3

        threadsCreate(nOfThreads) {
            results.add(barrier.await())
        }

        assert(results.containsAll(listOf(nOfThreads - 1)))
    }

    @Test
    fun `test await 2`() {
        val runnable = Runnable {
            Thread.sleep(2000)
            println("Hello World")
        }
        val barrier = CyclicBarrier(3, runnable)
        val results = mutableListOf<Int>()
        val nOfThreads = 3

        threadsCreate(nOfThreads) {
            results.add(barrier.await())
        }

        assert(results.containsAll(listOf(nOfThreads - 1)))
    }

    @Test
    fun `testAwaitWithTimeout`() {
        val barrier = CyclicBarrier(3)
        val results = mutableListOf<Int>()
        val nOfThreads = 2
        val timeout = 2L

        threadsCreate(nOfThreads) {
            assertFailsWith<TimeoutException> {
                results.add(barrier.await(timeout, TimeUnit.SECONDS))
            }
        }

    }

    @Test
    fun testIsBroken() {
        val barrier = CyclicBarrier(3)
        val nThreads = 3
        val counter = AtomicInteger(0)

        threadsCreate(nThreads) {
            if (it < nThreads - 1) {
                try {
                    barrier.await()
                } catch (_: BrokenBarrierException) {
                    counter.incrementAndGet()
                }
            } else {
                barrier.reset()
            }
        }

        assertEquals(nThreads - 1, counter.get())
    }
}