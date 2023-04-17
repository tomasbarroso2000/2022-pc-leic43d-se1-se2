package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.isZero
import java.util.concurrent.Callable
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.time.Duration
import java.util.*

class ThreadPoolExecutor(
    private val maxThreadPoolSize: Int,
    private val keepAliveTime: Duration,
) {
    init {
        require(maxThreadPoolSize > 0 && !keepAliveTime.isZero)
    }

    private val lock = ReentrantLock()
    private val workerThreads: LinkedList<WorkerThreadRequest> = LinkedList<WorkerThreadRequest>()
    private val workItems: LinkedList<Runnable> = LinkedList<Runnable>()

    private class WorkerThreadRequest(
        var workItem: Runnable? = null,
        var remainingTime: Long,
        var thread: Thread? = null
    )

    private var isShutdown: Boolean = false
    private val executorCondition = lock.newCondition()
    private var isExecutorDone: Boolean = false

    @Throws(RejectedExecutionException::class)
    fun execute(runnable: Runnable): Unit = lock.withLock {
        if (isShutdown) throw RejectedExecutionException("Cannot execute after shutdown")

        val myWorkerThread = WorkerThreadRequest(runnable, keepAliveTime.inWholeNanoseconds, Thread {})

        if (workerThreads.size < maxThreadPoolSize) {
            when {
                workerThreads.none { it.workItem == null } -> {
                    if (workItems.isNotEmpty()) {
                        myWorkerThread.workItem = workItems.poll()
                        workItems.add(runnable)
                    }
                    workerThreads.add(myWorkerThread)
                    myWorkerThread.thread = thread {
                        workerLoop(myWorkerThread)
                    }
                }
                else -> {
                    workerThreads.firstOrNull { it.workItem == null }?.let {
                        it.workItem = if (workItems.isNotEmpty()) {
                            workItems.add(runnable)
                            workItems.poll()
                        } else runnable
                    }
                }
            }

        } else {
            workItems.add(runnable)
        }
    }

    fun shutdown(): Unit = lock.withLock {
        if (!isShutdown)
            isShutdown = true
    }

    @Throws(InterruptedException::class)
    fun awaitTermination(timeout: Duration): Boolean {
        lock.withLock {
            require(isShutdown) {"ThreadPoolExecutor is not in shutdown mode"}

            //fast path
            if (isExecutorDone) return false

            //wait path
            var remainingTime = timeout.inWholeNanoseconds

            while (true) {
                try {
                    remainingTime = executorCondition.awaitNanos(remainingTime)

                    if (isExecutorDone) return true

                    if (remainingTime <= 0) {
                        finishAll()
                        logger.info("timeout")
                        return false
                    }
                } catch(e: InterruptedException) {
                    if (isExecutorDone) {
                        Thread.currentThread().interrupt()
                        return true
                    }
                    finishAll()
                    throw e
                }
            }
        }
    }

    fun <T> execute(callable: Callable<T>): Future<T> {
        val runnable = Runnable {
            try {
                callable.call()
            } catch (e: Exception) {
                logger.warn("${e.message}")
            }
        }
        execute(runnable)
        return Future(callable)
    }

    sealed class GetWorkItemResult {
        object Exit : GetWorkItemResult()
        class WorkItem(val workItem: Runnable) : GetWorkItemResult()
    }

    private fun getNextWorkItem(workerThread: WorkerThreadRequest): GetWorkItemResult = lock.withLock {
        val workItem = workerThread.workItem
        workerThread.workItem = null
        when {
            workItem != null -> GetWorkItemResult.WorkItem(workItem)
            workItems.isNotEmpty() -> GetWorkItemResult.WorkItem(workItems.poll())
            else -> GetWorkItemResult.Exit
        }
    }

    private fun signalThreadPoolEnd(workerThread: WorkerThreadRequest) = lock.withLock {
        workerThreads.remove(workerThread)
        if (workerThreads.isEmpty()) {
            isExecutorDone = true
            executorCondition.signalAll()
        }
    }

    // Does NOT hold the lock
    private fun workerLoop(workerThread: WorkerThreadRequest) {
        var currentRunnable: Runnable? = workerThread.workItem
        workerThread.workItem = null

        logger.info("running with thread ${workerThread.hashCode()}")

        val startTime = System.nanoTime()
        while (System.nanoTime() - startTime < workerThread.remainingTime) {
            currentRunnable?.let { safeRun(it) }

            currentRunnable = when (val result = getNextWorkItem(workerThread)) {
                is GetWorkItemResult.WorkItem -> result.workItem
                GetWorkItemResult.Exit -> null
            }
        }

        signalThreadPoolEnd(workerThread)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThreadPoolExecutor::class.java)

        private fun safeRun(runnable: Runnable) {
            try {
                runnable.run()
            } catch (ex: Throwable) {
                logger.warn("Unexpected exception while running work item, ignoring it")
                // ignoring exception
            }
        }
    }

    private fun finishAll() = lock.withLock {
        workerThreads.forEach {
            it.thread?.interrupt()
        }
        workItems.clear()
        workerThreads.clear()
    }
}