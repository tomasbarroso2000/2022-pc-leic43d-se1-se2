package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import pt.isel.pc.problemsets.set1.utils.threadsCreate
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ThreadPoolExecutorTest {
    @Test
    fun `simple test`() {
        val nOfThreads: Int = 4
        val maxThreadPoolSize: Int = 2
        val threadPoolExecutor = ThreadPoolExecutor(maxThreadPoolSize, 3000.toDuration(DurationUnit.MILLISECONDS))
        val solutions: AtomicInteger = AtomicInteger(0)

        threadsCreate(nOfThreads) { index ->
            threadPoolExecutor.execute {
                Thread.sleep(2000)
                println("Hello World {$index}")
            }
            solutions.incrementAndGet()
        }.forEach{ it.join() }

        assertEquals(nOfThreads, solutions.get())
    }
}