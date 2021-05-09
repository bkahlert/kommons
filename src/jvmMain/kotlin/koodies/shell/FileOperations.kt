package koodies.shell

import koodies.shell.ShellScript.ScriptContext
import koodies.text.LineSeparators
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.quoted

public class FileOperations(private val script: ScriptContext, private val path: String) {

    /**
     * Removes the all lines matching the specified [line] terminated by one of the [LineSeparators]
     * or the end of the file.
     */
    public fun removeLine(line: String, backupExtension: String = ".bak"): FileOperations {
        script.line("perl -i$backupExtension -pe 's/$line(?:\\R|$)//smg' ${path.quoted}")
        return this
    }

    /**
     * Appends [content] to this [path], whereas an eventually existing line separator is replaced by [LineSeparators.LF].
     */
    public fun appendLine(content: String): FileOperations {
        val separator = HereDocBuilder.randomLabel()
        script.line("cat <<$separator >>${path.quoted}\n${content.withoutTrailingLineSeparator}\n$separator")
        return this
    }
}
