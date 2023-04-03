package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.isZero
import java.util.LinkedList
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.system.measureTimeMillis
import kotlin.time.Duration

private val log = LoggerFactory.getLogger(ThreadPoolExecutor::class.java)

class ThreadPoolExecutor(
    private val maxThreadPoolSize: Int,
    private val keepAliveTime: Duration,
) {
    private val mLock = ReentrantLock()
    private val workerThreads: LinkedList<WorkerThreadRequest> = LinkedList<WorkerThreadRequest>()
    private val workItems: LinkedList<Runnable> = LinkedList<Runnable>()

    private class WorkerThreadRequest(
        var runnable: Runnable? = null,
        val condition: Condition,
        var remainingTime: Long,
    )

    @Throws(RejectedExecutionException::class)
    fun execute(runnable: Runnable): Unit = mLock.withLock {
        val myWorkerThread = WorkerThreadRequest(
            condition = mLock.newCondition(),
            remainingTime = keepAliveTime.inWholeNanoseconds
        )

        if (workerThreads.size < maxThreadPoolSize) {
            when {
                    workerThreads.isEmpty() || workerThreads.none { it.runnable == null } -> {
                    if (workItems.isNotEmpty()) {
                        myWorkerThread.runnable = workItems.poll()
                        workItems.add(runnable)
                        workerThreads.push(myWorkerThread)
                    } else {
                        myWorkerThread.runnable = runnable
                        workerThreads.push(myWorkerThread)
                    }
                }
                else -> {
                    workerThreads.firstOrNull { it.runnable == null }?.runnable =
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

        signalAllThreads()

        var remainingTime = myWorkerThread.remainingTime

        while (true) {

            if (workerThreads.firstOrNull { it.runnable != null } == myWorkerThread)
                executeAll(myWorkerThread)

            signalAllThreads()

            remainingTime = myWorkerThread.condition.awaitNanos(remainingTime)

            myWorkerThread.remainingTime = remainingTime

            if (remainingTime <= 0) {
                workerThreads.remove(myWorkerThread)
                return
            }

        }
    }

    private fun signalAllThreads() = workerThreads.filter { it.runnable != null }.forEach { it.condition.signal() }

    private fun executeAll(myWorkerThread: WorkerThreadRequest) = mLock.withLock {
        while (myWorkerThread.runnable != null && myWorkerThread.remainingTime > 0) {
            val runnable = myWorkerThread.runnable
            val timeSpent = measureTimeMillis {
                log.info("running with thread ${myWorkerThread.hashCode()}")
                Thread {
                    runnable?.run()
                }.start()
            }

            myWorkerThread.remainingTime -= timeSpent

            if (myWorkerThread.remainingTime <= 0) {
                workerThreads.remove(myWorkerThread)
                break
            } else myWorkerThread.runnable = workItems.poll()
        }
    }
}