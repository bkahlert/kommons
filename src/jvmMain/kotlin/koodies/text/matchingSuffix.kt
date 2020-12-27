package koodies.text

fun CharSequence.matchingSuffix(vararg strings: CharSequence): String {
    val expr = strings.joinToString("|") { Regex.escape("$it") }
    return Regex("($expr)\$").find(toString())?.value ?: ""
}
