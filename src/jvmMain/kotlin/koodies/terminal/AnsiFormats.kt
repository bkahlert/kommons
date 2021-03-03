package koodies.terminal

public object AnsiFormats {
    public fun CharSequence.bold(): String = ANSI.termColors.bold("$this")
    public fun CharSequence.dim(): String = ANSI.termColors.dim("$this")
    public fun CharSequence.italic(): String = ANSI.termColors.italic("$this")
    public fun CharSequence.underline(): String = ANSI.termColors.underline("$this")
    public fun CharSequence.inverse(): String = ANSI.termColors.inverse("$this")
    public fun CharSequence.hidden(): String = if (IDE.isIntelliJ) " ".repeat((length * 1.35).toInt()) else ANSI.termColors.hidden("$this")
    public fun CharSequence.strikethrough(): String = ANSI.termColors.strikethrough("$this")
}
