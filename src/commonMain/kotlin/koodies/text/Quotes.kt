package koodies.text

inline val CharSequence?.quoted: String get() = (this ?: "␀").wrap("\"")
inline val CharSequence?.singleQuoted: String get() = (this ?: "␀").wrap("'")
inline val CharSequence.unquoted: String get() = "${unwrap("\"", "\'")}"
fun CharSequence.unwrap(vararg delimiters: CharSequence): CharSequence =
    delimiters.fold(this) { unwrapped, delimiter ->
        unwrapped.removeSurrounding(delimiter = delimiter)
    }.takeUnless { it == "␀" } ?: ""

fun CharSequence?.wrap(value: CharSequence): String = "$value${this ?: "␀"}$value"
fun CharSequence?.wrap(left: CharSequence, right: CharSequence): String = "$left${this ?: "␀"}$right"
