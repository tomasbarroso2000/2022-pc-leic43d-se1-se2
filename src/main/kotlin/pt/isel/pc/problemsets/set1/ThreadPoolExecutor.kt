package pt.isel.pc.problemsets.set1

import pt.isel.pc.problemsets.set1.utils.NodeLinkedList
import pt.isel.pc.problemsets.set1.utils.isZero
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.ReentrantLock
import kotlin.time.Duration

class ThreadPoolExecutor(
    private val maxThreadPoolSize: Int,
    private val keepAliveTime: Duration,
) {
    private val mLock = ReentrantLock()
    private val mCondition = mLock.newCondition()
    private val requests: NodeLinkedList<Request> = NodeLinkedList()
    private val threadPool: NodeLinkedList<WorkerThread> = NodeLinkedList()

    private class WorkerThread(
        val runnable: Runnable,
        val thread: Thread,
        var isDone: Boolean = false,
        var remainingNanos: Long = 0
    )

    private class Request(
        val runnable: Runnable,
        var isDone: Boolean = false
    )


    @Throws(RejectedExecutionException::class)
    fun execute(runnable: Runnable): Unit {
        require(maxThreadPoolSize > 0 && !keepAliveTime.isZero)

        //fast path
        if(requests.empty && threadPool.empty) {
            val thread = threadPool.enqueue(WorkerThread(runnable = runnable, thread = Thread()))
            thread.value.runnable.run()
            thread.value.isDone = true
            return
        }

        //wait path
        val myRequest = requests.enqueue(Request(runnable = runnable))

        while (true) {
            try {
                //Talvez trocar threadPool por uma lista
                var availableThread: WorkerThread? = null
                var headNode = threadPool.headNode

                for (item in 0 until  threadPool.count) {
                    if (headNode?.value?.isDone == true) {
                        availableThread = headNode.value
                        break
                    }

                    headNode = headNode
                }



            } catch (e: InterruptedException) {

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
}