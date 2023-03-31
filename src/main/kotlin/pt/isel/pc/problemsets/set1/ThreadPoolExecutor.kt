package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.isZero
import java.lang.System.currentTimeMillis
import java.time.LocalDate
import java.util.LinkedList
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.system.measureTimeMillis
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private val log = LoggerFactory.getLogger(ThreadPoolExecutor::class.java)

class ThreadPoolExecutor(
    private val maxThreadPoolSize: Int,
    private val keepAliveTime: Duration,
) {
    private val mLock = ReentrantLock()
    private val workerThreads: LinkedList<WorkerThreadRequest> = LinkedList<WorkerThreadRequest>()
    private val workItems: LinkedList<Runnable> = LinkedList<Runnable>()

    private class WorkerThreadRequest(
        var runnable: Runnable?,
        val condition: Condition,
    ) { var isDone: Boolean = false }

    @Throws(RejectedExecutionException::class)
    fun execute(runnable: Runnable): Unit = mLock.withLock {
        val myWorkerThread = WorkerThreadRequest(
            runnable = null,
            condition = mLock.newCondition()
        )

        if (workerThreads.size < maxThreadPoolSize) {
            when {
                workerThreads.isEmpty() || !workerThreads.peekFirst().isDone -> {
                    if (workItems.isEmpty()) myWorkerThread.runnable = runnable
                    else {
                        myWorkerThread.runnable = workItems.poll()
                        workItems.add(runnable)
                    }
                    workerThreads.push(myWorkerThread)
                }
                else -> {
                    workerThreads.peekFirst().runnable =
                        if (workItems.isEmpty()) runnable
                        else {
                            workItems.add(runnable)
                            workItems.poll()
                        }
                }
            }
            signalAllThreads()
        } else {
            workItems.add(runnable)
            signalAllThreads()
        }

        var remainingTime = keepAliveTime.inWholeNanoseconds

        while (workerThreads.size != 0) {
            remainingTime = myWorkerThread.condition.awaitNanos(remainingTime)
            //correr runnables
            if (!myWorkerThread.isDone) {
                var currentRunnable = myWorkerThread.runnable
                while (true) {
                    if (currentRunnable == null) break
                    else {
                        myWorkerThread.isDone = true
                        log.info("running with thread ${myWorkerThread.hashCode()}")
                        currentRunnable.run()
                        if (workItems.isNotEmpty()) currentRunnable = workItems.poll()
                        else {
                            myWorkerThread.isDone = true
                            myWorkerThread.runnable = null
                            break
                        }
                    }
                }
            }

            if (remainingTime <= 0) {
                workerThreads.remove(myWorkerThread)
            }
        }

    }

    private fun signalAllThreads() {
        workerThreads.forEach {
            if (it.runnable == null) it.isDone = true
        }
    }
}
