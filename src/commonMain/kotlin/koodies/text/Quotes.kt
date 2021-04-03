package koodies.text

public inline val CharSequence?.spaced: String get() = (this ?: Semantics.Null).wrap(Unicode.NO_BREAK_SPACE)
public inline val CharSequence?.quoted: String get() = (this ?: Semantics.Null).wrap("\"")
public inline val CharSequence?.singleQuoted: String get() = (this ?: Semantics.Null).wrap("'")
public inline val CharSequence.unquoted: String get() = "${unwrap("\"", "\'")}"
public fun CharSequence.unwrap(vararg delimiters: CharSequence): CharSequence =
    delimiters.fold(this) { unwrapped, delimiter ->
        unwrapped.removeSurrounding(delimiter = delimiter)
    }.takeUnless { it == Semantics.Null } ?: ""

public fun CharSequence?.wrap(char: Char): String = wrap("$char")
public fun CharSequence?.wrap(text: CharSequence): String = "$text${this ?: Semantics.Null}$text"
public fun <T : CharSequence, U : CharSequence> CharSequence?.wrap(pair: Pair<T, U>): String = wrap(pair.first, pair.second)
public fun CharSequence?.wrap(left: CharSequence, right: CharSequence): String = "$left${this ?: Semantics.Null}$right"
