package com.bkahlert.kommons.text

/** Concatenates characters in this [CharSequence] into a String. */
public inline fun CharSequence.concatToString(): String =
    toList().toCharArray().concatToString()
