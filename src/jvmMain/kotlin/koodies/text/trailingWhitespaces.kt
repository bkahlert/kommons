package koodies.text

import koodies.text.Unicode

val CharSequence.trailingWhitespaces: String
    get() = toString().mapCodePoints { if (it.char !in Unicode.whitespaces) null else it.char }.takeLastWhile { it != null }.joinToString("")
