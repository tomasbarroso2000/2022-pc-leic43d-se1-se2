package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.isPast
import java.util.LinkedList
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.system.measureNanoTime
import kotlin.time.Duration

class ThreadPoolExecutor(
    private val maxThreadPoolSize: Int,
    private val keepAliveTime: Duration,
) {
    private val mLock = ReentrantLock()
    private val workerThreads: LinkedList<WorkerThreadRequest> = LinkedList<WorkerThreadRequest>()
    private val workItems: LinkedList<Runnable> = LinkedList<Runnable>()
    private var isShutdown: Boolean = false

    private class WorkerThreadRequest(
        var workItem: Runnable? = null,
        var remainingTime: Long,
    )

    private val executorCondition = mLock.newCondition()
    private val isExecutorDone: Boolean = false

    @Throws(RejectedExecutionException::class)
    fun execute(runnable: Runnable): Unit = mLock.withLock {
        val myWorkerThread = WorkerThreadRequest(
            remainingTime = keepAliveTime.inWholeNanoseconds
        )

        if (isShutdown) throw RejectedExecutionException()

        if (workerThreads.size < maxThreadPoolSize) {

            when {
                workerThreads.isEmpty() || workerThreads.none { it.workItem == null } -> {
                    if (workItems.isNotEmpty()) {
                        myWorkerThread.workItem = workItems.poll()
                        workItems.add(runnable)
                    } else {
                        myWorkerThread.workItem = runnable
                    }
                    workerThreads.push(myWorkerThread)
                    logger.info("running with thread ${myWorkerThread.hashCode()}")
                    thread {
                        myWorkerThread.workItem?.let { workerLoop(it, myWorkerThread.remainingTime) }
                    }
                }
                else -> {
                    workerThreads.firstOrNull { it.workItem == null }?.workItem =
                        if (workItems.isEmpty()) runnable
                        else {
                            workItems.add(runnable)
                            workItems.poll()
                        }
                }
            }
        } else {
            workItems.add(runnable)
        }
    }

    sealed class GetWorkItemResult {
        object Exit : GetWorkItemResult()
        class WorkItem(val workItem: Runnable) : GetWorkItemResult()
    }

    private fun getNextWorkItem(): GetWorkItemResult = mLock.withLock {
        return if (workItems.isNotEmpty()) {
            GetWorkItemResult.WorkItem(workItems.poll())
        } else {
            GetWorkItemResult.Exit
        }
    }

    // Does NOT hold the lock
    private fun workerLoop(firstRunnable: Runnable, remainingTime: Long) {
        var currentRunnable: Runnable? = firstRunnable
        val startTime = System.nanoTime()
        while ((System.nanoTime() - startTime) < remainingTime) {
            currentRunnable?.let { safeRun(it) }
            currentRunnable = when (val result = getNextWorkItem()) {
                is GetWorkItemResult.WorkItem -> result.workItem
                GetWorkItemResult.Exit -> null
            }
        }
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
}
