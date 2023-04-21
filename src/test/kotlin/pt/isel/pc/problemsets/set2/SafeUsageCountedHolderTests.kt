package pt.isel.pc.problemsets.set2

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.threadsCreate
import java.io.Closeable
import kotlin.test.assertFailsWith

class SafeUsageCountedHolderTests {

    companion object {
        private val log = LoggerFactory.getLogger(SafeUsageCountedHolder::class.java)

        class Work : Closeable {
            override fun close() {
                log.info("closed")
            }

            fun hello() {
                log.info("Hello World")
            }
        }
    }

    @Test
    fun `simple test`() {
        val safeUsage = SafeUsageCountedHolder(Work())
        threadsCreate(1) {
            safeUsage.endUse()
        }
    }

    @Test
    fun `simple test 2`() {
        val safeUsage = SafeUsageCountedHolder(Work())
        threadsCreate(1) {
            safeUsage.tryStartUse()?.hello()
        }
        threadsCreate(1) {
            safeUsage.endUse()
        }
        threadsCreate(1) {
            safeUsage.endUse()
        }
    }

    @Test
    fun `simple test that ends with IllegalStateException`() {
        val safeUsage = SafeUsageCountedHolder(Work())
        threadsCreate(1) {
            safeUsage.endUse()
        }

        threadsCreate(1) {
            assertFailsWith<IllegalStateException> {
                safeUsage.endUse()
            }
        }
    }

    @Test
    fun `simple test 3`() {
        val safeUsage = SafeUsageCountedHolder(Work())
        val nThreads = 50
        threadsCreate(nThreads) {
            safeUsage.tryStartUse()?.hello()
        }
        threadsCreate(nThreads + 1) {
            safeUsage.endUse()
        }
    }
}