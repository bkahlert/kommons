package koodies.text

/**
 * Returns this [CharSequence] truncated to [length] and if necessary padded from the end.
 */
fun CharSequence.padEndFixedLength(
    length: Int = 15,
    strategy: TruncationStrategy = TruncationStrategy.END,
    marker: String = "…",
    padChar: Char = ' ',
): CharSequence =
    strategy.truncate(toString(), length, marker).padEnd(length, padChar).toString()
