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
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.quoted
import koodies.text.truncate
import koodies.text.withRandomSuffix
import koodies.text.wrapMultiline
import koodies.toBaseName
import org.codehaus.plexus.util.cli.Commandline
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.notExists

/**
 * A shell script.
 */
public class ShellScript(

    /**
     * Optional name of this script.
     */
    public val name: String? = null,

    /**
     * The content of this script.
     */
    content: CharSequence? = null,
) : Iterable<String>, Executable<Exec> {

    /**
     * The content of this script.
     */
    public val content: String = content?.toString()?.trimIndent() ?: ""

    /**
     * The [content] lines of this script.
     */
    private val lines = this.content.lines()

    /**
     * Returns an iterator over the lines this script is made of.
     */
    override fun iterator(): Iterator<String> {
        return lines.toList().iterator()
    }

    // TOOD -> toString
    public fun build(name: String? = this.name): String {
        var echoNameCommandAdded = false
        val echoNameCommand = bannerEchoingCommand(name)
        val script = lines.joinToString("") { line ->
            if (!echoNameCommandAdded && line.isShebang()) {
                echoNameCommandAdded = true
                line + LF + echoNameCommand
            } else {
                line + LF
            }
        }
        return if (echoNameCommandAdded) script else echoNameCommand + script
    }

    override fun toExec(
        redirectErrorStream: Boolean,
        environment: Map<String, String>,
        workingDirectory: Path?,
        execTerminationCallback: ExecTerminationCallback?,
    ): Exec = toCommandLine(environment, workingDirectory)
        .toExec(redirectErrorStream, environment, workingDirectory, execTerminationCallback)

    public override val summary: String
        get() = (name?.let { "${it.quoted}: " } ?: "").let {
            "Script($it${build().lines(ignoreTrailingSeparator = true).joinToString(";").truncate(150, MIDDLE, " … ")}})"
        }

    public fun buildTo(path: Path): Path = path.apply {
        if (path.notExists()) path.withDirectoriesCreated().createFile()
        writeText(build())
        executable = true
    }

    override fun toCommandLine(environment: Map<String, String>, workingDirectory: Path?): CommandLine {
        val script: String = build()
        val shell = Commandline().shell
        return CommandLine(shell.shellCommand, *shell.shellArgsList.toTypedArray(), script)
    }

    override fun toString(): String = summary
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShellScript

        if (name != other.name) return false
        if (content != other.content) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + content.hashCode()
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

        @Deprecated("replace with shebang", replaceWith = ReplaceWith("shebang", "koodies.shell.shebang"))
        public val `#!`: Shebang
            get() = shebang

        /**
         * Adds `#!` to this script.
         */
        public val shebang: Shebang get() = Shebang(lines)

        /**
         * Adds `cd [directory] || exit [errorCode]` to this script.
         */
        public fun changeDirectoryOrExit(directory: Path, errorCode: UByte = 1u): String = lines {
            lines.add("cd \"$directory\" || exit $errorCode")
        }

        /**
         * Adds `this` character sequence as a separate line to this script.
         */
        public operator fun CharSequence.not(): String = lines {
            lines.add(this@not.toString())
        }

        /**
         * Adds all elements of `this` collection to this script.
         */
        public operator fun Iterable<CharSequence>.not(): String = lines {
            lines.addAll(this@not.map { it.toString() })
        }

        /**
         * Adds the given [words] concatenated with a whitespace to this script.
         */
        public fun line(vararg words: String): String = lines {
            lines.add(words.joinToString(" "))
        }

        /**
         * Adds the given [line] to this script.
         */
        public fun line(line: String): String = lines {
            lines.add(line)
        }

        /**
         * Adds the given [lines] to this script.
         */
        public fun lines(lines: Iterable<String>): String = lines {
            this.lines.addAll(lines)
        }

        /**
         * Adds a [CommandLine] build with the given [command] and [arguments] to this script.
         */
        public fun command(command: String, vararg arguments: String): String =
            command(CommandLine(command, *arguments))

        /**
         * Adds the given [CommandLine] to this script.
         */
        public fun command(command: CommandLine): String = lines {
            this.lines.addAll(command.lines)
        }

        /**
         * Adds the [CommandLine] used by `this` [Executable] to this script.
         */
        public operator fun Executable<*>.not(): String =
            command(toCommandLine(emptyMap(), null))

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
         * Embeds the given [ShellScript] in this script.
         *
         * As soon as the execution comes to this point, the script
         * is unpacked to a temporary script file, executed and
         * when it has completed, removed.
         */
        public fun embed(shellScript: ShellScript): String = lines {
            val baseName = shellScript.name.toBaseName()
            val fileName = "$baseName.sh"
            val delimiter = "EMBEDDED-SCRIPT-$baseName".withRandomSuffix()
            val finalScript = shellScript.build().withoutTrailingLineSeparator
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
            lines { lines.add("exit $code") }

        /**
         * Adds the given [text] as a comment to this script.
         */
        public fun comment(text: String): String =
            lines { lines.add(text.prefixLinesWith(prefix = "# ")) }

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
        public operator fun invoke(name: String?, init: ScriptInit): ShellScript {
            val lines = mutableListOf<String>()
            val trailingContent = ScriptContext(lines).init()
            return ShellScript(name, (lines + trailingContent.toString()).joinToString(LF))
        }

        /**
         * Builds a new [ShellScript] using the given [init].
         */
        override fun invoke(init: ScriptInit): ShellScript = invoke(null, init)

        /**
         * Return—if [name] is not `null`—a command line that echos [name].
         *
         * If [name] is `null` an empty string is returned.
         */
        public fun bannerEchoingCommand(name: String?): String = name?.takeIf { it.isNotBlank() }?.let { "echo ${banner(name).quoted}$LF" } ?: ""

        /**
         * Whether this byte array starts with `0x23 0x21` (`!#`).
         */
        public val ByteArray.isScript: Boolean
            get() = size >= 2 && get(0) == 0x23.toByte() && get(1) == 0x21.toByte()

        /**
         * Whether this character sequence starts with bytes `0x23 0x21` (`!#`)
         */
        public val CharSequence.isScript: Boolean
            get() = length >= 2 && get(0) == '#' && get(1) == '!'

        /**
         * Whether this file exists and starts with bytes `0x23 0x21` (`!#`)
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
