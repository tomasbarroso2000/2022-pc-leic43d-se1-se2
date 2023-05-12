package pt.isel.pc.problemsets.set2

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CompletableFutureTests {
    companion object {
        private val log = LoggerFactory.getLogger(CompletableFutureTests::class.java)
    }

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
            println("Hello, World!")
        }
        val list = listOf(future)
        val compFuture: CompletableFuture<Unit> = any(list)
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
    fun `simple test with exception`() {
        val nFutures = 1
        val futures: MutableList<CompletableFuture<Int>> = mutableListOf()
        repeat(nFutures) {
            futures.add(
                CompletableFuture.supplyAsync {
                    Thread.sleep(1000)
                    throw Exception("ups something went wrong")
                }
            )
        }

        val compFuture: CompletableFuture<Int> = any(futures)

        try {
            compFuture.get()
        } catch (e: Exception) {
            log.info("${e.cause}")
            val ex = e.cause as CustomException
            log.info("Exceptions: ${ex.exceptions}")
            assert(e.cause is CustomException)
        }
    }

    @Test
    fun `complex test with exceptions`() {
        val nFutures = 20
        val futures: MutableList<CompletableFuture<Int>> = mutableListOf()
        repeat(nFutures) {
            futures.add(
                CompletableFuture.supplyAsync {
                    Thread.sleep(1000)
                    throw Exception("ups something went wrong")
                }
            )
        }

        val compFuture: CompletableFuture<Int> = any(futures)

        try {
            compFuture.get()
        } catch (e: Exception) {
            log.info("${e.cause}")
            val ex = e.cause as CustomException
            log.info("Exceptions: ${ex.exceptions}")
            assert(e.cause is CustomException)
        }
    }

    @Test
    fun `any should return result of successful future`() {
        val future1 = CompletableFuture.completedFuture(1)
        val future2 = CompletableFuture<Int>()
        val future3 = CompletableFuture<Int>()

        future2.completeExceptionally(RuntimeException("future2 failed"))
        future3.completeExceptionally(RuntimeException("future3 failed"))

        val result = any(listOf(future1, future2, future3)).get()
        assertEquals(1, result)
    }

    @Test
    fun `any should throw exception with multiple failures`() {
        val future1 = CompletableFuture<Int>()
        val future2 = CompletableFuture<Int>()
        val future3 = CompletableFuture<Int>()

        future1.completeExceptionally(RuntimeException("future1 failed"))
        future2.completeExceptionally(RuntimeException("future2 failed"))
        future3.completeExceptionally(RuntimeException("future3 failed"))


        try {
            any(listOf(future1, future2, future3)).get()
        } catch (e: Exception) {
            log.info("${e.cause}")
            assert(e.cause is CustomException)
            val ex = e.cause as CustomException
            log.info("Exceptions: ${ex.exceptions}")
            assertEquals(3, ex.exceptions.size)
        }
    }

    @Test
    fun `any should throw exception with single failure`() {
        val future1 = CompletableFuture<Int>()
        val future2 = CompletableFuture<Int>()
        val future3 = CompletableFuture<Int>()

        future1.completeExceptionally(RuntimeException("future1 failed"))
        future2.complete(2)

        try {
            any(listOf(future1, future2, future3)).get()
        } catch (e: Exception) {
            log.info("${e.cause}")
            assert(e.cause is CustomException)
            val ex = e.cause as CustomException
            log.info("Exceptions: ${ex.exceptions}")
            assertEquals(1, ex.exceptions.size)
            assertTrue(ex.exceptions[0].message?.contains("future1 failed") ?: false)
        }
    }

    @Test
    fun `any should throw InterruptedException when interrupted`() {
        val future1 = CompletableFuture<Int>()
        val future2 = CompletableFuture<Int>()
        val future3 = CompletableFuture<Int>()

        val executor = Executors.newSingleThreadExecutor()

        val future = executor.submit {
            assertThrows<InterruptedException> {
                any(listOf(future1, future2, future3))
            }
        }

        Thread.sleep(100)
        future.cancel(true)

        executor.shutdownNow()
    }
}