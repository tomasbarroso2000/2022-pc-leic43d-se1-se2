package pt.isel.pc.problemsets.set1

import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set1.utils.isZero
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.DurationUnit

/*class ThreadPoolExecutor(
    private val maxThreadPoolSize: Int,
    private val keepAliveTime: Duration,
) {
    private val log = LoggerFactory.getLogger(ThreadPoolExecutor::class.java)
    private val mLock = ReentrantLock()
    private val mCondition = mLock.newCondition()
    private val requests: NodeLinkedList<Request> = NodeLinkedList()
    private val threadPool: MutableList<WorkerThread> = mutableListOf()

    private class WorkerThread(
        var runnable: Runnable,
        val thread: Thread,
        var isDone: Boolean = false,
        var totalNanos: Long = 0
    )

    private class Request(
        val runnable: Runnable,
        var isDone: Boolean = false
    )


    @Throws(RejectedExecutionException::class)
    fun execute(runnable: Runnable): Unit {
        mLock.withLock {
            require(maxThreadPoolSize > 0 && !keepAliveTime.isZero)

            //fast path
            if(requests.empty && threadPool.isEmpty()) {
                log.info("size: ${threadPool.size}")
                log.info("fast path")
                val thread = WorkerThread(runnable = runnable, thread = Thread(), totalNanos = System.currentTimeMillis())
                threadPool.add(thread)
                thread.thread.run { thread.runnable.run() }
                thread.isDone = true
                mCondition.signalAll()
                return
            }

            //wait path
            val myRequest = requests.enqueue(Request(runnable = runnable))

            while (true) {
                log.info("size: ${threadPool.size}")
                try {
                    log.info("wait path")

                    if (requests.headNode == myRequest) {
                        if (threadPool.isEmpty()) {
                            val thread = WorkerThread(runnable = runnable, thread = Thread(), totalNanos = System.currentTimeMillis())
                            threadPool.add(thread)
                            //thread.runnable.run()
                            thread.thread.run { thread.runnable.run() }
                            thread.isDone = true
                            requests.remove(myRequest)
                            mCondition.signalAll()
                            return
                        }

                        log.info("threadpool not empty")

                        val currentTime: Long = System.currentTimeMillis()
                        //Verificar também se threadPool está vazia
                        val availableThread: WorkerThread? = threadPool.find { it.isDone }
                        if (availableThread != null) {
                            if(currentTime - availableThread.totalNanos > keepAliveTime.toLong(DurationUnit.MICROSECONDS)) {
                                threadPool.remove(availableThread)
                            } else {
                                availableThread.isDone = false
                                availableThread.runnable = runnable
                                availableThread.thread.run { availableThread.runnable.run() }
                                availableThread.isDone = true
                                requests.remove(myRequest)
                                mCondition.signalAll()
                                return
                            }
                        }

                    }

                    mCondition.await()

                } catch (e: InterruptedException) {
                    TODO()
                }
            }
        }
    }

    fun shutdown(): Unit {
        mLock.withLock {
            TODO()
        }
    }

    @Throws(InterruptedException::class)
    fun awaitTermination(timeout: Duration): Boolean {
        mLock.withLock {
            TODO()
        }
    }
}*/