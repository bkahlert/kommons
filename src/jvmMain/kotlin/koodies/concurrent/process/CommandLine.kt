package koodies.concurrent.process

import com.github.ajalt.mordant.AnsiColorCode
import koodies.concurrent.scriptPath
import koodies.io.path.Locations
import koodies.io.path.asPath
import koodies.logging.RenderingLogger
import koodies.logging.asStatus
import koodies.logging.logging
import koodies.regex.get
import koodies.shell.ShellScript
import koodies.shell.shebang
import koodies.terminal.ANSI
import koodies.text.LineSeparators
import koodies.text.unquoted
import org.codehaus.plexus.util.StringUtils
import org.codehaus.plexus.util.cli.CommandLineUtils
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.MULTILINE
import org.codehaus.plexus.util.cli.Commandline as PlexusCommandLine

/**
 * A command as it can be run in a shell.
 */
open class CommandLine(
    /**
     * Redirects like `2>&1` to be used when running this command line.
     */
    val redirects: List<String>,
    /**
     * The environment to be exposed to the [ManagedProcess] that runs this
     * command line.
     */
    val environment: Map<String, String>,
    /**
     * The working directory of the [ManagedProcess] that runs this
     * command line.
     */
    workingDirectory: Path,
    /**
     * The command to be executed.
     */
    val command: String,
    /**
     * The arguments to be passed to [command].
     */
    val arguments: List<String>,
) {

    constructor(
        redirects: List<String>,
        environment: Map<String, String>,
        workingDirectory: Path,
        command: String,
        vararg arguments: String,
    ) : this(redirects, environment, workingDirectory, command, arguments.toList())

    constructor(
        environment: Map<String, String>,
        workingDirectory: Path,
        command: String,
        vararg arguments: String,
    ) : this(emptyList(), environment, workingDirectory, command, arguments.toList())

    constructor(
        workingDirectory: Path,
        command: String,
        vararg arguments: String,
    ) : this(emptyList(), emptyMap(), workingDirectory, command, arguments.toList())

    constructor(
        command: String,
        vararg arguments: String,
    ) : this(emptyList(), emptyMap(), Locations.WorkingDirectory, command, arguments.toList())
    

    /**
     * The working directory of the [ManagedProcess] that runs this
     * command line.
     */
    val workingDirectory: Path = workingDirectory.toAbsolutePath()

    private val formattedRedirects =
        redirects.takeIf { it.isNotEmpty() }?.joinToString(separator = " ", postfix = " ") ?: ""

    /**
     * The array consisting of the command and its arguments that make up this command,
     * e.g. `[echo, Hello World!]`.
     */
    val commandLineParts: Array<String> by lazy { arrayOf(command, *arguments.toTypedArray()) }

    /**
     * The command line as it can be used on the shell,
     * e.g. `echo "Hello World!"`.
     */
    val commandLine: String by lazy {
        formattedRedirects + CommandLineUtils.toString(commandLineParts).fixHereDoc()
    }

    /**
     * The command line as it can be used on the shell, but in contrast to [commandLine],
     * this version eventually spans multiple lines using escaped line separators to be
     * easier readable, e.g.
     * ```shell
     * command \
     * --argument \
     * "argument" \
     * -org
     * ```
     */
    val multiLineCommandLine: String by lazy {
        commandLineParts.joinToString(separator = " \\${LineSeparators.LF}") {
            StringUtils.quoteAndEscape(
                it.trim(),
                '\"'
            )
        }.fixHereDoc()
    }

    /**
     * A human-readable representation of this command line.
     */
    val summary: String
        get() = arguments
            .map { line ->
                line.split("\\b".toRegex()).filter { part -> part.trim().run { length > 1 && !startsWith("-") } }
            }
            .filter { it.isNotEmpty() }
            .map { words ->
                when (words.size) {
                    0 -> "❓"
                    1 -> words.first()
                    2 -> words.joinToString("…")
                    else -> words.first() + "…" + words.last()
                }
            }.asStatus()

    /**
     * Contains all accessible files contained in this command line.
     */
    val includedFiles: List<Path>
        get() = commandLineParts.map { it.unquoted.asPath() }
            .filter { it != it.root }
            .filter { it.exists() }

    /**
     * Contains a formatted list of files contained in this command line.
     */
    val formattedIncludesFiles: String get() = includedFiles.joinToString("\n") { "📄 ${it.toUri()}" }

    override fun toString(): String = multiLineCommandLine

    /**
     * The [multiLineCommandLine] represented as a list of single-line strings.
     */
    val lines: List<String> get() = multiLineCommandLine.lines()

    companion object {

        /**
         * Builds a [CommandLine] using the specified mandatory [command]
         * further options build using [init].
         */
        fun build(command: String, init: CommandLineBuilder.() -> Unit = {}): CommandLine =
            CommandLineBuilder.build(command, init)

        /**
         * Parses a [commandLine] string and returns an instance of [CommandLine]
         * that would generate the same string again.
         */
        fun parse(commandLine: String, workingDirectory: Path): CommandLine {
            val plexusCommandLine = PlexusCommandLine(commandLine)
            val rawCommandline = plexusCommandLine.rawCommandline
            return rawCommandline.takeIf { it.isNotEmpty() }
                ?.let { CommandLine(emptyList(), emptyMap(), workingDirectory, it.first(), it.drop(1)) }
                ?: throw IllegalArgumentException("$commandLine is no valid command line.")
        }

        /**
         * A [Regex] that matches the start of a [here document](https://en.wikipedia.org/wiki/Here_document).
         */
        val hereDocStartRegex: Regex = Regex("<<(?<name>\\w[-\\w]*)\\s*")

        /**
         * Given `this` command line string containing [here documents](https://en.wikipedia.org/wiki/Here_document),
         * this function will returns a command line string where the here documents will no longer
         * be wrapped by double or single quotes.
         */
        fun String.fixHereDoc(): String {
            var fixed = this
            val hereDocNames = hereDocStartRegex.findAll(fixed).map { it["name"] }
            hereDocNames.forEach { name ->
                fixed = Regex("[\"']+<<$name.*^$name[\"']+", setOf(MULTILINE, DOT_MATCHES_ALL)).replace(fixed) {
                    var hereDoc = it.value
                    while (true) {
                        val unquoted = hereDoc.unquoted
                        if (unquoted != hereDoc) hereDoc = unquoted
                        else break
                    }
                    hereDoc
                }
            }

            return fixed
        }
    }

    /**
     * Builds a proper script that runs this command line.
     *
     * @return temporary `.sh` file
     */
    fun toShellScript(): Path =
        ShellScript().apply {
            shebang
            changeDirectoryOrExit(directory = workingDirectory)
            command(commandLine)
        }.buildTo(workingDirectory.scriptPath())

    /**
     * Creates an actual [ManagedProcess] that executes this command line.
     */
    open fun toManagedProcess(expectedExitValue: Int? = 0, processTerminationCallback: (() -> Unit)? = null): ManagedProcess =
        ManagedProcess.from(this, expectedExitValue, processTerminationCallback)

    /**
     * Prepares a new [ManagedProcess] that runs this command line as soon as it's triggered.
     *
     * @see execute
     */
    open fun prepare(expectedExitValue: Int = 0): ManagedProcess = toManagedProcess(expectedExitValue, null)

    /**
     * Starts a new [ManagedProcess] that runs this command line.
     */
    open fun execute(expectedExitValue: Int = 0): ManagedProcess = prepare(expectedExitValue).also { it.start() }

    /**
     * Starts a new [ManagedProcess] that runs this command line
     * and has it fully processed using `this` [RenderingLogger].
     */
    open fun RenderingLogger.executeLogging(
        caption: String,
        bordered: Boolean = true,
        ansiCode: AnsiColorCode = ANSI.termColors.brightBlue,
        nonBlockingReader: Boolean = false,
        expectedExitValue: Int = 0,
    ): Int = logging(caption = caption, bordered = bordered, ansiCode = ansiCode) {
        execute(expectedExitValue).process(
            nonBlockingReader = nonBlockingReader,
            processInputStream = InputStream.nullInputStream(),
            processor = Processors.loggingProcessor(this)
        )
    }.waitForTermination()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CommandLine

        if (!commandLineParts.contentEquals(other.commandLineParts)) return false

        return true
    }

    override fun hashCode(): Int = commandLineParts.contentHashCode()
}
