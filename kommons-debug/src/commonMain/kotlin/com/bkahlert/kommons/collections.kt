package com.bkahlert.kommons

/** Throws an [IllegalArgumentException] if the specified [collection] [isEmpty]. */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T : Collection<*>> requireNotEmpty(collection: T): T = collection.also { require(it.isNotEmpty()) }

/** Throws an [IllegalArgumentException] if the specified [array] [isEmpty]. */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T> requireNotEmpty(array: Array<T>): Array<T> = array.also { require(it.isNotEmpty()) }

/** Throws an [IllegalArgumentException] with the result of calling [lazyMessage] if the specified [collection] [isEmpty]. */
public inline fun <T : Collection<*>> requireNotEmpty(collection: T, lazyMessage: () -> Any): T = collection.also { require(it.isNotEmpty(), lazyMessage) }

/** Throws an [IllegalArgumentException] with the result of calling [lazyMessage] if the specified [array] [isEmpty]. */
public inline fun <T> requireNotEmpty(array: Array<T>, lazyMessage: () -> Any): Array<T> = array.also { require(it.isNotEmpty(), lazyMessage) }


/** Throws an [IllegalStateException] if the specified [collection] [isEmpty]. */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T : Collection<*>> checkNotEmpty(collection: T): T = collection.also { check(it.isNotEmpty()) }

/** Throws an [IllegalStateException] if the specified [array] [isEmpty]. */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T> checkNotEmpty(array: Array<T>): Array<T> = array.also { check(it.isNotEmpty()) }

/** Throws an [IllegalStateException] with the result of calling [lazyMessage] if the specified [collection] [isEmpty]. */
public inline fun <T : Collection<*>> checkNotEmpty(collection: T, lazyMessage: () -> Any): T = collection.also { check(it.isNotEmpty(), lazyMessage) }

/** Throws an [IllegalStateException] with the result of calling [lazyMessage] if the specified [array] [isEmpty]. */
public inline fun <T> checkNotEmpty(array: Array<T>, lazyMessage: () -> Any): Array<T> = array.also { check(it.isNotEmpty(), lazyMessage) }


/** Returns this collection if it [isNotEmpty] or `null`, if it is. */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T : Collection<*>> T.takeIfNotEmpty(): T? = takeIf { it.isNotEmpty() }

/** Returns this array if it [isNotEmpty] or `null`, if it is. */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T> Array<T>.takeIfNotEmpty(): Array<T>? = takeIf { it.isNotEmpty() }

/** Returns this collection if it [isNotEmpty] or `null`, if it is. */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T : Collection<*>> T.takeUnlessEmpty(): T? = takeUnless { it.isEmpty() }

/** Returns this array if it [isNotEmpty] or `null`, if it is. */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T> Array<T>.takeUnlessEmpty(): Array<T>? = takeUnless { it.isEmpty() }


/** The first element of this collection. Throws a [NoSuchElementException] if this collection is empty. */
public inline val <T> Iterable<T>.head: T get() = first()

/** The first element of this collection or `null` if this collection is empty. */
public inline val <T> Iterable<T>.headOrNull: T? get() = firstOrNull()

/** A list containing all but the first element of this collection. */
public inline val <T> Iterable<T>.tail: List<T> get() = drop(1)


/**
 * Returns an index pair with:
 * - [Pair.first] being the index of the located element, and
 * - [Pair.second] being the virtual index inside the located element.
 *
 * The length of each element is computed with the specified [length].
 *
 * @throws IndexOutOfBoundsException if the specified [index] doesn't locate an element
 */
public fun <T> Iterable<T>.locate(index: Int, length: (T) -> Int): Pair<Int, Int> {
    if (index < 0) throw IndexOutOfBoundsException("index out of range: $index")
    var virtualIndex = index
    for ((actualIndex, element) in withIndex()) {
        val elementLength = length(element)
        if (virtualIndex < elementLength) return actualIndex to virtualIndex
        else virtualIndex -= elementLength
    }
    throw IndexOutOfBoundsException("index out of range: $index")
}

/**
 * Returns a triple with:
 * - [Triple.first] being the indices of the located elements,
 * - [Triple.second] being the virtual index inside the first located element, and
 * - [Triple.third] being the virtual exclusive index inside the second located element.
 *
 * The length of each element is computed with the specified [length].
 *
 * @throws IndexOutOfBoundsException if the specified [startIndex] or [endIndex] doesn't locate an element
 */
public fun <T> Iterable<T>.locate(startIndex: Int, endIndex: Int, length: (T) -> Int): Triple<IntRange, Int, Int> {
    if (endIndex <= startIndex) throw IndexOutOfBoundsException("begin $startIndex, end $endIndex")
    val (elementStartIndex, virtualStartIndex) = locate(startIndex, length)
    val (elementEndIndex, virtualEndIndex) = locate(endIndex - 1, length)
    return (elementStartIndex..elementEndIndex) to virtualStartIndex too virtualEndIndex + 1
}
