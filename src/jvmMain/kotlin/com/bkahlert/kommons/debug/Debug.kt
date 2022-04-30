package com.bkahlert.kommons.debug

import com.bkahlert.kommons.collections.map
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.ANSI.ansiRemoved
import com.bkahlert.kommons.text.Semantics.BlockDelimiters.INTROSPECTION
import com.bkahlert.kommons.text.Semantics.FieldDelimiters.FIELD
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.text.asCodePoint
import com.bkahlert.kommons.text.wrap
import com.bkahlert.kommons.toSimpleString
import java.util.Locale

public val <T> XRay<T>.debug: XRay<T>
    get() = transform { it.debug }

public inline val CharSequence?.debug: String
    get() = if (this == null) null.wrap(INTROSPECTION.map { it.formattedAs.debug })
    else toString().replaceNonPrintableCharacters().wrap("❬".formattedAs.debug,
        FIELD.ansiRemoved.formattedAs.debug + "${this.length}".ansi.gray + "❭".formattedAs.debug)
public inline val <T> Iterable<T>?.debug: String get() = this?.joinToString("") { it.toString().debug }.debug
public inline val List<Byte>?.debug: String get() = this?.toByteArray()?.let { bytes: ByteArray -> String(bytes) }.debug
public inline val Char?.debug: String get() = this.toString().replaceNonPrintableCharacters().wrap("❬", "❭")

/**
 * Contains this byte array in its debug form, e.g. `❬0x80, 0xFFÿ, 0x00␀, 0x01␁, 0x7F␡❭`
 */
public inline val Byte?.debug: String
    get() = this?.let { byte: Byte ->
        buildString {
            append("0x".formattedAs.debug)
            append(String.format("%02x", byte).uppercase(Locale.getDefault()))
            append(byte.asCodePoint().string.replaceNonPrintableCharacters().formattedAs.input)
        }
    }.let { it.wrap(INTROSPECTION.first.formattedAs.debug, INTROSPECTION.second.formattedAs.debug) }
public val Array<*>?.debug: String
    get() = this?.joinToString(",") {
        with(INTROSPECTION.map { it.formattedAs.debug }) {
            it.debug.removeSurrounding(first, second)
        }
    }
        .let { it.wrap("【".formattedAs.debug, "】".formattedAs.debug) }
public inline val Boolean?.debug: String get() = asEmoji
public inline val Any?.debug: String
    get() = when (this) {
        null -> "❬null❭"
        is Iterable<*> -> this.debug
        is CharSequence -> this.debug
        is ByteArray -> this.toList().toTypedArray().debug
        is Array<*> -> this.debug
        is Function<*> -> this.toSimpleString()
        is Byte -> this.debug
        else -> toString().debug
    }
