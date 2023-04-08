package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ThreadPoolExecutorTest {
    @Test
    fun `invalid ThreadPoolExecutor constructor`() {
        assertFailsWith<IllegalArgumentException> {
            val pool = ThreadPoolExecutor(0, 0.seconds)

            repeat(1) {
                pool.execute {
                    Thread.sleep(1000) // Sleep for 10 seconds
                    println("$it")
                }
            }
        }
    }

    @Test
    fun `simple test`() {
        val nOfThreads = 4
        val nRepeats = 2
        val timeout = 5.seconds
        val pool = ThreadPoolExecutor(nOfThreads, timeout)
        val solutions = ConcurrentLinkedQueue<Int>()

        repeat(nRepeats * nOfThreads) {
            pool.execute {
                Thread.sleep(1000) // Sleep for 10 seconds
                solutions.add(it)
                println("$it")
            }
        }

        Thread.sleep(timeout.inWholeMilliseconds + 1000)
        assertEquals(nRepeats * nOfThreads, solutions.size)
        assertTrue { solutions.containsAll(listOf(7)) }
        println("Completed tasks: ${solutions}")
    }

    @Test
    fun `simple test 2`() {
        val nOfThreads = 4
        val nRepeats = 2
        val sem = Semaphore(0)
        val timeout = 5.seconds
        val pool = ThreadPoolExecutor(nOfThreads, timeout)
        val poolThreadIds = ConcurrentHashMap<Thread, Boolean>()
        val solutions = ConcurrentLinkedQueue<Int>()
        repeat(nRepeats * nOfThreads) {
            pool.execute {
                println("$it")
                solutions.add(it)
                Thread.sleep(2000)
                poolThreadIds[Thread.currentThread()] = true
                sem.release()
            }
        }

        Thread.sleep(timeout.inWholeMilliseconds + 1000)
        assertTrue(sem.tryAcquire(nOfThreads, 3000, TimeUnit.MILLISECONDS))
        assertEquals(nOfThreads, poolThreadIds.size)
        assertEquals(nRepeats * nOfThreads, solutions.size)
        assertTrue { solutions.containsAll(listOf(7)) }
    }

    @Test
    fun `simple test to execute runnable successfully`() {
        val nOfThreads: Int = 1
        val maxThreadPoolSize: Int = 3
        val timeout = 5.seconds
        val threadPoolExecutor = ThreadPoolExecutor(maxThreadPoolSize, timeout)
        val solutions = ConcurrentLinkedQueue<Int>()

        repeat(nOfThreads) { index ->
            threadPoolExecutor.execute {
                println("Hello World {$index}")
                solutions.add(index)
            }
        }

        Thread.sleep(timeout.inWholeMilliseconds + 1000)
        assertEquals(nOfThreads, solutions.size)
    }

    @Test
    fun `simple test to execute 2 runnable but 1 times out`() {
        val nOfThreads: Int = 2
        val maxThreadPoolSize: Int = 1
        val timeout = 3.seconds
        val threadPoolExecutor = ThreadPoolExecutor(maxThreadPoolSize, timeout)
        val solutions = ConcurrentLinkedQueue<Int>()

        repeat(nOfThreads) { index ->
            threadPoolExecutor.execute {
                Thread.sleep(3000)
                println("Hello World {$index}")
                solutions.add(index)
            }
        }

        Thread.sleep(timeout.inWholeMilliseconds + 1000)
        assertEquals(1, solutions.size)
        assertTrue { solutions.contains(0) }
    }

    @Test
    fun `simple test to execute multiple runnable and keep threads alive for 5 seconds`() {
        val nOfThreads: Int = 4
        val maxThreadPoolSize: Int = 4
        val timeout = 5.seconds
        val threadPoolExecutor = ThreadPoolExecutor(maxThreadPoolSize, timeout)
        val solutions = ConcurrentLinkedQueue<Int>()

        repeat(nOfThreads) { index ->
            threadPoolExecutor.execute {
                println("Hello World {$index}")
                solutions.add(index)
            }
        }

        Thread.sleep(timeout.inWholeMilliseconds + 1000)
        assertEquals(nOfThreads, solutions.size)
        assertTrue { solutions.containsAll(listOf(3)) }
    }
}