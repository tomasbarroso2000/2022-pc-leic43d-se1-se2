package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import pt.isel.pc.problemsets.set1.utils.threadsCreate
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ThreadPoolExecutorTest {
    @Test
    fun `simple test`() {
        val nRepeats: Int = 4
        val nOfThreads: Int = 4
        val maxThreadPoolSize: Int = 3
        val threadPoolExecutor = ThreadPoolExecutor(maxThreadPoolSize, 5.toDuration(DurationUnit.SECONDS))
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
    fun `simple test to execute 0 runnable in time due to times out`() {
        val nOfThreads: Int = 2
        val maxThreadPoolSize: Int = 1
        val threadPoolExecutor = ThreadPoolExecutor(maxThreadPoolSize, 2.seconds)
        val solutions: AtomicReference<MutableList<Int>> = AtomicReference(mutableListOf())

        threadsCreate(nOfThreads) { index ->
            threadPoolExecutor.execute {
                Thread.sleep(3000)
                println("Hello World {$index}")
                solutions.get().add(index)
            }
        }.forEach{ it.join() }

        assertEquals(0, solutions.get().toMutableList().size)
    }

    @Test
    fun `simple test to execute multiple runnable`() {
        val nOfThreads: Int = 4
        val maxThreadPoolSize: Int = 4
        val threadPoolExecutor = ThreadPoolExecutor(maxThreadPoolSize, 10.seconds)
        val solutions: AtomicReference<MutableList<Int>> = AtomicReference(mutableListOf())

        threadsCreate(nOfThreads) { index ->
            threadPoolExecutor.execute {
                println("Hello World {$index}")
                solutions.get().add(index)
            }
        }.forEach{ it.join() }

        assertEquals(nOfThreads, solutions.get().toMutableList().size)
    }
}