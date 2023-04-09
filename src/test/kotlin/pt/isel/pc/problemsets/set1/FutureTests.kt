package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class FutureTests {

    @Test
    fun `callable executor with a timeout test`() {

        val fut = Future.execute<Int> {
            Thread.sleep(1000)
            23
        }

        val num = fut.get(2000, TimeUnit.MILLISECONDS)
        assertEquals(23, num)
    }
}