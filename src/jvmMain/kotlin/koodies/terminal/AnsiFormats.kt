package koodies.terminal

object AnsiFormats {
    fun CharSequence.bold() = ANSI.termColors.bold("$this")
    fun CharSequence.dim() = ANSI.termColors.dim("$this")
    fun CharSequence.italic() = ANSI.termColors.italic("$this")
    fun CharSequence.underline() = ANSI.termColors.underline("$this")
    fun CharSequence.inverse() = ANSI.termColors.inverse("$this")
    fun CharSequence.hidden(): String = if (IDE.isIntelliJ) " ".repeat((length * 1.35).toInt()) else ANSI.termColors.hidden("$this")
    fun CharSequence.strikethrough() = ANSI.termColors.strikethrough("$this")
}
