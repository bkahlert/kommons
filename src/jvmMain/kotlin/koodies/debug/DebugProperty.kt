package koodies.debug

import koodies.debug.Debug.DELIM
import koodies.debug.Debug.defaultEnclosement
import koodies.debug.Debug.meta
import koodies.debug.Debug.secondaryMeta
import koodies.terminal.AnsiColors.brightCyan
import koodies.terminal.AnsiColors.cyan
import koodies.terminal.AnsiColors.gray
import koodies.text.ANSI.ansiRemoved
import koodies.text.Semantics
import koodies.text.Semantics.Enclosements.introspection
import koodies.text.Semantics.formattedAs
import koodies.text.asCodePoint
import koodies.text.withoutPrefix
import koodies.text.withoutSuffix
import koodies.text.wrap

public object Debug {
    public fun CharSequence.meta(): String = brightCyan()
    public fun CharSequence.secondaryMeta(): String = cyan()
    public val defaultEnclosement: Pair<String, String> = introspection.formatAs { debug }
    public fun wrap(text: CharSequence?, prefix: String = introspection.open, suffix: String = introspection.end): String =
        text?.wrap(prefix.meta(), suffix.meta()) ?: null.wrap("❬".meta(), "❭".meta())

    public val DELIM: String = Semantics.Delimiter.ansiRemoved.formattedAs.debug
}

public val <T> XRay<T>.debug: XRay<T>
    get() = transform { debug }

public inline val CharSequence?.debug: String
    get() = if (this == null) null.wrap(defaultEnclosement)
    else toString().replaceNonPrintableCharacters().wrap("❬".meta(), DELIM.meta() + "${this.length}".gray() + "❭".meta())
public inline val <T> Iterable<T>?.debug: String get() = this?.joinToString("") { it.toString().debug }.debug
public inline val List<Byte>?.debug: String get() = this?.toByteArray()?.let { bytes: ByteArray -> String(bytes) }.debug
public inline val Char?.debug: String get() = this.toString().replaceNonPrintableCharacters().wrap("❬", "❭")

/**
 * Contains this byte array in its debug form, e.g. `❬0x80, 0xFFÿ, 0x00␀, 0x01␁, 0x7F␡❭`
 */
public inline val Byte?.debug: String
    get() = this?.let { byte: Byte ->
        StringBuilder().apply {
            append("0x".meta())
            append(String.format("%02x", byte).toUpperCase())
            append(byte.asCodePoint().string.replaceNonPrintableCharacters().secondaryMeta())
        }.toString()
    }.let { Debug.wrap(it) }
public val Array<*>?.debug: String
    get() = this?.joinToString(",") {
        it.debug.withoutPrefix(defaultEnclosement.first).withoutSuffix(defaultEnclosement.second)
    }.let { Debug.wrap(it, prefix = "【", suffix = "】") }
public inline val Boolean?.debug: String get() = asEmoji
public inline val Any?.debug: String
    get() = when (this) {
        null -> "❬null❭"
        is Iterable<*> -> this.debug
        is CharSequence -> this.debug
        is ByteArray -> this.toList().toTypedArray().debug
        is Array<*> -> this.debug
        is Byte -> this.debug
        else -> toString().debug
    }
