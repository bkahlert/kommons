package com.bkahlert.kommons.shell

import com.bkahlert.kommons.LineSeparators
import com.bkahlert.kommons.LineSeparators.removeTrailingLineSeparator
import com.bkahlert.kommons.quoted
import com.bkahlert.kommons.shell.ShellScript.ScriptContext

public class FileOperations(private val script: ScriptContext, private val path: String) {

    /**
     * Removes the all lines matching the specified [line] terminated by one of the [LineSeparators]
     * or the end of the file.
     */
    public fun removeLine(line: String, backupExtension: String = ".bak"): FileOperations {
        script.command("perl", "-i$backupExtension", "-pe", "s/\\Q$line\\E(?:\\R|$)//smg", path)
        return this
    }

    /**
     * Appends [content] to this [path], whereas an eventually existing line separator is replaced by [LineSeparators.LF].
     */
    public fun appendLine(content: String): FileOperations {
        val delimiter = HereDoc.randomDelimiter()
        script.line("cat <<$delimiter >>${path.quoted}\n${content.removeTrailingLineSeparator()}\n$delimiter")
        return this
    }
}
