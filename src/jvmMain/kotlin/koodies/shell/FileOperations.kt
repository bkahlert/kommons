package koodies.shell

import koodies.io.path.asString
import koodies.text.LineSeparators
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.mapCodePoints
import koodies.text.quoted
import java.nio.file.Path

public class FileOperations(private val shellScript: ShellScript, private val path: String) {
    public constructor(shellScript: ShellScript, path: Path) : this(shellScript, path.asString())

    private val optionalLineSeparator = LineSeparators
        .map { lineSeparator -> lineSeparator.mapCodePoints { codePoint -> "N{U+" + codePoint.hexCode + "}" }.joinToString("") }
        .joinToString(prefix = "(", separator = "|", postfix = ")?")

    /**
     * Removes the all lines matching the specified [line] terminated by one of the [LineSeparators]
     * or the end of the file.
     */
    public fun removeLine(line: String, backupExtension: String = ".bak"): FileOperations {
        shellScript.line("perl -i$backupExtension -pe 's/$line(?:\\R|$)//smg' ${path.quoted}")
        return this
    }

    /**
     * Appends [content] to this [path], whereas an eventually existing line separator is replaced by [LineSeparators.LF].
     */
    public fun appendLine(content: String): FileOperations {
        val separator = HereDocBuilder.randomLabel()
        shellScript.line("cat <<$separator >>${path.quoted}\n${content.withoutTrailingLineSeparator}\n$separator")
        return this
    }
}
