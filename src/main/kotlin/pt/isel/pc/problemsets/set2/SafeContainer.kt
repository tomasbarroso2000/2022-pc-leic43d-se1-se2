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
        val index = observedData.index
        val newSafeValues = observedData.safeValues.clone()
        newSafeValues[index] = SafeValue(newSafeValues[index].value, newSafeValues[index].lives - 1)
        return Data(observedData.index, newSafeValues)
    }

}