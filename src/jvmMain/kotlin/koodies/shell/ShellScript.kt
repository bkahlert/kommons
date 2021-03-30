package koodies.shell

import koodies.concurrent.process.CommandLine
import koodies.concurrent.scriptPath
import koodies.io.path.Locations
import koodies.io.path.asString
import koodies.io.path.executable
import koodies.io.path.withDirectoriesCreated
import koodies.io.path.writeText
import koodies.terminal.Banner.banner
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.lines
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.prefixLinesWith
import koodies.text.quoted
import koodies.text.truncate
import koodies.text.withRandomSuffix
import koodies.text.wrapMultiline
import koodies.toBaseName
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.notExists

@DslMarker
public annotation class ShellScriptMarker

@ShellScriptMarker
public class ShellScript(public val name: String? = null, content: String? = null) : Iterable<String>, ShellExecutable {

    private val lines: MutableList<String> = mutableListOf()

    init {
        if (content != null) lines.addAll(content.trimIndent().lines())
    }

    /**
     * Returns an iterator over the lines this script is made of.
     */
    override fun iterator(): Iterator<String> {
        return lines.toList().iterator()
    }

    public val `#!`: Shebang get() = Shebang(lines)

    public val shebang: Shebang get() = Shebang(lines)

    public fun changeDirectoryOrExit(directory: Path, @Suppress("UNUSED_PARAMETER") errorCode: Int = -1) {
        lines.add("cd \"$directory\" || exit -1")
    }

    public operator fun String.not() {
        lines.add(this)
    }

    /**
     * Adds the given [words] concatenated with a whitespace to this script.
     */
    public fun line(vararg words: String) {
        lines.add(words.joinToString(" "))
    }

    /**
     * Adds the given [line] to this script.
     */
    public fun line(line: String) {
        lines.add(line)
    }

    /**
     * Adds the given [lines] to this script.
     */
    public fun lines(lines: Iterable<String>) {
        this.lines.addAll(lines)
    }

    /**
     * Builds a [command] call using the [arguments].
     */
    public fun command(command: String, vararg arguments: String) {
        lines.add(listOf(command, *arguments).joinToString(" "))
    }

    /**
     * Builds a [command] call.
     */
    public fun command(command: CommandLine) {
        lines.addAll(command.lines)
    }

    /**
     * Initializes a [FileOperations] builder for the file specified by [path] and
     * the optional [init] applied to it.
     */
    public fun file(path: String, init: FileOperations.() -> Unit = {}): FileOperations = FileOperations(this, path).apply(init)

    /**
     * Initializes a [FileOperations] builder for the file specified by [path] and
     * the optional [init] applied to it.
     */
    public fun file(path: Path, init: FileOperations.() -> Unit = {}): FileOperations = FileOperations(this, path).apply(init)

    public fun embed(shellScript: ShellScript) {
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
                ) > "$fileName"
                if [ -f "$fileName" ]; then
                  chmod 755 "$fileName"
                  "./$fileName"
                  wait
                  rm "$fileName"
                else
                  echo "Error creating \"$fileName\""
                fi
            """,
        ))
    }

    public fun exit(code: Int) {
        lines.add("exit $code")
    }

    public fun comment(text: String) {
        lines += text.prefixLinesWith(prefix = "# ")
    }

    public fun sudo(password: String, command: String) {
        lines += "echo ${password.quoted} | sudo -S $command"
    }

    /**
     * Adds a command that removes / deletes the running script file.
     *
     * It's highly recommended to only "self-destruct" as the last
     * command.
     */
    public fun deleteSelf() {
        lines += "rm -- \"\$0\""
    }

    public fun sanitize(workingDirectory: Path? = null): ShellScript {
        var linesKept = lines.dropWhile { it.isShebang() || it.isBlank() }
        if (workingDirectory != null && linesKept.firstOrNull()?.startsWith("cd ") == true) linesKept = linesKept.drop(1)
        return ShellScript(name = name).apply {
            shebang
            workingDirectory?.let { changeDirectoryOrExit(it) }
            linesKept.filter { !it.isShebang() }.forEach { lines += it }
        }
    }

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

    public fun buildTo(path: Path): Path = path.apply {
        if (path.notExists()) path.withDirectoriesCreated().createFile()
        writeText(build())
        executable = true
    }

    public override val summary: String
        get() = "Script(name=$name;content=${build().lines(ignoreTrailingSeparator = true).joinToString(";").truncate(150, MIDDLE, " … ")}})"


    override fun toCommandLine(): CommandLine = toCommandLine(emptyMap())

    /**
     * Creates a [CommandLine] from `this` [ShellScript] by saving to [Locations.Temp].
     */
    public fun toCommandLine(environment: Map<String, String>): CommandLine =
        toCommandLine(Locations.Temp, environment)

    /**
     * Creates a [CommandLine] from `this` [ShellScript] by saving to `this` [Path].
     */
    public fun toCommandLine(path: Path, environment: Map<String, String> = emptyMap()): CommandLine {
        val scriptFile = sanitize(path).buildTo(path.scriptPath())
        return CommandLine(environment, path, scriptFile.asString())
    }

    override fun toString(): String = summary
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShellScript

        if (name != other.name) return false
        if (lines != other.lines) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + lines.hashCode()
        return result
    }

    public companion object {
        /**
         * Builds and returns an actual instance.
         */
        public fun (ShellScript.() -> Unit).build(name: String? = null): ShellScript =
            ShellScript(name = name).apply(this)

        public operator fun invoke(name: String? = null, block: ShellScript.() -> Unit): ShellScript {
            val build = block.build()
            val content = build.build()
            return ShellScript(name, content)
        }

        /**
         * Return—if [name] is not `null`—a command line that echos [name].
         *
         * If [name] is `null` an empty string is returned.
         */
        public fun bannerEchoingCommand(name: String?): String = name?.takeIf { it.isNotBlank() }?.let { "echo ${banner(name).quoted}$LF" } ?: ""
    }
}
