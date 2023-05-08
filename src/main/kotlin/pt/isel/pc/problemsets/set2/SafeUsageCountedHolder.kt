package pt.isel.pc.problemsets.set2

import java.io.Closeable
import java.util.concurrent.atomic.AtomicReference

class SafeUsageCountedHolder<T : Closeable>(value: T) {

    private class Data<T>(val value: T?, val useCounter: Int)
    private val data: AtomicReference<Data<T>> = AtomicReference(Data(value, 1))

    fun tryStartUse(): T? {
        while (true) {
            val observedData = data.get()
            if (observedData.value == null) return null

            if (data.compareAndSet(observedData, Data(observedData.value, observedData.useCounter + 1)))
                return observedData.value
        }
    }

    fun endUse() {
        do {
            val observedData = data.get()
            if (observedData.useCounter == 0) throw IllegalStateException("Already closed")

            if (observedData.useCounter == 1 && data.compareAndSet(observedData, Data(null, 0))) {
                observedData.value?.close()
                return
            }

        } while (!data.compareAndSet(observedData, observedData))
    }
}