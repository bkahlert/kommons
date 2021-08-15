package com.bkahlert.kommons.collections

/**
 * Returns an array containing all elements of the original array and then the given [element].
 */
public inline operator fun <reified T, reified U : T> Array<out T>.plus(element: U): Array<out T> =
    toMutableList().apply { add(element) }.toTypedArray()

/**
 * Returns an array containing all elements of the original array and then the given [elements] array.
 */
public inline operator fun <reified T, reified U : T> Array<out T>.plus(elements: Array<U>): Array<out T> =
    toMutableList().apply { addAll(elements) }.toTypedArray()

/**
 * Returns an array containing all elements of the original array and then the given [elements] collection.
 */
public inline operator fun <reified T, reified U : T> Array<out T>.plus(elements: Collection<U>): Array<out T> =
    toMutableList().apply { addAll(elements) }.toTypedArray()

/**
 * Returns an array containing all elements of the original array and then the given [elements] sequence.
 */
public inline operator fun <reified T, reified U : T> Array<out T>.plus(elements: Sequence<U>): Array<out T> =
    toMutableList().apply { addAll(elements) }.toTypedArray()
