package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import pt.isel.pc.problemsets.set1.utils.threadsCreate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.measureNanoTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ThreadPoolExecutorTest {
    @Test
    fun `simple test`() {
        val nRepeats: Int = 2
        val nOfThreads: Int = 4
        val maxThreadPoolSize: Int = 4
        val timeout = 5.seconds
        val threadPoolExecutor = ThreadPoolExecutor(maxThreadPoolSize, timeout)
        val solutions: AtomicReference<MutableList<Int>> = AtomicReference(mutableListOf())

        repeat(nRepeats) {
            threadsCreate(nOfThreads) { index ->
                threadPoolExecutor.execute {
                    println("Hello World {$index}")
                    solutions.get().add(index)
                }

            }.forEach{ it.join() }
        }

        assertEquals(nOfThreads*nRepeats, solutions.get().toMutableList().size)
    }

    @Test
    fun `simple test 2`() {
        val nOfThreads = 4
        val sem = Semaphore(0)
        val timeout = 5.seconds
        val pool = ThreadPoolExecutor(nOfThreads, timeout)
        val poolThreadIds = ConcurrentHashMap<Thread, Boolean>()
        val solutions = ConcurrentLinkedQueue<Int>()
        repeat(2 * nOfThreads) {
            pool.execute {
                println("$it")
                solutions.add(it)
                Thread.sleep(1000)
                poolThreadIds[Thread.currentThread()] = true
                sem.release()
            }
        }
        assertTrue(sem.tryAcquire(nOfThreads, 3000, TimeUnit.MILLISECONDS))
        assertEquals(nOfThreads, poolThreadIds.size)
        assertTrue(solutions.containsAll(listOf(7)))
    }

    @Test
    fun `simple test to execute runnable successfully`() {
        val nOfThreads: Int = 1
        val maxThreadPoolSize: Int = 3
        val threadPoolExecutor = ThreadPoolExecutor(maxThreadPoolSize, 5.seconds)
        val solutions: AtomicReference<MutableList<Int>> = AtomicReference(mutableListOf())

        threadsCreate(nOfThreads) { index ->
            threadPoolExecutor.execute {
                println("Hello World {$index}")
                solutions.get().add(index)
            }
        }.forEach{ it.join() }

        assertEquals(1, solutions.get().toMutableList().size)
    }

    @Test
    fun `simple test to execute 2 runnable but 1 times out`() {
        val nOfThreads: Int = 2
        val maxThreadPoolSize: Int = 1
        val threadPoolExecutor = ThreadPoolExecutor(maxThreadPoolSize, 4.seconds)
        val solutions: AtomicReference<MutableList<Int>> = AtomicReference(mutableListOf())

        threadsCreate(nOfThreads) { index ->
            threadPoolExecutor.execute {
                Thread.sleep(3000)
                println("Hello World {$index}")
                solutions.get().add(index)
            }
        }.forEach{ it.join() }

        assertEquals(1, solutions.get().size)
    }

    @Test
    fun `simple test to execute multiple runnable and keep threads alive for 5 seconds`() {
        val nOfThreads: Int = 4
        val maxThreadPoolSize: Int = 4
        val timeout = 5.seconds
        val threadPoolExecutor = ThreadPoolExecutor(maxThreadPoolSize, timeout)
        val solutions: AtomicReference<MutableList<Int>> = AtomicReference(mutableListOf())

        measureNanoTime {
            threadsCreate(nOfThreads) { index ->
                threadPoolExecutor.execute {
                    println("Hello World {$index}")
                    solutions.get().add(index)
                }
            }.forEach{ it.join() }
        }

        assertEquals(nOfThreads, solutions.get().size)
    }
}