package koodies.terminal

import koodies.text.ANSI.Text.Companion.ansi

@Deprecated("use ansi instead")
public object AnsiFormats {
    @Deprecated("use ansi instead", ReplaceWith("this.ansi.bold", "koodies.text.ANSI.Text.Companion.ansi"))
    public fun CharSequence.bold(): String = !ansi.bold

    @Deprecated("use ansi instead", ReplaceWith("this.ansi.dim", "koodies.text.ANSI.Text.Companion.ansi"))
    public fun CharSequence.dim(): String = !ansi.dim

    @Deprecated("use ansi instead", ReplaceWith("this.ansi.italic", "koodies.text.ANSI.Text.Companion.ansi"))
    public fun CharSequence.italic(): String = !ansi.italic

    @Deprecated("use ansi instead", ReplaceWith("this.ansi.underline", "koodies.text.ANSI.Text.Companion.ansi"))
    public fun CharSequence.underline(): String = !ansi.underline

    @Deprecated("use ansi instead", ReplaceWith("this.ansi.inverse", "koodies.text.ANSI.Text.Companion.ansi"))
    public fun CharSequence.inverse(): String = !ansi.inverse

    @Deprecated("use ansi instead", ReplaceWith("this.ansi.hidden", "koodies.text.ANSI.Text.Companion.ansi"))
    public fun CharSequence.hidden(): String = !ansi.hidden

    @Deprecated("use ansi instead", ReplaceWith("this.ansi.strikethrough", "koodies.text.ANSI.Text.Companion.ansi"))
    public fun CharSequence.strikethrough(): String = !ansi.strikethrough
}
