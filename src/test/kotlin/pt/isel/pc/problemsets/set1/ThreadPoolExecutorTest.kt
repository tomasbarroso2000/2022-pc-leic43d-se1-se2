package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ThreadPoolExecutorTest {
    @Test
    fun `simple test`() {
        /*val threadPoolExecutor = ThreadPoolExecutor(1, 10L.toDuration(DurationUnit.SECONDS))
        val threads: MutableList<Thread> = mutableListOf()

        repeat(3) { index ->
            threads.add(
                Thread {
                    threadPoolExecutor.execute { println("Hello World {$index}") }
                }.apply { start() }
            )
        }

        threads.forEach { thread -> thread.join() }*/
    }
}