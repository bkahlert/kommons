package com.bkahlert.kommons.shell

import com.bkahlert.kommons.Kommons
import com.bkahlert.kommons.exec.CommandLine
import com.bkahlert.kommons.exec.Exec
import com.bkahlert.kommons.exec.ExecTerminationCallback
import com.bkahlert.kommons.exec.Executable
import com.bkahlert.kommons.io.path.createParentDirectories
import com.bkahlert.kommons.io.path.executable
import com.bkahlert.kommons.io.path.pathString
import com.bkahlert.kommons.io.path.tempFile
import com.bkahlert.kommons.io.path.writeText
import com.bkahlert.kommons.shell.ShellScript.ScriptContext
import com.bkahlert.kommons.shell.ShellScript.ScriptContext.Line
import com.bkahlert.kommons.text.Banner.banner
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.LineSeparators.lines
import com.bkahlert.kommons.text.LineSeparators.prefixLinesWith
import com.bkahlert.kommons.text.joinLinesToString
import com.bkahlert.kommons.text.quoted
import com.bkahlert.kommons.text.singleQuoted
import com.bkahlert.kommons.time.minutes
import com.bkahlert.kommons.time.seconds
import com.bkahlert.kommons.toBaseName
import org.codehaus.plexus.util.cli.Commandline
import org.intellij.lang.annotations.Language
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.notExists
import kotlin.time.Duration

/**
 * A shell script.
 *
 * @see <a href="https://pubs.opengroup.org/onlinepubs/7908799/xcu/chap2.html">Shell Command Language</a>
 */
