package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.isZero
import java.util.LinkedList
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.DurationUnit

class ThreadPoolExecutor(
    private val maxThreadPoolSize: Int,
    private val keepAliveTime: Duration,
) {

    private val mLock = ReentrantLock()
    private val workerThreads: LinkedList<WorkerThreadRequest> = LinkedList<WorkerThreadRequest>()
    private val workItems: LinkedList<Runnable> = LinkedList<Runnable>()

    private class WorkerThreadRequest(
        val remainingTime: Duration,
        var runnable: Runnable?,
    ) { var isDone: Boolean = false }

    @Throws(RejectedExecutionException::class)
    fun execute(runnable: Runnable): Unit {
        mLock.withLock {

            val myWorkerThread = WorkerThreadRequest(remainingTime = keepAliveTime, runnable = null)

            //fast path
            if (workerThreads.size < maxThreadPoolSize) {

                if(workerThreads.isEmpty() && workItems.isEmpty()) {
                    myWorkerThread.runnable = runnable
                    workerThreads.push(myWorkerThread)
                }

                if (workerThreads.peekFirst()?.isDone == false) {
                    if (workItems.isEmpty()) myWorkerThread.runnable = runnable
                    else {
                        myWorkerThread.runnable = workItems.poll()
                    }
                    workerThreads.push(myWorkerThread)
                } else {
                    workerThreads.peekFirst().runnable =
                        if (workItems.isEmpty()) runnable
                        else workItems.poll()
                }
            } else {
                workItems.add(runnable)
            }

            while (workerThreads.isNotEmpty()) {
                val workerThread = workerThreads.poll()
                if (!workerThread.remainingTime.isZero) {
                    val runnable = workerThread.runnable
                    if (runnable != null) {
                        Thread {
                            workerThread.isDone = true
                            workerLoop(runnable)
                        }.start()
                    } else return
                }
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

    private fun workerLoop(runnable: Runnable) {
        var currentRunnable = runnable
        while (true) {
            currentRunnable.run()
            if (workItems.isNotEmpty()) currentRunnable = workItems.poll()
            else return
        }
    }
}