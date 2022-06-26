package com.bkahlert.kommons.text

/**
 * Repeats this [Char] [count] times.
 */
public fun Char.repeat(count: Int): String = String(CharArray(count) { this })

/**
 * Repeats this [CharSequence] [count] times.
 */
public fun CharSequence.repeat(count: Int): String {
    val string = toString()
    return buildString { repeat(count) { append(string) } }
}
