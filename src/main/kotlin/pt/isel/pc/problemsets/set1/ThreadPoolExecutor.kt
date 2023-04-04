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

private val log = LoggerFactory.getLogger(ThreadPoolExecutor::class.java)

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
        val condition: Condition,
        var remainingTime: Duration,
    )

    private val executorCondition = mLock.newCondition()
    private val isExecutorDone: Boolean = false

    @Throws(RejectedExecutionException::class)
    fun execute(runnable: Runnable): Unit = mLock.withLock {
        val myWorkerThread = WorkerThreadRequest(
            condition = mLock.newCondition(),
            remainingTime = keepAliveTime
        )

        if (isShutdown) throw RejectedExecutionException()

        if (workerThreads.size < maxThreadPoolSize) {
            when {
                workerThreads.isEmpty() || workerThreads.none { it.workItem == null } -> {
                    if (workItems.isNotEmpty()) {
                        myWorkerThread.workItem = workItems.poll()
                        workItems.add(runnable)
                        workerThreads.push(myWorkerThread)
                    } else {
                        myWorkerThread.workItem = runnable
                        workerThreads.push(myWorkerThread)
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
            return
        }

        signalWorkerThreads()

        var remainingTime = myWorkerThread.remainingTime.inWholeNanoseconds

        while (true) {
            if (remainingTime <= 0) {
                workerThreads.remove(myWorkerThread)
                return
            }

            if (workerThreads.firstOrNull { it.workItem != null } == myWorkerThread) {

                thread {
                    log.info("worker loop with thread ${myWorkerThread.hashCode()}")
                    myWorkerThread.workItem?.let { remainingTime -= workerLoop(it, remainingTime) }
                    myWorkerThread.workItem = null
                }
            }

            remainingTime = myWorkerThread.condition.awaitNanos(remainingTime)
        }
    }

    fun shutdown(): Unit = mLock.withLock {
        //realizar as verificações todas
        //ver se ainda á trabalho para fazer
        //ver se as threads ainda estão a trabalhar mas
        isShutdown = true
        //corpo do shutdown



        //isShutDownDone = true
        //shutDownCondition.signal()
    }

    @Throws(InterruptedException::class)
    fun awaitTermination(timeout: Duration): Boolean {
        mLock.withLock {
            var remainingTime = timeout.inWholeNanoseconds
            while (true) {
                remainingTime = executorCondition.awaitNanos(remainingTime)

                if (isExecutorDone) return true

                if (remainingTime <= 0) {
                    //sair timeout
                }
            }
        }
    }

    private fun signalWorkerThreads() = mLock.withLock {
        workerThreads.firstOrNull { it.workItem != null }?.condition?.signal()
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
    private fun workerLoop(firstRunnable: Runnable, remainingTime: Long): Long {
        var elapsedTime = 0L
        var currentRunnable = firstRunnable

        while (true) {
            if (remainingTime - elapsedTime <= 0) return elapsedTime

            elapsedTime += measureNanoTime{
                safeRun(currentRunnable)
            }

            currentRunnable = when (val result = getNextWorkItem()) {
                is GetWorkItemResult.WorkItem -> result.workItem
                GetWorkItemResult.Exit -> return elapsedTime
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