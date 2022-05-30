package com.bkahlert.kommons.text

import com.bkahlert.kommons.text.Semantics.Symbols

public inline val Char?.spaced: String get() = this?.toString().spaced

public inline val CharSequence?.spaced: String get() = (this ?: Symbols.Null).wrap(" ")
public inline val CharSequence?.leftSpaced: String get() = (this ?: Symbols.Null).wrap(" ", "")
public inline val CharSequence?.rightSpaced: String get() = (this ?: Symbols.Null).wrap("", " ")

// TODO migrate
public inline val CharSequence?.quoted: String get() = (this ?: Symbols.Null).wrap("\"")
public inline val CharSequence?.singleQuoted: String get() = (this ?: Symbols.Null).wrap("'")
public inline val CharSequence.unquoted: String get() = "${unwrap("\"", "\'")}"
public fun CharSequence.unwrap(vararg delimiters: CharSequence): CharSequence =
    delimiters.fold(this) { unwrapped, delimiter ->
        unwrapped.removeSurrounding(delimiter = delimiter)
    }.takeUnless { it == Symbols.Null } ?: ""

public fun CharSequence?.wrap(char: Char): String = wrap("$char")
public fun CharSequence?.wrap(text: CharSequence): String = "$text${this ?: Symbols.Null}$text"
public fun <T : CharSequence, U : CharSequence> CharSequence?.wrap(pair: Pair<T, U>): String = wrap(pair.first, pair.second)
public fun CharSequence?.wrap(left: CharSequence, right: CharSequence): String = "$left${this ?: Symbols.Null}$right"

// TODO replace quoted with quote implementation
/*

/**
 * Escape string using double quotes
 */
public fun String.quote(): String = buildString { this@quote.quoteTo(this) }

private fun String.quoteTo(out: StringBuilder) {
    out.append("\"")
    for (i in 0 until length) {
        when (val ch = this[i]) {
            '\\' -> out.append("\\\\")
            '\n' -> out.append("\\n")
            '\r' -> out.append("\\r")
            '\t' -> out.append("\\t")
            '\"' -> out.append("\\\"")
            else -> out.append(ch)
        }
    }
    out.append("\"")
}

 */
