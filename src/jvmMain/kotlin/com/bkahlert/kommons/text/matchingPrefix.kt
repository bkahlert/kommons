package com.bkahlert.kommons.text

public fun CharSequence.matchingPrefix(vararg strings: CharSequence): String {
    val expr = strings.joinToString("|") { Regex.escape("$it") }
    return Regex("^($expr)").find(toString())?.value ?: ""
}
