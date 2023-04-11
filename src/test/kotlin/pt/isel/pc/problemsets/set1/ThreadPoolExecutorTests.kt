package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Future
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ThreadPoolExecutorTests {

    companion object {
        private val log = LoggerFactory.getLogger(ThreadPoolExecutorTests::class.java)
    }

    @Test
    fun `invalid ThreadPoolExecutor constructor`() {
        assertFailsWith<IllegalArgumentException> {
            ThreadPoolExecutor(0, 0.seconds)
        }
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
    fun `simple test`() {
        val nOfThreads = 4
        val nRepeats = 2
        val timeout = 5.seconds
        val threadPoolExecutor = ThreadPoolExecutor(nOfThreads, timeout)
        val solutions = ConcurrentLinkedQueue<Int>()

        repeat(nRepeats * nOfThreads) {
            threadPoolExecutor.execute {
                Thread.sleep(1000) // Sleep for 10 seconds
                solutions.add(it)
                println("$it")
            }
        }

        Thread.sleep(timeout.inWholeMilliseconds + 1000)
        assertEquals(nRepeats * nOfThreads, solutions.size)
        assertTrue { solutions.containsAll(listOf(7)) }
    }

    @Test
    fun `simple test 2`() {
        val nOfThreads = 4
        val nRepeats = 2
        val sem = Semaphore(0)
        val timeout = 5.seconds
        val threadPoolExecutor = ThreadPoolExecutor(nOfThreads, timeout)
        val poolThreadIds = ConcurrentHashMap<Thread, Boolean>()
        val solutions = ConcurrentLinkedQueue<Int>()
        repeat(nRepeats * nOfThreads) {
            threadPoolExecutor.execute {
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

    @Test
    fun `simple test to execute runnable successfully with Future`() {
        val nOfThreads: Int = 1
        val maxThreadPoolSize: Int = 3
        val timeout = 5.seconds
        val threadPoolExecutor = ThreadPoolExecutor(maxThreadPoolSize, timeout)
        val solutions = ConcurrentLinkedQueue<Int>()

        val values: List<Future<Unit>> = (0 until nOfThreads).map { index ->
            threadPoolExecutor.execute(
                Callable {
                    solutions.add(index)
                    println("Hello World {$index}")
                }
            )
        }

        values.forEach { it.get(5000, TimeUnit.MILLISECONDS) }

        while (!values.all { it.isDone }) ;

        assertEquals(nOfThreads, solutions.size)

    }

    @Test
    fun `simple test with Future`() {
        val nOfThreads = 4
        val nRepeats = 2
        val timeout = 5.seconds
        val pool = ThreadPoolExecutor(nOfThreads, timeout)
        val solutions = ConcurrentLinkedQueue<Int>()
        val values: List<Future<Int>> = (0 until nOfThreads * nRepeats).map {
            pool.execute(
                Callable {
                    Thread.sleep(1000)
                    println("$it")
                    it
                }
            )
        }

        values.forEach { solutions.add(it.get(5000, TimeUnit.MILLISECONDS)) }

        while (!values.all { it.isDone }) ;

        assertEquals(nOfThreads * nRepeats, solutions.size)
        assert(solutions.containsAll(listOf(nOfThreads * nRepeats - 1)))
    }

    @Test
    fun `simple test to execute 2 runnable that end with timeout with future`() {
        val nOfThreads: Int = 2
        val maxThreadPoolSize: Int = 1
        val timeout = 3.seconds
        val threadPoolExecutor = ThreadPoolExecutor(maxThreadPoolSize, timeout)
        val solutions = AtomicInteger(0)
        val errors = AtomicInteger(0)

        val values: List<Future<Int>> = (0 until nOfThreads).map { index ->
            threadPoolExecutor.execute(
                Callable {
                    Thread.sleep(3000)
                    println("Hello World {$index}")
                    index
                }
            )
        }
        values.forEach {
            try {
                it.get(100, TimeUnit.MILLISECONDS)
                solutions.incrementAndGet()
            } catch (_: TimeoutException) {
                errors.incrementAndGet()
            }
        }

        while (!values.all { it.isDone }) ;

        assertEquals(0, solutions.get())
        assertEquals(2, errors.get())
    }

    @Test
    fun `simple test to execute two runnable but one end with timeout with future`() {
        val nOfThreads: Int = 2
        val maxThreadPoolSize: Int = 1
        val timeout = 3.seconds
        val threadPoolExecutor = ThreadPoolExecutor(maxThreadPoolSize, timeout)
        val solutions = AtomicInteger(0)
        val errors = AtomicInteger(0)

        val values: List<Future<Int>> = (0 until nOfThreads).map { index ->
            threadPoolExecutor.execute(
                Callable {
                    Thread.sleep(1000)
                    println("Hello World {$index}")
                    index
                }
            )
        }
        for (index in values.indices) {
            try {
                if(index == 0) values[index].get(100, TimeUnit.MILLISECONDS)
                else values[index].get(3000, TimeUnit.MILLISECONDS)
                solutions.incrementAndGet()
            } catch (_: TimeoutException) {
                errors.incrementAndGet()
            }
        }

        while (!values.all { it.isDone }) ;

        assertEquals(1, solutions.get())
        assertEquals(1, errors.get())
    }
}