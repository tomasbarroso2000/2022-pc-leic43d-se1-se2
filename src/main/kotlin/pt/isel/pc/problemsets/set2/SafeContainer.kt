package pt.isel.pc.problemsets.set2

import java.util.concurrent.atomic.AtomicReference

class SafeValue<T>(val value: T, var lives: Int)
class SafeContainer<T>(values: Array<SafeValue<T>>) {

    init {
        require(values.isNotEmpty()) { "values cannot be empty" }
    }

    private class Data<T>(val index: Int, val safeValues: Array<SafeValue<T>>)
    private val data: AtomicReference<Data<T>> = AtomicReference(Data(0, values))

    fun consume(): T? {
        while (true) {
            if (Thread.interrupted()) {
                throw InterruptedException()
            }
            val observedData = data.get()
            do {
                if (observedData.index >= observedData.safeValues.size &&
                    data.compareAndSet(observedData, observedData)
                    ) return null

                if (observedData.safeValues[observedData.index].lives > 0 &&
                    data.compareAndSet(observedData, computeData(observedData))
                    ) return observedData.safeValues[observedData.index].value

            } while (data.compareAndSet(observedData, Data(observedData.index + 1, observedData.safeValues)))
        }
    }

    private fun computeData(observedData: Data<T>): Data<T> {
        val safeValues = observedData.safeValues.copyOf()
        val value = safeValues[observedData.index]
        safeValues[observedData.index] = SafeValue(value.value, value.lives - 1)
        return Data(observedData.index, safeValues)
    }
}