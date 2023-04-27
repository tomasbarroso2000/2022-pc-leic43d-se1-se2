package pt.isel.pc.problemsets.set2

import java.io.Closeable
import java.util.concurrent.atomic.AtomicReference

class SafeUsageCountedHolder<T : Closeable>(value: T) {

    private class Data<T>(val value: T?, val useCounter: Int)
    private val data: AtomicReference<Data<T>> = AtomicReference(Data(value, 1))

    fun tryStartUse(): T? {
        while (true) {
            val observedData = data.get()
            if (observedData == data.get() && observedData == null) return null

            if (observedData.value != null && data.compareAndSet(observedData, Data(observedData.value, observedData.useCounter + 1)))
                return observedData.value
        }
    }

    fun endUse() {
        while (true) {
            val observedData = data.get()
            if (observedData == data.get() && observedData.useCounter == 0) throw IllegalStateException("Already closed")

            val newData = Data(observedData.value, observedData.useCounter - 1)
            if (newData.useCounter == 0 && data.compareAndSet(observedData, Data(null, 0))) {
                newData.value?.close()
                return
            }
            if (data.compareAndSet(observedData, newData)) return
        }
    }
}