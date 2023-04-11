package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FutureTests {

    companion object {
        private val log = LoggerFactory.getLogger(FutureTests::class.java)
    }

    @Test
    fun `callable executor simple test`() {

        val fut = Future.execute<Int> {
            Thread.sleep(1000)
            23
        }

        val num = fut.get(2000, TimeUnit.MILLISECONDS)
        assertEquals(23, num)
    }

    @Test
    fun `callable executor end with timeout exception`() {
        val fut = Future.execute<Int> {
            Thread.sleep(1000)
            23
        }
        assertFailsWith<TimeoutException> {
            fut.get(200, TimeUnit.MILLISECONDS)
        }
    }

    @Test
    fun `callable executor interrupted`() {
        val fut = Future.execute<Int> {
            Thread.sleep(1000)
            23
        }

        fut.cancel(true)

        assertFailsWith<CancellationException> {
            fut.get(2000, TimeUnit.MILLISECONDS)
        }
    }

    @Test
    fun first() {
        val nrFutures = 3
        val solutions = AtomicInteger(0)

        val futures = (0 until nrFutures).map {
            Future.execute<Int> {
                log.info("continuation $it")
                it + 1
            }
        }

        log.info("Before complete")
        while (futures.all { !it.isDone }) {
            futures.forEach {
                log.info("${it.get(1000, TimeUnit.MILLISECONDS)}")
                solutions.incrementAndGet()
            }
        }
        log.info("After complete")
        assertEquals(nrFutures, solutions.get())
    }

    @Test
    fun fifth() {
        val latch = CountDownLatch(1)

        val client: HttpClient = HttpClient.newHttpClient()

        val request = HttpRequest.newBuilder()
            .GET()
            .uri(URI("https://www.isel.pt/"))
            .build()

        val future: Future<HttpResponse<String>> = Future.execute {
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).get()
        } as Future<HttpResponse<String>>

        future.get(5000, TimeUnit.MILLISECONDS).let{
            log.info(it.body())
            latch.countDown()
        }

        latch.await()
    }

}