package koodies.terminal

import koodies.text.ANSI.Text.Companion.ansi

@Deprecated("use ansi instead")
public object AnsiFormats {
    @Deprecated("use ansi instead")
    public fun CharSequence.bold(): String = !ansi.bold

    @Deprecated("use ansi instead")
    public fun CharSequence.dim(): String = !ansi.dim

    @Deprecated("use ansi instead")
    public fun CharSequence.italic(): String = !ansi.italic

    @Deprecated("use ansi instead")
    public fun CharSequence.underline(): String = !ansi.underline

    @Deprecated("use ansi instead")
    public fun CharSequence.inverse(): String = !ansi.inverse

    @Deprecated("use ansi instead")
    public fun CharSequence.hidden(): String = !ansi.hidden

    @Deprecated("use ansi instead")
    public fun CharSequence.strikethrough(): String = !ansi.strikethrough
}
