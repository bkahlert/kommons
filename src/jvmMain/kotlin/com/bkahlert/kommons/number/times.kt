package com.bkahlert.kommons.number

/**
 * Calls the [function] [this] times.
 *
 * Examples:
 * ```
 * 3 * { i -> println(i) }
 * ```
 *
 * ```
 * 8 times { _ -> action() }
 * ```
 */
public inline infix operator fun <reified T : Number> T.times(function: (Int) -> Unit) {
    (0..this.toInt()).forEach { i -> function(i) }
}
