package com.bkahlert.kommons.exec

import com.bkahlert.kommons.exec.ShellScript.ScriptContext.Line
import com.bkahlert.kommons.quoted
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.LineSeparators.lines
import com.bkahlert.kommons.text.LineSeparators.mapLines
import org.codehaus.plexus.util.cli.Commandline
import org.intellij.lang.annotations.Language
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.pathString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * A shell script.
 *
 * @see <a href="https://pubs.opengroup.org/onlinepubs/7908799/xcu/chap2.html">Shell Command Language</a>
 */
public data class ShellScript(

    /** Optional name of this script. */
    public val name: CharSequence?,

    /** Code lines this script consists of. */
    public val lines: List<String>,
) : Executable {

    public constructor(
        /** Optional name of this script. */
        name: CharSequence?,
        /** Content this script consists of. */
        @Language("Shell Script") content: CharSequence? = null,
    ) : this(name, content?.lines() ?: emptyList())

    public constructor(
        /** Content this script consists of. */
        @Language("Shell Script") content: CharSequence? = null,
    ) : this(null, content)

    /**
     * The content of this script.
     */
    public val content: String = lines.joinToString(LF).trimIndent()

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

    override val exec: SyncExecutor
        get() = toCommandLine().exec

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShellScript

        if (name != other.name) return false
        if (this.content != other.content) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + this.content.hashCode()
        return result
    }

    /**
     * Returns a shell script line as it can be used in a shell,
     * e.g.
     * ```shell
     * #!/bin/sh
     * echo "Hello World!"
     * ```
     */
    public override fun toString(): String = content

    /**
     * Context to ease building shell scripts.
     */
    public class ScriptContext(private val lines: MutableList<CharSequence>) {

        public inner class Line(
            @Language("Shell Script") private var line: String,
        ) : CharSequence {

            init {
                lines.add(this)
            }

            public infix fun or(other: CharSequence): Line {
                line += " || $other"
                if (other is Line) other.line = ""
                return this
            }

            public infix fun and(other: CharSequence): Line {
                line += " && $other"
                if (other is Line) other.line = ""
                return this
            }

            public infix fun redirectTo(file: CharSequence): Line {
                line += " > '$file'"
                return this
            }

            public infix fun redirectTo(file: Path): Line =
                redirectTo(file.pathString)

            override val length: Int get() = line.length
            override fun get(index: Int): Char = line[index]
            override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = line.subSequence(startIndex, endIndex)
            override fun toString(): String = line
        }

        /** Adds the default [Shebang](https://en.wikipedia.org/wiki/Shebang_(Unix)) `#!/bin/sh` to this script. */
        public val shebang: Line get() = shebang()

        /** Adds a [Shebang](https://en.wikipedia.org/wiki/Shebang_(Unix)) to this script. */
        public fun shebang(interpreter: Path, vararg arguments: String): Line = shebang(interpreter.pathString, *arguments)

        /** Adds a [Shebang](https://en.wikipedia.org/wiki/Shebang_(Unix)) to this script. */
        public fun shebang(interpreter: String = "/bin/sh", vararg arguments: String): Line =
            Line("#!$interpreter" + arguments.joinToString("") { " $it" })

        /**
         * Adds `cd [directory]` to this script.
         */
        public fun changeDirectory(directory: Path): Line = command("cd", directory.pathString)

        /**
         * Adds `cd [directory] || exit [errorCode]` to this script.
         */
        public fun changeDirectoryOrExit(directory: Path, errorCode: UByte = 1u): Line = changeDirectory(directory) or exit(errorCode)

        /**
         * Adds this character sequence as a separate line to this script.
         */
        public operator fun @receiver:Language("Shell Script") CharSequence.not(): Line = Line(toString())

        /**
         * Adds the given [words] concatenated with a whitespace to this script.
         */
        public fun line(@Language("Shell Script") vararg words: CharSequence): Line = Line(words.joinToString(" "))

        /**
         * Adds the given [line] to this script.
         */
        public fun line(@Language("Shell Script") line: CharSequence): Line = Line(line.toString())

        /**
         * Adds the given [lines] to this script.
         */
        public fun lines(@Language("Shell Script") lines: Iterable<CharSequence>): String {
            lines.forEach { line(it) }
            return ""
        }

        /**
         * Adds a [CommandLine] build with the given [command] and [arguments] to this script.
         */
        public fun command(command: CharSequence, arguments: Iterable<CharSequence>): Line = command(
            CommandLine(
                command.toString(),
                arguments.map { it.toString() }
            )
        )

        /**
         * Adds a [CommandLine] build with the given [command] and [arguments] to this script.
         */
        public fun command(command: CharSequence, vararg arguments: CharSequence): Line = command(
            CommandLine(
                command,
                *arguments
            )
        )

        /**
         * Adds the given [CommandLine] to this script.
         */
        public fun command(command: CommandLine): Line = Line(command.toString())

        /** Add an echo command with the given [arguments] */
        public fun echo(vararg arguments: Any): Line = command("echo", arguments.map { it.toString() })

        /**
         * Adds a while loop that attempts to connect to the given [uri] until
         * it succeeds or the [timeout] is reached (results in exit code `124`).
         *
         * @param uri The [URI] to connect to.
         * @param interval The interval of connection attempts.
         * @param attemptTimeout The maximum duration spent on a single connection attempt.
         * @param timeout The maximum duration connection attempts are made before exiting.
         */
        public fun poll(
            uri: URI,
            interval: Duration = 2.seconds,
            attemptTimeout: Duration = 5.seconds,
            timeout: Duration = 5.minutes,
            verbose: Boolean = false,
        ): Line {
            require(interval >= 1.seconds) { "interval must be greater or equal to 1 second" }
            require(attemptTimeout >= 1.seconds) { "attempt timeout must be greater or equal to 1 second" }
            require(timeout >= 1.seconds) { "timeout must be greater or equal to 1 second" }
            return command(
                "perl", "-e", "alarm ${timeout.inWholeSeconds}; exec @ARGV", "sh", "-c",
                buildString {
                    appendLine("while true; do")
                    if (verbose) appendLine("    echo 'Polling $uri...'")
                    append("    1>/dev/null curl --silent --fail --location --max-time ${attemptTimeout.inWholeSeconds} '$uri'")
                    if (verbose) append("&& echo 'Polled $uri successfully.'")
                    appendLine(" && break")
                    appendLine("    sleep ${interval.inWholeSeconds}")
                    appendLine("done")
                },
            )
        }

        /**
         * Embeds the given [shellScript] in this script.
         */
        public fun embed(shellScript: ShellScript): Line {
            return Line(shellScript.toCommandLine().toString())
        }

        /**
         * Adds `shutdown -h now` to this script.
         */
        public val shutdown: Line
            get() = shutdown()

        /**
         * Adds `shutdown` to this script.
         */
        public fun shutdown(halt: Boolean = true, time: String = "now", message: String? = null): Line =
            command("shutdown", *listOfNotNull(if (halt) "-h" else null, time, message).toTypedArray())

        /**
         * Adds `exit [code]` to this script.
         */
        public fun exit(code: UByte = 0u): Line = command("exit", code.toString())

        /**
         * Adds the given [text] as a comment to this script.
         */
        public fun comment(text: String): Line = Line(text.mapLines { "# $it" })

        /**
         * Adds `echo "[password]" | sudo -S [command]` to this script.
         */
        public fun sudo(password: String, command: String): Line = Line("echo ${password.quoted} | sudo -S $command")

        /**
         * Adds a command that removes / deletes this script file.
         *
         * **This command should only be added as the last command** as
         * the behavior of a script being deleted while executed
         * is undetermined.
         */
        public fun deleteSelf(): Line = Line("rm -- \"\$0\"")
    }

    public companion object {

        /**
         * Builds a new [ShellScript] using the given [name] and [init].
         */
        public operator fun invoke(
            name: CharSequence? = null,
            init: ScriptContext.() -> CharSequence,
        ): ShellScript {
            val lines = mutableListOf<CharSequence>()
            val trailingContent = ScriptContext(lines).init().takeUnless { it is Line }
            return ShellScript(
                name,
                buildList {
                    lines.filterNot { it is Line && it.isEmpty() }.forEach {
                        add(it.toString())
                    }
                    trailingContent?.also { add(it.toString()) }
                }
            )
        }
    }
}
