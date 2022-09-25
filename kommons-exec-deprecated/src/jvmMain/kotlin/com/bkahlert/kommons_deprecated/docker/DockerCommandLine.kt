package com.bkahlert.kommons_deprecated.docker

import com.bkahlert.kommons_deprecated.exec.CommandLine
import com.bkahlert.kommons_deprecated.exec.Process.ExitState.ExitStateHandler

/**
 * Specialized [CommandLine] to run [Docker] commands
 * like `docker info`.
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
    /**
     * The name of this command line.
     */
    name: CharSequence?,
) : CommandLine("docker", listOf(dockerCommand, *arguments.toTypedArray()), name = name) {

    public constructor(dockerCommand: String, vararg arguments: String, name: CharSequence?) : this(dockerCommand, arguments.toList(), name)

    override val exitStateHandler: ExitStateHandler? = DockerExitStateHandler

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DockerCommandLine

        if (!commandLineParts.contentEquals(other.commandLineParts)) return false

        return true
    }

    override fun hashCode(): Int = commandLineParts.contentHashCode()
}
