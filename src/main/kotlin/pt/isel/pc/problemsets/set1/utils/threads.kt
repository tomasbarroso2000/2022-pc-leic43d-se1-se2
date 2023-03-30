package pt.isel.pc.problemsets.set1.utils

fun threadsCreate(nOfThreads: Int, block: (index: Int) -> Any?): List<Thread> =
    (0 until nOfThreads).map {
        Thread {
            block(it)
        }.apply { start() }
    }