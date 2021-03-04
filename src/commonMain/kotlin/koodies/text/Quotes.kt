package koodies.text

public inline val CharSequence?.quoted: String get() = (this ?: "␀").wrap("\"")
public inline val CharSequence?.singleQuoted: String get() = (this ?: "␀").wrap("'")
public inline val CharSequence.unquoted: String get() = "${unwrap("\"", "\'")}"
public fun CharSequence.unwrap(vararg delimiters: CharSequence): CharSequence =
    delimiters.fold(this) { unwrapped, delimiter ->
        unwrapped.removeSurrounding(delimiter = delimiter)
    }.takeUnless { it == "␀" } ?: ""

public fun CharSequence?.wrap(value: CharSequence): String = "$value${this ?: "␀"}$value"
public fun CharSequence?.wrap(left: CharSequence, right: CharSequence): String = "$left${this ?: "␀"}$right"
