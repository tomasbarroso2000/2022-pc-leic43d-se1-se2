package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.DurationUnit
import kotlin.time.toDuration

const val N_EXCHANGER = 4
const val N_THREADS = 4
const val TIMEOUT = 10000L

class NAryExchangerTest {

    private val log = LoggerFactory.getLogger(NAryExchangerTest::class.java)

    @Test
    fun `first implementation test of NAryExchanger`() {
        val nAryExchanger = NAryExchanger<Int>(N_EXCHANGER)
        val threads = mutableListOf<Thread>()
        val solutions = mutableListOf<List<Int>?>()
        val lock = ReentrantLock()

        repeat(N_THREADS) {
            threads.add(
                Thread {
                    val res: List<Int>? = nAryExchanger.exchange(it, TIMEOUT.toDuration(DurationUnit.SECONDS))
                    log.info("$res")
                    lock.withLock {
                        solutions.add(res)
                    }
                }.apply { start() }
            )
        }

        threads.forEach { thread -> thread.join() }

        assertEquals(N_THREADS, solutions.size)

        assertTrue {
            var total: Int = 0
            solutions.forEach { list ->
                if(list != null) total += list.size
            }
            total == N_EXCHANGER * N_THREADS
        }

    }
}