package com.bkahlert.kommons.collections


/**
 * Returns a sequence of values built from the elements of this sequence and the [other] sequence with the same index
 * using the provided [transform] function applied to each pair of elements.
 * The resulting sequence ends as soon as the longest input sequence ends—filling missing values with [default].
 *
 * The operation is _intermediate_ and _stateless_.
 */
public fun <T, R, V> Sequence<T>.zipWithDefault(other: Sequence<R>, default: Pair<T, R>, transform: (a: T, b: R) -> V): Sequence<V> {
    return MergingSequenceWithDefault(this, other, default, transform)
}
