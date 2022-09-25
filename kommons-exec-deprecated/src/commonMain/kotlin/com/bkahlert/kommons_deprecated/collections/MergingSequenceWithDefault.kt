package com.bkahlert.kommons_deprecated.collections

/**
 * A sequence which takes the values from two parallel underlying sequences, passes them to the given
 * [transform] function and returns the values returned by that function. The sequence returns all
 * values and uses default values as soon as one of the underlying sequences stops returning values.
 */
public class MergingSequenceWithDefault<T1, T2, V>(
    private val sequence1: Sequence<T1>,
    private val sequence2: Sequence<T2>,
    private val defaultValues: Pair<T1, T2>,
    private val transform: (T1, T2) -> V,
) : Sequence<V> {
    override fun iterator(): Iterator<V> = object : Iterator<V> {
        val iterator1 = sequence1.iterator()
        val iterator2 = sequence2.iterator()
        override fun next(): V =
            transform(
                if (iterator1.hasNext()) iterator1.next() else defaultValues.first,
                if (iterator2.hasNext()) iterator2.next() else defaultValues.second)

        override fun hasNext(): Boolean = iterator1.hasNext() || iterator2.hasNext()
    }
}
