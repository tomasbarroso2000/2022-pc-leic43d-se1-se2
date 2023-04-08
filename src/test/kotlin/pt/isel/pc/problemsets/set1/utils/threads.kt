package pt.isel.pc.problemsets.set1.utils

import kotlin.concurrent.thread

fun threadsCreate(nOfThreads: Int, block: (index: Int) -> Any?): List<Thread> =
    (0 until nOfThreads).map {
        thread {
            block(it)
        }
    }.onEach { it.join() }