package koodies.debug

import koodies.collections.map
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.ansiRemoved
import koodies.text.Semantics.BlockDelimiters.INTROSPECTION
import koodies.text.Semantics.FieldDelimiters.FIELD
import koodies.text.Semantics.formattedAs
import koodies.text.asCodePoint
import koodies.text.wrap
import koodies.toSimpleString
import java.util.Locale

public val <T> XRay<T>.debug: XRay<T>
    get() = transform { debug }

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
        StringBuilder().apply {
            append("0x".formattedAs.debug)
            append(String.format("%02x", byte).uppercase(Locale.getDefault()))
            append(byte.asCodePoint().string.replaceNonPrintableCharacters().formattedAs.input)
        }.toString()
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
