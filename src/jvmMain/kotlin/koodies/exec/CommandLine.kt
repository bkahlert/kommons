package koodies.exec

import koodies.builder.BuilderTemplate
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.ListBuildingContext
import koodies.builder.context.MapBuildingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.exec.CommandLine.Companion.CommandLineContext
import koodies.io.path.Locations
import koodies.io.path.asPath
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.lines
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.truncate
import koodies.text.unquoted
import org.codehaus.plexus.util.StringUtils.quoteAndEscape
import java.nio.file.Path
import kotlin.io.path.exists
import org.codehaus.plexus.util.cli.Commandline as PlexusCommandLine

@DslMarker
public annotation class CommandLineDsl

/**
 * A command as it can be run in a shell.
 */
public open class CommandLine(
    /**
     * Redirects like `2>&1` to be used when running this command line.
     */
    public val redirects: List<String>,
    /**
     * The environment to be exposed to the [Exec] that runs this
     * command line.
     */
    public val environment: Map<String, String>,
    /**
     * The working directory of the [Exec] that runs this
     * command line.
     */
    workingDirectory: Path,
    /**
     * The command to be executed.
     */
    public val command: String,
    /**
     * The arguments to be passed to [command].
     */
    public val arguments: List<String>,
) : Executable {

    public constructor(
        redirects: List<String>,
        environment: Map<String, String>,
        workingDirectory: Path,
        command: String,
        vararg arguments: String,
    ) : this(redirects, environment, workingDirectory, command, arguments.toList())

    public constructor(
        environment: Map<String, String>,
        workingDirectory: Path,
        command: String,
        vararg arguments: String,
    ) : this(emptyList(), environment, workingDirectory, command, arguments.toList())

    public constructor(
        workingDirectory: Path,
        command: String,
        vararg arguments: String,
    ) : this(emptyList(), emptyMap(), workingDirectory, command, arguments.toList())

    public constructor(
        command: String,
        vararg arguments: String,
    ) : this(emptyList(), emptyMap(), Locations.WorkingDirectory, command, arguments.toList())


    /**
     * The working directory of the [Exec] that runs this
     * command line.
     */
    public val workingDirectory: Path = workingDirectory.toAbsolutePath()

    private val formattedRedirects =
        redirects.takeIf { it.isNotEmpty() }?.joinToString(separator = " ", postfix = " ") ?: ""

    /**
     * The array consisting of the command and its arguments that make up this command,
     * e.g. `[echo, Hello World!]`.
     */
    public val commandLineParts: Array<String> by lazy { arrayOf(command, *arguments.toTypedArray()) }

    /**
     * The command line as it can be used on the shell,
     * e.g. `echo "Hello World!"`.
     */
    public val commandLine: String by lazy {
        formattedRedirects + asShellCommand(commandLineParts)
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
    public val multiLineCommandLine: String by lazy {
        commandLineParts.joinToString(separator = " \\$LF") {
            quoteAndEscape(
                it.trim(),
                '\"'
            )
        }
    }

    /**
     * A human-readable representation of this command line.
     */
    public override val summary: String
        get() = multiLineCommandLine.lines().joinToString("; ").replace("\\; ", "").truncate(60, strategy = MIDDLE, marker = " … ")

    override fun toCommandLine(): CommandLine = this

    /**
     * Contains all accessible files contained in this command line.
     */
    public open val includedFiles: List<Path>
        get() = commandLineParts.map { it.unquoted.asPath() }
            .filter { it != it.root }
            .filter { it.exists() }


    override fun toString(): String = multiLineCommandLine

    /**
     * The [multiLineCommandLine] represented as a list of single-line strings.
     */
    public val lines: List<String> get() = multiLineCommandLine.lines()


    public companion object : BuilderTemplate<CommandLineContext, CommandLine>() {

        /**
         * Context to build a [CommandLine].
         */
        @CommandLineDsl
        public class CommandLineContext(override val captures: CapturesMap) : CapturingContext() {

            /**
             * Specifies the redirects like `2>&1` to be used when running this built command line.
             */
            public val redirects: SkippableCapturingBuilderInterface<ListBuildingContext<String>.() -> Unit, List<String>> by listBuilder()

            /**
             * Specifies the environment to be exposed to the [Exec] that runs this built
             * command line.
             */
            public val environment: SkippableCapturingBuilderInterface<MapBuildingContext<String, String>.() -> Unit, Map<String, String>> by mapBuilder()

            /**
             * Specifies the working directory of the [Exec] that runs this built
             * command line.
             */
            public val workingDirectory: SkippableCapturingBuilderInterface<() -> Path, Path?> by builder()

            /**
             * The command to be executed.
             */
            public val command: SkippableCapturingBuilderInterface<() -> String, String?> by builder()

            /**
             * Specifies the arguments to be passed to [command].
             */
            public val arguments: SkippableCapturingBuilderInterface<ListBuildingContext<String>.() -> Unit, List<String>> by listBuilder()
        }

        override fun BuildContext.build(): CommandLine = ::CommandLineContext {
            CommandLine(
                redirects = ::redirects.evalOrDefault(emptyList()),
                environment = ::environment.evalOrDefault(emptyMap()),
                workingDirectory = ::workingDirectory.evalOrDefault(Locations.WorkingDirectory),
                command = ::command.evalOrDefault(""),
                arguments = ::arguments.evalOrDefault(emptyList()),
            )
        }

        /**
         * Parses a [commandLine] string and returns an instance of [CommandLine]
         * that would generate the same string again.
         */
        public fun parse(commandLine: String, workingDirectory: Path): CommandLine {
            val plexusCommandLine = PlexusCommandLine(commandLine.replace("\\$LF", ""))
            val rawCommandline = plexusCommandLine.rawCommandline
            return rawCommandline.takeIf { it.isNotEmpty() }
                ?.let { CommandLine(emptyList(), emptyMap(), workingDirectory, it.first(), it.drop(1)) }
                ?: throw IllegalArgumentException("$commandLine is no valid command line.")
        }

        /**
         * Formats the given [commandLineParts] so they can be run in a shell.
         */
        public fun asShellCommand(commandLineParts: Array<String>): String =
            if (commandLineParts.isEmpty()) ""
            else {
                val quoteChar = '\"'
                val escapedChars = charArrayOf('\"')
                val quotingTriggers = charArrayOf(' ', '\t')
                val escapeChar = '\\'
                val force = false
                commandLineParts.joinToString(" ") { part ->
                    quoteAndEscape(part, quoteChar, escapedChars, quotingTriggers, escapeChar, force)
                }
            }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CommandLine

        if (!commandLineParts.contentEquals(other.commandLineParts)) return false

        return true
    }

    override fun hashCode(): Int = commandLineParts.contentHashCode()
}
