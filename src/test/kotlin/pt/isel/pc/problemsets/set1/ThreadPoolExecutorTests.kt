package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.threadsCreate
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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
        val nOfThreads = 1
        val maxThreadPoolSize = 3
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
    fun `simple test to show thread pool running`() {
        val timeout = 5.seconds
        val threadPoolExecutor = ThreadPoolExecutor(4, timeout)
        val mainTid = Thread.currentThread().id
        println("[T$mainTid] :: STARTING ::")

        for (n in 0..15) {
            threadPoolExecutor.execute {
                val tid = Thread.currentThread().id
                println("[T$tid] executing task #$n")
                if (n == 3) {
                    throw Exception("Emotional damage!")
                }
                Thread.sleep(50)
            }
        }

        println("[T$mainTid] :: WAITING ::")
        threadPoolExecutor.shutdown()

        if (!threadPoolExecutor.awaitTermination(timeout)) {
            println("[T$mainTid] :: timed out ::")
        } else {
            println("[T$mainTid] :: DONE ::")
        }
    }

    @Test
    fun `not so simple test`() {
        val nOfThreads = 4
        val sem = Semaphore(0)
        val timeout = 5.seconds
        val threadPoolExecutor = ThreadPoolExecutor(4, timeout)
        val poolThreadIds = ConcurrentHashMap<Thread, Boolean>()
        val ths = List(2 * nOfThreads) {
            thread {
                threadPoolExecutor.execute {
                    Thread.sleep(1000)
                    poolThreadIds[Thread.currentThread()] = true
                    sem.release()
                }
            }
        }
        ths.forEach { it.join(1000) }
        assertTrue(ths.all { !it.isAlive })
        assertTrue(sem.tryAcquire(2 * nOfThreads, 3000, TimeUnit.MILLISECONDS))
        assertEquals(nOfThreads, poolThreadIds.size)
    }

    @Test
    fun `simple test to execute 2 runnable but 1 times out`() {
        val nOfThreads = 2
        val maxThreadPoolSize = 1
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
        val nOfThreads = 4
        val maxThreadPoolSize = 4
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
    fun `test await termination with success`() {
        val timeout = 5.seconds
        val nThreads = 4
        val executor = ThreadPoolExecutor(nThreads, timeout)
        val solutions = AtomicInteger(0)

        repeat(nThreads) {
            executor.execute {
                println("Hello: $it")
                Thread.sleep(1000)
                solutions.incrementAndGet()
            }
        }

        executor.shutdown()

        assert(executor.awaitTermination(10.seconds))
        assertEquals(nThreads, solutions.get())
    }

    @Test
    fun `test await termination with failure`() {
        val timeout = 10.seconds
        val nThreads = 4
        val executor = ThreadPoolExecutor(nThreads, timeout)
        val solutions = AtomicInteger(0)

        repeat(nThreads) {
            executor.execute {
                println("Hello: $it")
                Thread.sleep(1000)
                solutions.incrementAndGet()
            }
        }

        executor.shutdown()

        assertFalse(executor.awaitTermination(5.seconds))
        assertEquals(nThreads, solutions.get())
    }

    @Test
    fun `test shutdown`() {
        val timeout = 10.seconds
        val nThreads = 4
        val executor = ThreadPoolExecutor(nThreads, timeout)
        val solutions = AtomicInteger(0)

        repeat(nThreads) {
            executor.execute {
                println("Hello: $it")
                Thread.sleep(10000)
                solutions.incrementAndGet()
            }
        }

        executor.shutdown()

        assertFailsWith<RejectedExecutionException> {
            executor.execute {
                println("Hello: 1000")
            }
        }

        assertEquals(0, solutions.get())
    }

    @Test
    fun `thread pool stress test`() {
        val nOfThreads = 100
        val maxThreadPoolSize = 100
        val timeout = 5.seconds
        val threadPoolExecutor = ThreadPoolExecutor(maxThreadPoolSize, timeout)
        val solutions = ConcurrentLinkedQueue<Int>()

        repeat(nOfThreads) { index ->
            threadPoolExecutor.execute {
                println("Hello World {$index}")
                solutions.add(index)
                Thread.sleep(1000)
            }
        }

        Thread.sleep(timeout.inWholeMilliseconds + 1000)
        assertEquals(nOfThreads, solutions.size)
        assertTrue { solutions.containsAll(listOf(nOfThreads-1)) }
    }

    @Test
    fun `simple test to execute runnable successfully with Future`() {
        val nOfThreads = 1
        val maxThreadPoolSize = 3
        val timeout = 5.seconds
        val threadPoolExecutor = ThreadPoolExecutor(maxThreadPoolSize, timeout)
        val solutions = ConcurrentLinkedQueue<Int>()

        repeat(nOfThreads) { index ->
            val fut = threadPoolExecutor.execute(
                Callable {
                    println("Hello World {$index}")
                    solutions.add(index)
                }
            )
            fut.get(2000, TimeUnit.MILLISECONDS)
        }

        Thread.sleep(timeout.inWholeMilliseconds + 1000)
        assertEquals(nOfThreads, solutions.size)
        assert(solutions.contains(0))
    }

    @Test
    fun `simple test with Future`() {
        val nOfThreads = 8
        val maxThreadPoolSize = 3
        val timeout = 5.seconds
        val threadPoolExecutor = ThreadPoolExecutor(maxThreadPoolSize, timeout)
        val solutions = ConcurrentLinkedQueue<Int>()
        val resultList = (0 until nOfThreads).toList()

        repeat(nOfThreads) { index ->
            val fut = threadPoolExecutor.execute(
                Callable {
                    println("Hello World {$index}")
                    solutions.add(index)
                }
            )
            fut.get(2000, TimeUnit.MILLISECONDS)
        }

        Thread.sleep(timeout.inWholeMilliseconds + 1000)
        assertEquals(nOfThreads, solutions.size)
        assert(solutions.containsAll(resultList))
    }

    @Test
    fun `simple test 2 with Future`() {
        val nOfThreads = 4
        val nRepeats = 2
        val sem = Semaphore(0)
        val timeout = 10.seconds
        val threadPoolExecutor = ThreadPoolExecutor(nOfThreads, timeout)
        val poolThreadIds = ConcurrentHashMap<Thread, Boolean>()
        val solutions = ConcurrentLinkedQueue<Int>()

        repeat(nOfThreads * nRepeats) {
            val call = Callable {
                println("$it")
                solutions.add(it)
                Thread.sleep(5000)
                poolThreadIds[Thread.currentThread()] = true
                sem.release()
            }

            val fut = threadPoolExecutor.execute(call)

            fut.get(10000, TimeUnit.MILLISECONDS)
        }

        Thread.sleep(timeout.inWholeMilliseconds + 1000)
        assertTrue(sem.tryAcquire(nOfThreads, 3000, TimeUnit.MILLISECONDS))
        assertEquals(nOfThreads, poolThreadIds.size)
        assertEquals(nRepeats * nOfThreads, solutions.size)
        assertTrue { solutions.containsAll(listOf(7)) }
    }

    @Test
    fun `simple test to execute 2 runnable but 1 times out with Future`() {
        val nOfThreads = 2
        val maxThreadPoolSize = 1
        val timeout = 3.seconds
        val threadPoolExecutor = ThreadPoolExecutor(maxThreadPoolSize, timeout)
        val solutions = ConcurrentLinkedQueue<Int>()

        repeat(nOfThreads) { index ->
            val fut = threadPoolExecutor.execute(
                Callable {
                    Thread.sleep(3000)
                    println("Hello World {$index}")
                    solutions.add(index)
                }
            )
            if (index == 0)
                fut.get(5000, TimeUnit.MILLISECONDS)
            else
                assertFailsWith<TimeoutException> {
                    fut.get(5000, TimeUnit.MILLISECONDS)
                }
        }

        Thread.sleep(timeout.inWholeMilliseconds + 1000)
        assertEquals(maxThreadPoolSize, solutions.size)
        assertTrue { solutions.contains(0) }
    }

    @Test
    fun `thread pool stress test with Future`() {
        val nOfThreads = 100
        val maxThreadPoolSize = 100
        val timeout = 5.seconds
        val threadPoolExecutor = ThreadPoolExecutor(maxThreadPoolSize, timeout)
        val solutions = ConcurrentLinkedQueue<Int>()

        repeat(nOfThreads) { index ->
            val fut = threadPoolExecutor.execute (
                Callable {
                    println("Hello World {$index}")
                    solutions.add(index)
                    Thread.sleep(1000)
                }
            )
            fut.get(2000, TimeUnit.MILLISECONDS)
        }

        Thread.sleep(timeout.inWholeMilliseconds + 1000)
        assertEquals(nOfThreads, solutions.size)
        assertTrue { solutions.containsAll(listOf(nOfThreads-1)) }
    }
}