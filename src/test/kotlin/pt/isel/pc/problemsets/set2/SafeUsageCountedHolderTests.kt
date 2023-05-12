package pt.isel.pc.problemsets.set2

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.threadsCreate
import java.io.Closeable
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SafeUsageCountedHolderTests {

    companion object {
        private val log = LoggerFactory.getLogger(SafeUsageCountedHolder::class.java)

        class Work<T>(val value: T) : Closeable {
            override fun close() {
                log.info("closed")
            }

            fun test(): T {
                log.info("$value")
                return value
            }
        }
    }

    @Test
    fun `simple test`() {
        val safeUsage = SafeUsageCountedHolder(Work("Hello World"))
        threadsCreate(1) {
            safeUsage.endUse()
        }
    }

    @Test
    fun `simple test 2`() {
        val safeUsage = SafeUsageCountedHolder(Work("Hello World"))
        threadsCreate(1) {
            safeUsage.tryStartUse()?.test()
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
        val safeUsage = SafeUsageCountedHolder(Work("Hello World"))
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
        val safeUsage = SafeUsageCountedHolder(Work("Hello World"))
        val nThreads = 50
        threadsCreate(nThreads) {
            safeUsage.tryStartUse()?.test()
        }
        threadsCreate(nThreads + 1) {
            safeUsage.endUse()
        }
    }

    @Test
    fun `tryStartUse returns the value when it is not closed`() {
        val value = "test"
        val holder = SafeUsageCountedHolder(Work(value))

        val result = holder.tryStartUse()
        result?.test()

        assertEquals(value, result?.value)
    }

    @Test
    fun `tryStartUse returns null when it is closed`() {
        val holder = SafeUsageCountedHolder(Work("test"))

        holder.endUse()
        val result = holder.tryStartUse()

        assertNull(result)
    }

    @Test
    fun `endUse throws an exception when it is already closed`() {
        val holder = SafeUsageCountedHolder(Work("test"))
        holder.endUse()
        assertThrows<IllegalStateException> { holder.endUse() }
    }
}