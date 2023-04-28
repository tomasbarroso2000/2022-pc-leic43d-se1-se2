package pt.isel.pc.problemsets.set2

import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CompletableFutureTests {
    @Test
    fun `simple test with invalid parameter`() {
        assertFailsWith<IllegalArgumentException> {
            any<String>(emptyList())
        }
    }

    @Test
    fun `simple test`() {
        val future = CompletableFuture.supplyAsync {
            Thread.sleep(2000)
            "Hello, World!"
        }
        val list = listOf(future)
        val compFuture: CompletableFuture<String> = any(list)
        println(compFuture.get())
    }

    @Test
    fun `simple test 2`() {
        val nFutures = 20
        val futures: List<CompletableFuture<Int>> = (0 until nFutures).map {
            CompletableFuture.supplyAsync {
                Thread.sleep(1000)
                it
            }
        }
        val compFuture: CompletableFuture<Int> = any(futures)
        val result = compFuture.get()
        println(result)
        assert (result in 0 until nFutures)
    }

    @Test
    fun `simple test with exceptions`() {
        val future = CompletableFuture.supplyAsync {
            Thread.sleep(1000)
            throw Exception("ups something went wrong")
        }
        val list = listOf(future)
        val compFuture: CompletableFuture<Nothing> = any(list)
        assertFailsWith<Exception> {
            compFuture.get()
        }
    }

    @Test
    fun `complex test with exceptions`() {
        val nFutures = 20
        val exceptions = AtomicReference<Exception>()
        val futures: List<CompletableFuture<Int>> =
            (0 until nFutures).map {
                CompletableFuture.supplyAsync {
                    Thread.sleep(1000)
                    throw Exception("ups something went wrong")
                }
            }
        val compFuture: CompletableFuture<Int> = any(futures)
        assertFailsWith<Exception> {
            try {
                compFuture.get()
            } catch (e: Exception) {
                exceptions.set(e)
                throw e
            }
        }
        val excList = exceptions.get().toString().split(',')
        assertEquals(nFutures, excList.size)
    }
}