package koodies.docker

import koodies.builder.BuilderTemplate
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.CommandLine.Companion.CommandLineContext
import koodies.concurrent.process.ManagedProcess
import koodies.io.path.Locations
import java.nio.file.Path

/**
 * A docker command as it can be run in a shell.
 */
public open class DockerCommandLine(
    /**
     * Redirects like `2>&1` to be used when running this command line.
     */
    redirects: List<String>,
    /**
     * The environment to be exposed to the [ManagedProcess] that runs this
     * docker command line.
     */
    environment: Map<String, String>,
    /**
     * The working directory of the [ManagedProcess] that runs this
     * docker command line.
     */
    workingDirectory: Path,
    /**
     * The docker command to be executed.
     */
    dockerCommand: String,
    /**
     * The arguments to be passed to [dockerCommand].
     */
    arguments: List<String>,
) : CommandLine(redirects, environment, workingDirectory, "docker", listOf(dockerCommand, *arguments.toTypedArray())) {

    public constructor(
        redirects: List<String>,
        environment: Map<String, String>,
        workingDirectory: Path,
        dockerCommand: String,
        vararg arguments: String,
    ) : this(redirects, environment, workingDirectory, dockerCommand, arguments.toList())

    public constructor(
        environment: Map<String, String>,
        workingDirectory: Path,
        dockerCommand: String,
        vararg arguments: String,
    ) : this(emptyList(), environment, workingDirectory, dockerCommand, arguments.toList())

    public constructor(
        workingDirectory: Path,
        dockerCommand: String,
        vararg arguments: String,
    ) : this(emptyList(), emptyMap(), workingDirectory, dockerCommand, arguments.toList())

    public constructor(
        dockerCommand: String,
        vararg arguments: String,
    ) : this(emptyList(), emptyMap(), Locations.WorkingDirectory, dockerCommand, arguments.toList())

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
            DockerCommandLine(
                redirects = ::redirects.eval(),
                environment = ::environment.eval(),
                workingDirectory = ::workingDirectory.evalOrDefault { Locations.WorkingDirectory },
                dockerCommand = ::command.eval(),
                arguments = ::arguments.eval<List<String>>()
            )
        }
    }
}
