package koodies.shell

import koodies.exec.CommandLine
import koodies.exec.Exec
import koodies.exec.ExecTerminationCallback
import koodies.exec.Executable
import koodies.io.path.executable
import koodies.io.path.pathString
import koodies.io.path.withDirectoriesCreated
import koodies.io.path.writeText
import koodies.shell.ShellScript.ScriptContext
import koodies.text.Banner.banner
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.lines
import koodies.text.LineSeparators.prefixLinesWith
import koodies.text.LineSeparators.trailingLineSeparatorRemoved
import koodies.text.joinLinesToString
import koodies.text.quoted
import koodies.text.withRandomSuffix
import koodies.text.wrapMultiline
import koodies.time.minutes
import koodies.time.seconds
import koodies.toBaseName
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
    public val hasShebang: Boolean = content?.isScript == true

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
        val script: String = toString()
        val transformedScript: String = transform(script)
        return with(Commandline().shell) {
            CommandLine(shellCommand, listOf(*shellArgsList.toTypedArray(), transformedScript), name = name)
        }
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
    public fun toFile(path: Path, echoName: Boolean = false, name: CharSequence? = this.name): Path = path.apply {
        if (notExists()) withDirectoriesCreated().createFile()
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
    public class ScriptContext(private val lines: MutableList<String>) {

        /**
         * The companion builder supports the build lambda argument to return a
         * string itself that is also added to the script—next to [lines].
         *
         * This methods serves as a helper, so build methods don't accidentally
         * return what they already added to the [lines] list (with the consequence
         * of adding the contents twice).
         */
        private fun lines(block: ScriptContext.() -> Unit): String =
            lines.run {
                block()
                ""
            }

        /** Adds the default [Shebang](https://en.wikipedia.org/wiki/Shebang_(Unix)) `#!/bin/sh` to this script. */
        public val shebang: String get() = shebang()

        /** Adds a [Shebang](https://en.wikipedia.org/wiki/Shebang_(Unix)) to this script. */
        public fun shebang(interpreter: Path, vararg arguments: String): String = shebang(interpreter.pathString, *arguments)

        /** Adds a [Shebang](https://en.wikipedia.org/wiki/Shebang_(Unix)) to this script. */
        public fun shebang(interpreter: String = "/bin/sh", vararg arguments: String): String = lines {
            lines.add("#!$interpreter" + arguments.joinToString("") { " $it" })
        }

        /**
         * Adds `cd [directory] || exit [errorCode]` to this script.
         */
        public fun changeDirectoryOrExit(directory: Path, errorCode: UByte = 1u): String = lines {
            lines.add("cd \"$directory\" || exit $errorCode")
        }

        /**
         * Adds `this` character sequence as a separate line to this script.
         */
        public operator fun @receiver:Language("Shell Script") CharSequence.not(): String = lines {
            lines.add(this@not.toString())
        }

        /**
         * Adds the given [words] concatenated with a whitespace to this script.
         */
        public fun line(@Language("Shell Script") vararg words: CharSequence): String = lines {
            lines.add(words.joinToString(" "))
        }

        /**
         * Adds the given [line] to this script.
         */
        public fun line(@Language("Shell Script") line: CharSequence): String = lines {
            lines.add(line.toString())
        }

        /**
         * Adds the given [lines] to this script.
         */
        public fun lines(@Language("Shell Script") lines: Iterable<CharSequence>): String = lines {
            lines.forEach { line(it) }
        }

        /**
         * Adds a [CommandLine] build with the given [command] and [arguments] to this script.
         */
        public fun command(command: CharSequence, arguments: Iterable<CharSequence>): String =
            command(CommandLine(command, arguments))

        /**
         * Adds a [CommandLine] build with the given [command] and [arguments] to this script.
         */
        public fun command(command: CharSequence, vararg arguments: CharSequence): String =
            command(CommandLine(command, *arguments))

        /**
         * Adds the given [CommandLine] to this script.
         */
        public fun command(command: CommandLine): String = lines {
            this.lines.add(command.shellCommand)
        }

        /**
         * Adds the [CommandLine] used by `this` [Executable] to this script.
         */
        public operator fun Executable<*>.not(): String =
            command(toCommandLine())

        /** Add an echo command with the given [arguments] */
        public fun echo(vararg arguments: Any): String = command("echo", arguments.map { it.toString() })

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
        ): String {
            require(interval >= 1.seconds) { "interval must be greater or equal to 1 second" }
            require(attemptTimeout >= 1.seconds) { "attempt timeout must be greater or equal to 1 second" }
            require(timeout >= 1.seconds) { "timeout must be greater or equal to 1 second" }
            return command("timeout", "${timeout.inWholeSeconds}s", "sh", "-c", """
                    while true; do
                        echo "$uri"
                        curl --silent --fail --max-time ${attemptTimeout.inWholeSeconds} '$uri' && break
                        sleep ${interval.inWholeSeconds}
                    done
                """.trimIndent())
        }

        /**
         * Initializes a [FileOperations] builder for the file specified by [path] and
         * the optional [init] applied to it.
         */
        public fun file(path: String, init: FileOperations.() -> Unit = {}): String = lines {
            FileOperations(this, path).apply(init)
        }

        /**
         * Initializes a [FileOperations] builder for the file specified by [path] and
         * the optional [init] applied to it.
         */
        public fun file(path: Path, init: FileOperations.() -> Unit = {}): String = lines {
            FileOperations(this, path.pathString).apply(init)
        }

        /**
         * Embeds the given [shellScript] in this script.
         */
        public fun embed(shellScript: ShellScript, echoName: Boolean = false): String = lines {
            val baseName = shellScript.name.toBaseName()
            val fileName = "$baseName.sh"
            val delimiter = "EMBEDDED-SCRIPT-$baseName".withRandomSuffix()
            val finalScript = shellScript.toString(echoName = echoName).trailingLineSeparatorRemoved
            lines.add(finalScript.wrapMultiline(
                """
                (
                cat <<'$delimiter'
                """,
                """
                $delimiter
                ) > "./$fileName"
                if [ -f "./$fileName" ]; then
                  chmod 755 "./$fileName"
                  "./$fileName"
                  wait
                  rm "./$fileName"
                else
                  echo "Error creating ""$fileName"
                fi
            """,
            ))
        }

        /**
         * Adds `exit [code]` to this script.
         */
        public fun exit(code: Int): String =
            command("exit", code.toString())

        /**
         * Adds the given [text] as a comment to this script.
         */
        public fun comment(text: String): String =
            lines { lines.add(text.prefixLinesWith("# ")) }

        /**
         * Adds `echo "[password]" | sudo -S [command]` to this script.
         */
        public fun sudo(password: String, command: String): String =
            lines { lines.add("echo ${password.quoted} | sudo -S $command") }

        /**
         * Adds a command that removes / deletes this script file.
         *
         * **This command should only be added as the last command** as
         * the behavior of a script being deleted while executed
         * is undetermined.
         */
        public fun deleteSelf(): String =
            lines { lines.add("rm -- \"\$0\"") }
    }

    public companion object : koodies.builder.Builder<ScriptInit, ShellScript> {

        /**
         * Builds a new [ShellScript] using the given [name] and [init].
         */
        public operator fun invoke(name: CharSequence?, init: ScriptInit): ShellScript {
            val lines = mutableListOf<String>()
            val trailingContent = ScriptContext(lines).init()
            return ShellScript(name, (lines + trailingContent.toString()).joinLinesToString())
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
