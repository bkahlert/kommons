package koodies.debug

import koodies.debug.Debug.DELIM
import koodies.debug.Debug.defaultAnsiPrefix
import koodies.debug.Debug.defaultAnsiSuffix
import koodies.debug.Debug.meta
import koodies.debug.Debug.secondaryMeta
import koodies.terminal.AnsiColors.brightCyan
import koodies.terminal.AnsiColors.cyan
import koodies.terminal.AnsiColors.gray
import koodies.text.ANSI.escapeSequencesRemoved
import koodies.text.Semantics
import koodies.text.asCodePoint
import koodies.text.withoutPrefix
import koodies.text.withoutSuffix
import koodies.text.wrap

public object Debug {
    public fun CharSequence.meta(): String = brightCyan()
    public fun CharSequence.secondaryMeta(): String = cyan()
    public const val defaultPrefix: String = "❬"
    public const val defaultSuffix: String = "❭"
    public val defaultAnsiPrefix: String = defaultPrefix.meta()
    public val defaultAnsiSuffix: String = defaultSuffix.meta()
    public fun wrap(text: CharSequence?, prefix: String = defaultPrefix, suffix: String = defaultSuffix): String =
        text?.wrap(prefix.meta(), suffix.meta()) ?: null.wrap("❬".meta(), "❭".meta())

    public val DELIM: String = Semantics.Delimiter.escapeSequencesRemoved
}

public val <T> XRay<T>.debug: XRay<T>
    get() = transform { debug }

public inline val CharSequence?.debug: String
    get() = if (this == null) null.wrap("❬".meta(), "❭".meta())
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
        it.debug.withoutPrefix(defaultAnsiPrefix).withoutSuffix(defaultAnsiSuffix)
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
