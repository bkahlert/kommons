package com.bkahlert.kommons.collections

import java.util.stream.Stream
import kotlin.streams.asStream

// TODO delete
/**
 * Creates a [Stream] instance that wraps this array returning its elements when being iterated.
 */
public fun <T> Array<T>.asStream(): Stream<T> = asSequence().asStream()

/**
 * Creates a [Stream] instance that wraps this list returning its elements when being iterated.
 */
public fun <T> List<T>.asStream(): Stream<T> = stream()

/**
 * Creates a [Stream] instance that wraps this iterable returning its elements when being iterated.
 */
public fun <T> Iterable<T>.asStream(): Stream<T> = asSequence().asStream()

/**
 * Creates a [Stream] instance that wraps this iterator returning its elements when being iterated.
 */
public fun <T> Iterator<T>.asStream(): Stream<T> = asSequence().asStream()
