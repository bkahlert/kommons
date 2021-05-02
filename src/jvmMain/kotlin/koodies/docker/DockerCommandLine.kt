package koodies.docker

import koodies.builder.BuilderTemplate
import koodies.exec.CommandLine
import koodies.exec.CommandLine.Companion.CommandLineContext
import koodies.exec.Process.ExitState.ExitStateHandler

/**
 * A docker command as it can be run in a shell.
 */
public open class DockerCommandLine(
    /**
     * The docker command to be executed.
     */
    dockerCommand: String,
    /**
     * The arguments to be passed to [dockerCommand].
     */
    arguments: List<String>,
) : CommandLine("docker", listOf(dockerCommand, *arguments.toTypedArray())) {

    public constructor(dockerCommand: String, vararg arguments: String) : this(dockerCommand, arguments.toList())

    override val exitStateHandler: ExitStateHandler? = DockerExitStateHandler

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DockerCommandLine

        if (!commandLineParts.contentEquals(other.commandLineParts)) return false

        return true
    }

    override fun hashCode(): Int = commandLineParts.contentHashCode()

    public companion object : BuilderTemplate<CommandLineContext, DockerCommandLine>() {

        override fun BuildContext.build(): DockerCommandLine = ::CommandLineContext {
            DockerCommandLine(::command.eval(), ::arguments.eval<List<String>>())
        }
    }
}
