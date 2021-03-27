package koodies.text

public inline val CharSequence?.quoted: String get() = (this ?: Semantics.Null).wrap("\"")
public inline val CharSequence?.singleQuoted: String get() = (this ?: Semantics.Null).wrap("'")
public inline val CharSequence.unquoted: String get() = "${unwrap("\"", "\'")}"
public fun CharSequence.unwrap(vararg delimiters: CharSequence): CharSequence =
    delimiters.fold(this) { unwrapped, delimiter ->
        unwrapped.removeSurrounding(delimiter = delimiter)
    }.takeUnless { it == Semantics.Null } ?: ""

public fun CharSequence?.wrap(value: CharSequence): String = "$value${this ?: Semantics.Null}$value"
public fun CharSequence?.wrap(left: CharSequence, right: CharSequence): String = "$left${this ?: Semantics.Null}$right"
