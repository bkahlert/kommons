package com.bkahlert.kommons

/** This string escaped and wrapped with double quotes. */
public val kotlin.Char.quoted: String get() = buildString { this@quoted.quoteTo(this) }

/** This string escaped and wrapped with double quotes. */
public val CharSequence.quoted: String get() = buildString { this@quoted.quoteTo(this) }

/**
 * The string returned by [Any.toString] escaped and wrapped with double quotes,
 * or the string "null" with no quotes if this object is `null`.
 */
public val Any?.quoted: String get() = this?.toString()?.quoted ?: "null"

/** Appends this character escaped and wrapped with double quotes to the specified [out]. */
private fun kotlin.Char.quoteTo(out: StringBuilder) {
    out.append("\"")
    escapeTo(out)
    out.append("\"")
}

/** Appends this string escaped and wrapped with double quotes to the specified [out]. */
private fun CharSequence.quoteTo(out: StringBuilder) {
    out.append("\"")
    for (element in this) element.escapeTo(out)
    out.append("\"")
}

/** Appends this character escaped to the specified [out]. */
@Suppress("NOTHING_TO_INLINE")
private inline fun kotlin.Char.escapeTo(out: StringBuilder) {
    when (this) {
        '\\' -> out.append("\\\\")
        '\n' -> out.append("\\n")
        '\r' -> out.append("\\r")
        '\t' -> out.append("\\t")
        '\"' -> out.append("\\\"")
        else -> out.append(this)
    }
}
