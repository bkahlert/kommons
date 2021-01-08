package koodies.shell

import koodies.io.path.asString
import koodies.text.LineSeparators
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.mapCodePoints
import koodies.text.quoted
import java.nio.file.Path

class FileOperations(private val shellScript: ShellScript, private val path: String) {
    constructor(shellScript: ShellScript, path: Path) : this(shellScript, path.asString())

    private val optionalLineSeparator = LineSeparators
        .map { it.mapCodePoints { it.`N{U+XXXX}` }.joinToString("") }
        .joinToString(prefix = "(", separator = "|", postfix = ")?")

    /**
     * Removes the line matching [line], whereas [line] is only lines with a line separator
     * or the end of the file are matched.
     */
    fun removeLine(line: String, backupExtension: String = ".bak"): FileOperations {
        shellScript.line("perl -i$backupExtension -pe 's/$line(\\R|$)//' ${path.quoted}")
        return this
    }

    /**
     * Appends [content] to this [path], whereas an eventually existing line separator is replaced by [LineSeparators.LF].
     */
    fun appendLine(content: String): FileOperations {
        val separator = HereDocBuilder.randomLabel()
        shellScript.line("cat <<$separator >>${path.quoted}\n${content.withoutTrailingLineSeparator}\n$separator")
        return this
    }
}
