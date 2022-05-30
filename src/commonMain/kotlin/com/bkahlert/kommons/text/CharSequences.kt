package com.bkahlert.kommons.text

/** Concatenates characters in this [CharSequence] into a String. */
@Suppress("NOTHING_TO_INLINE")
public inline fun CharSequence.concatToString(): String =
    toList().toCharArray().concatToString()