public open class ShellScript(

    /**
     * Optional name of this script.
     */
    override val name: CharSequence?,

    /**
     * The content of this script.
     */
    @Language("Shell Script") content: CharSequence?,
) : Iterable<String>, Executable<Exec> {

    public constructor(

        /**
         * The content of this script.
         */
        @Language("Shell Script") content: CharSequence?,
    ) : this(null, content)

    public constructor() : this(null, null)

    /**
     * The content of this script.
     */
    public override val content: String = content?.toString()?.trimIndent() ?: ""

    /**
     * Whether this scripts starts with a [Shebang](https://en.wikipedia.org/wiki/Shebang_(Unix)).
     */
    public val hasShebang: Boolean get() = content.isScript

    /**
     * The [Shebang](https://en.wikipedia.org/wiki/Shebang_(Unix)) this script starts with, `null` otherwise.
     */
    public val shebang: CommandLine? get() = if (hasShebang) CommandLine.parse(lines.first().drop(2)) else null

    /**
     * The [content] lines of this script.
     */
    private val lines get() = content.lines()

    /**
     * Returns an iterator over the lines this script is made of.
     */
    override fun iterator(): Iterator<String> {
        return lines.toList().iterator()
    }

    override fun toCommandLine(
        environment: Map<String, String>,
        workingDirectory: Path?,
        transform: (String) -> String,
    ): CommandLine {
        val interpreter: CommandLine = shebang
            ?.run { CommandLine(command, *arguments.toTypedArray(), "-c") }
            ?: Commandline().shell.run { CommandLine(shellCommand, listOf(*shellArgsList.toTypedArray())) }

        val scriptWithoutShebang: String = if (hasShebang) ShellScript(name, lines.drop(1).joinLinesToString()).toString() else toString()
        val transformedScriptWithoutShebang: String = transform(scriptWithoutShebang)
        return CommandLine(interpreter.command, *interpreter.arguments.toTypedArray(), transformedScriptWithoutShebang, name = name)
    }

    override fun toExec(
        redirectErrorStream: Boolean,
        environment: Map<String, String>,
        workingDirectory: Path?,
        execTerminationCallback: ExecTerminationCallback?,
    ): Exec = toCommandLine(environment, workingDirectory)
        .toExec(redirectErrorStream, environment, workingDirectory, execTerminationCallback)

    /**
     * Returns a shell script line as it can be used in a shell,
     * e.g.
     * ```shell
     * #!/bin/sh
     * echo "Hello World!"
     * ```
     *
     * If a [name] is set, it will be
     * printed as the first command.
     */
    public override fun toString(): String = toString(false, name)

    /**
     * Returns this shell script as it can be used in a shell,
     * e.g.
     * ```shell
     * #!/bin/sh
     * echo "Hello World!"
     * ```
     *
     * If [echoName] is used, the given [name] is will be
     * printed as the first command.
     */
    public fun toString(echoName: Boolean, name: CharSequence? = this.name): String =
        if (echoName && name != null) {
            if (hasShebang) {
                StringBuilder().apply {
                    appendLine(lines.first())
                    appendLine("echo '${banner(name)}'")
                    lines.drop(1).forEach { appendLine(it) }
                }.toString()
            } else {
                StringBuilder().apply {
                    appendLine("echo '${banner(name)}'")
                    lines.forEach { appendLine(it) }
                }.toString()
            }
        } else {
            content + LF
        }

    /**
     * Saves this shell script at the specified [path] and sets its [executable] flag.
     * @see toString
     */
    public fun toFile(
        path: Path = Kommons.FilesTemp.tempFile(this.name.toBaseName(), ".sh"),
        echoName: Boolean = false,
        name: CharSequence? = this.name,
    ): Path = path.apply {
        if (notExists()) createParentDirectories().createFile()
        writeText(this@ShellScript.toString(echoName, name))
        executable = true
    }

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
                line += " > ${file.singleQuoted}"
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
         * Adds `this` character sequence as a separate line to this script.
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
        public fun command(command: CharSequence, arguments: Iterable<CharSequence>): Line = command(CommandLine(command, arguments))

        /**
         * Adds a [CommandLine] build with the given [command] and [arguments] to this script.
         */
        public fun command(command: CharSequence, vararg arguments: CharSequence): Line = command(CommandLine(command, *arguments))

        /**
         * Adds the given [CommandLine] to this script.
         */
        public fun command(command: CommandLine): Line = Line(command.shellCommand)

        /**
         * Adds the [CommandLine] used by `this` [Executable] to this script.
         */
        public operator fun Executable<*>.not(): Line = command(toCommandLine())

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
            return command("timeout", "${timeout.inWholeSeconds}s", "sh", "-c", StringBuilder().apply {
                appendLine("while true; do")
                if (verbose) appendLine("    echo 'Polling $uri...'")
                append("    1>/dev/null curl --silent --fail --max-time ${attemptTimeout.inWholeSeconds} '$uri'")
                if (verbose) append("&& echo 'Polled $uri successfully.'")
                appendLine(" && break")
                appendLine("    sleep ${interval.inWholeSeconds}")
                appendLine("done")
            }.toString())
        }

        /**
         * Initializes a [FileOperations] builder for the file specified by [path] and
         * the optional [init] applied to it.
         */
        public fun file(path: String, init: FileOperations.() -> Unit = {}): String {
            FileOperations(this@ScriptContext, path).apply(init)
            return ""
        }

        /**
         * Initializes a [FileOperations] builder for the file specified by [path] and
         * the optional [init] applied to it.
         */
        public fun file(path: Path, init: FileOperations.() -> Unit = {}): String {
            FileOperations(this@ScriptContext, path.pathString).apply(init)
            return ""
        }

        /**
         * Embeds the given [shellScript] in this script.
         */
        public fun embed(shellScript: ShellScript, echoName: Boolean = false): Line {
            val finalShellScript = if (echoName) ShellScript(null, shellScript.toString(true)) else shellScript
            return Line(finalShellScript.toCommandLine().shellCommand)
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
        public fun comment(text: String): Line = Line(text.prefixLinesWith("# "))

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

    public companion object : com.bkahlert.kommons.builder.Builder<ScriptInit, ShellScript> {

        /**
         * Builds a new [ShellScript] using the given [name] and [init].
         */
        public operator fun invoke(name: CharSequence?, init: ScriptInit): ShellScript {
            val lines = mutableListOf<CharSequence>()
            val trailingContent: String = ScriptContext(lines).init().takeUnless { it is Line }?.toString() ?: ""
            return ShellScript(name, (lines.filterNot { it is Line && it.isEmpty() } + trailingContent).joinLinesToString())
        }

        /**
         * Builds a new [ShellScript] using the given [init].
         */
        override fun invoke(init: ScriptInit): ShellScript = invoke(null, init)

        /**
         * Whether this byte array starts with `0x23 0x21` (`#!`).
         */
        public val ByteArray.isScript: Boolean
            get() = size >= 2 && get(0) == 0x23.toByte() && get(1) == 0x21.toByte()

        /**
         * Whether this character sequence starts with bytes `0x23 0x21` (`#!`)
         */
        public val CharSequence.isScript: Boolean
            get() = length >= 2 && get(0) == '#' && get(1) == '!'

        /**
         * Whether this file exists and starts with bytes `0x23 0x21` (`#!`)
         */
        public val Path.isScript: Boolean
            get() = takeIf { it.exists() }?.run {
                inputStream().run {
                    read() == 0x23 && read() == 0x21
                }
            } ?: false
    }
}

/**
 * Type of the argument supported by [ShellScript] builder.
 *
 * @see ShellScript.Companion
 */
public typealias ScriptInit = ScriptContext.() -> CharSequence
