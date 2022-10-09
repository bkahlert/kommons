package com.bkahlert.kommons.exec

import org.codehaus.plexus.util.cli.Commandline
import org.intellij.lang.annotations.Language

/**
 * A shell script.
 *
 * @see <a href="https://pubs.opengroup.org/onlinepubs/7908799/xcu/chap2.html">Shell Command Language</a>
 */
public data class ShellScript(
    /** The content of this script. */
    @Language("Shell Script") public val content: String,
) : Executable {

    public constructor(
        /** The lines this script consists of. */
        @Language("Shell Script") vararg lines: CharSequence,
    ) : this(lines.joinToString("\n"))

    /** [CommandLine] that can be used to execute this script. */
    public fun toCommandLine(): CommandLine {
        val (shellCommand: String, shellArgs: List<String>) = Commandline().shell.run {
            shellCommand to shellArgsList
        }
        return CommandLine(
            shellCommand,
            *shellArgs.toTypedArray(),
            content,
        )
    }

    override val exec: Executor<Process.ExitState>
        get() = toCommandLine().exec

    /**
     * Returns a shell script line as it can be used in a shell,
     * e.g.
     * ```shell
     * #!/bin/sh
     * echo "Hello World!"
     * ```
     */
    public override fun toString(): String = content

    public companion object
}
