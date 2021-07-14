package koodies.exec

import koodies.builder.BuilderTemplate
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.ListBuildingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.exec.CommandLine.Companion.CommandLineContext
import koodies.exec.Process.ExitState
import koodies.exec.Process.ExitState.ExitStateHandler
import koodies.io.path.asPath
import koodies.io.path.executable
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.lines
import koodies.text.LineSeparators.trailingLineSeparatorRemoved
import koodies.text.unquoted
import org.codehaus.plexus.util.cli.shell.FormattingShell
import java.nio.file.Path
import kotlin.io.path.exists
import org.codehaus.plexus.util.cli.Commandline as PlexusCommandline

/**
 * A command as it can be run in a shell.
 */
public open class CommandLine(

    /**
     * The command to be executed.
     */
    public val command: String,

    /**
     * The arguments to be passed to [command].
     */
    public val arguments: List<String>,

    /**
     * Optional name of this script.
     */
    override val name: CharSequence?,

    /**
     * If set, each run [Exec] delegates its [ExitState] creation to it.
     */
    protected open val exitStateHandler: ExitStateHandler? = null,
) : Executable<Exec>, List<String> by listOf(command, *arguments.toTypedArray()) {

    public constructor(
        command: CharSequence,
        vararg arguments: CharSequence,
        name: CharSequence? = null,
    ) : this(command.toString(), arguments.map { it.toString() }.toList(), name, exitStateHandler = null)

    public constructor(
        command: CharSequence,
        arguments: Iterable<CharSequence>,
        name: CharSequence? = null,
    ) : this(command.toString(), arguments.map { it.toString() }.toList(), name, exitStateHandler = null)

    public constructor(
        commandLine: CommandLine,
    ) : this(commandLine.command, commandLine.arguments, commandLine.name, commandLine.exitStateHandler)

    /**
     * The array consisting of the command and its arguments that make up this command,
     * e.g. `[echo, Hello World!]`.
     */
    public val commandLineParts: Array<String> = arrayOf(command, *arguments.toTypedArray())

    /**
     * This command line as it can be used in a shell,
     * e.g. `'echo' 'Hello World!'`.
     */
    public val shellCommand: String = asShellCommand(commandLineParts)

    /**
     * This command line as it can be used in a shell,
     * but in contrast to [shellCommand] applies line breaks for better readability,
     * e.g.
     * ```shell
     * 'echo' \
     * 'Hello World!'
     * ```
     */
    public val multiLineShellCommand: String = asShellCommand(commandLineParts, " \\$LF")

    /**
     * A human-readable representation of this command line.
     *
     * ***Warning:** The content is not guaranteed to work due to simplified quoting. Use [shellCommand] for an always working command.*
     */
    override val content: CharSequence get() = commandLineParts.joinToString(" ").trailingLineSeparatorRemoved

    override fun toCommandLine(
        environment: Map<String, String>,
        workingDirectory: Path?,
        transform: (String) -> String,
    ): CommandLine = CommandLine(transform(command), arguments.map(transform), name, exitStateHandler)

    public override fun toExec(
        redirectErrorStream: Boolean,
        environment: Map<String, String>,
        workingDirectory: Path?,
        execTerminationCallback: ExecTerminationCallback?,
    ): Exec {

        val process = ProcessBuilder(*commandLineParts).let { pb ->
            pb.redirectErrorStream = redirectErrorStream
            pb.environment.putAll(environment)
            pb.workingDirectory = workingDirectory
            pb.start()
        }
        return JavaExec(process, workingDirectory, this, exitStateHandler, execTerminationCallback)
    }

    /**
     * Contains all accessible files contained in this command line.
     */
    public open val includedFiles: List<Path>
        get() = commandLineParts.map { it.unquoted.asPath() }
            .filter { it != it.root }
            .filter { it.exists() }
            .filterNot { it.executable }

    /**
     * Returns this command line as it can be used in a shell,
     * e.g.
     * ```shell
     * 'echo' \
     * 'Hello World!'
     * ```
     */
    override fun toString(): String = multiLineShellCommand

    /**
     * The [multiLineShellCommand] represented as a list of single-line strings.
     */
    public val lines: List<String> get() = multiLineShellCommand.lines()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CommandLine

        if (!commandLineParts.contentEquals(other.commandLineParts)) return false

        return true
    }

    override fun hashCode(): Int = commandLineParts.contentHashCode()

    public companion object : BuilderTemplate<CommandLineContext, CommandLine>() {

        /**
         * Context to build a [CommandLine].
         */
        @Deprecated("use constructor")
        public class CommandLineContext(override val captures: CapturesMap) : CapturingContext() {

            /**
             * The command to be executed.
             */
            @Deprecated("use property")
            public val command: SkippableCapturingBuilderInterface<() -> String, String?> by builder()

            /**
             * Specifies the arguments to be passed to [command].
             */
            @Deprecated("use property")
            public val arguments: SkippableCapturingBuilderInterface<ListBuildingContext<String>.() -> Unit, List<String>> by listBuilder()
        }

        override fun BuildContext.build(): CommandLine = ::CommandLineContext {
            CommandLine(::command.evalOrDefault(""), ::arguments.evalOrDefault(emptyList()))
        }

        /**
         * Parses a [commandLine] string and returns an instance of [CommandLine]
         * that would generate the same string again.
         */
        public fun parse(commandLine: CharSequence): CommandLine =
            parseOrNull(commandLine) ?: throw IllegalArgumentException("$commandLine is no valid command line.")

        /**
         * Parses a [commandLine] string and returns an instance of [CommandLine]
         * that would generate the same string again.
         */
        public fun parseOrNull(commandLine: CharSequence): CommandLine? {
            val plexusCommandLine = PlexusCommandline(commandLine.toString().replace("\\$LF", "").trailingLineSeparatorRemoved)
            val rawCommandline = plexusCommandLine.rawCommandline
            return rawCommandline.takeIf { it.isNotEmpty() }
                ?.let { CommandLine(it.first(), it.drop(1)) }
        }

        /**
         * Formats the given [commandLineParts] so they can be run in a shell.
         */
        public fun asShellCommand(commandLineParts: Array<String>, joiner: String = " "): String =
            if (commandLineParts.isEmpty()) ""
            else FormattingShell(joiner).run {
                getRawCommandLine(originalExecutable, commandLineParts).last()
            }
    }
}
