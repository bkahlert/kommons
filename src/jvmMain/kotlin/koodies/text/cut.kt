package koodies.text

fun CharSequence.cut(position: UInt): Pair<CharSequence, CharSequence> {
    val cutPosition = position.toInt().coerceAtMost(length)
    return subSequence(0, cutPosition) to subSequence(cutPosition, length)
}
