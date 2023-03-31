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
        var remainingTime: Duration,
        var runnable: Runnable?,
    ) { var isDone: Boolean = false }

    @Throws(RejectedExecutionException::class)
    fun execute(runnable: Runnable): Unit {
        mLock.withLock {

            val myWorkerThread = WorkerThreadRequest(remainingTime = keepAliveTime, runnable = null)

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
                    else -> workerThreads.peekFirst().runnable =
                            if (workItems.isEmpty()) runnable
                            else workItems.poll()
                }
            } else {
                workItems.add(runnable)
            }

            while (workerThreads.isNotEmpty()) {
                val workerThread = workerThreads.poll()
                //val startTime: Duration = currentTimeMillis().toDuration(DurationUnit.NANOSECONDS)
                val elapsed = measureTimeMillis {
                    if (!workerThread.remainingTime.isZero) {
                        val runnable = workerThread.runnable
                        if (runnable != null) {
                            Thread {
                                workerThread.isDone = false
                                workerLoop(workerThread, runnable)
                                //workerThread.runnable = null
                                workerThread.isDone = true
                            }.start()
                        }
                    } else return
                }

                if (workerThread.remainingTime.inWholeMilliseconds - elapsed <= 0)
                    workerThread.remainingTime = 0.toDuration(DurationUnit.MILLISECONDS)
                else
                    workerThread.remainingTime =
                        (workerThread.remainingTime.inWholeMilliseconds - elapsed).
                        toDuration(DurationUnit.MILLISECONDS)

                log.info("time: ${workerThread.remainingTime}")
                workerThreads.add(workerThread)
            }
        }
    }

    fun shutdown(): Unit {
        TODO()
    }

    @Throws(InterruptedException::class)
    fun awaitTermination(timeout: Duration): Boolean {
        TODO()
    }

    private fun workerLoop(workerThread: WorkerThreadRequest, runnable: Runnable) {
        var currentRunnable = runnable
        while (true) {
            currentRunnable.run()
            if (workItems.isNotEmpty()) currentRunnable = workItems.poll()
            else {
                //workerThread.runnable = null
                return
            }
        }
    }
}
