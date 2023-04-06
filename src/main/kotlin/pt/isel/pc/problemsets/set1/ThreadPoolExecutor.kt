package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
import java.util.*
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

    private class WorkerThreadRequest(
        var workItem: Runnable? = null,
        val condition: Condition,
        var remainingTime: Long
    )

    private var isShutdown: Boolean = false
    private val executorCondition = mLock.newCondition()
    private val isExecutorDone: Boolean = false
    private val mCondition = mLock.newCondition()

    @Throws(RejectedExecutionException::class)
    fun execute(runnable: Runnable): Unit = mLock.withLock {
        if (isShutdown) throw RejectedExecutionException("Cannot execute after shutdown")

        val myWorkerThread = WorkerThreadRequest(
            runnable,
            mLock.newCondition(),
            keepAliveTime.inWholeNanoseconds
        )

        if (workerThreads.size < maxThreadPoolSize) {
            when {
                workerThreads.none { it.workItem == null } -> {
                    if (workItems.isNotEmpty()) {
                        myWorkerThread.workItem = workItems.poll()
                        workItems.add(runnable)
                    }
                    workerThreads.add(myWorkerThread)
                    thread {
                        workerLoop(myWorkerThread)
                    }
                }

                else -> {
                    val workerThread = workerThreads.firstOrNull { it.workItem == null }
                    if (workerThread != null) {
                        workerThread.workItem = if (workItems.isNotEmpty()) {
                            workItems.add(runnable)
                            workItems.poll()
                        } else runnable
                        workerThread.condition.signalAll()
                    }
                }
            }

        } else {
            workItems.add(runnable)
            workerThreads.firstOrNull { it.workItem == null }?.condition?.signalAll()
        }
    }

    sealed class GetWorkItemResult {
        object Exit : GetWorkItemResult()
        class WorkItem(val workItem: Runnable) : GetWorkItemResult()
    }

    private fun getNextWorkItem(workerThread: WorkerThreadRequest, elapsed: Long): GetWorkItemResult = mLock.withLock {
        workerThread.remainingTime -= elapsed
        if (workItems.isNotEmpty()) {
            GetWorkItemResult.WorkItem(workItems.poll())
        } else {
            workerThread.remainingTime = workerThread.condition.awaitNanos(workerThread.remainingTime)
            workerThread.workItem = null
            GetWorkItemResult.Exit
        }
    }

    // Does NOT hold the lock
    private fun workerLoop(workerThread: WorkerThreadRequest) {
        var currentRunnable: Runnable? = workerThread.workItem
        logger.info("running with thread ${workerThread.hashCode()}")
        while (workerThread.remainingTime > 0) {
            val elapsed = if (currentRunnable != null) safeRun(currentRunnable)
                            else 0L

            currentRunnable = when (val result = getNextWorkItem(workerThread, elapsed)) {
                is GetWorkItemResult.WorkItem -> result.workItem
                GetWorkItemResult.Exit -> null
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThreadPoolExecutor::class.java)

        private fun safeRun(runnable: Runnable): Long =
            try {
                measureNanoTime { runnable.run() }
            } catch (ex: Throwable) {
                logger.warn("Unexpected exception while running work item, ignoring it")
                0L
                // ignoring exception
            }
    }
}
