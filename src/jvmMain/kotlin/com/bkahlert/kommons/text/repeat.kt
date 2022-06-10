package com.bkahlert.kommons.text

/**
 * Repeats this [Char] [count] times.
 */
public fun Char.repeat(count: Int): String = String(CharArray(count) { this })

/**
 * Repeats this [CharSequence] [count] times.
 */
public fun <T : CharSequence> T.repeat(count: Int): String = buildString { repeat(count) { append(this@repeat) } }
