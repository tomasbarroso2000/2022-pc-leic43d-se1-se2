package pt.isel.pc.problemsets.set2

import org.junit.jupiter.api.Test
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class CompletableFutureTests {
    @Test
    fun `simple test`() {
        val executor = Executors.newSingleThreadExecutor()
        val future = executor.submit(Callable {
            Thread.sleep(2000)
            "Hello, World!"
        })
        val list = listOf(future)
        val compFuture: CompletableFuture<String> = any(list)
        println(compFuture.get())
    }
}