package koodies.docker

import koodies.builder.buildArray
import koodies.text.Semantics.FieldDelimiters
import koodies.text.Semantics.formattedAs
import koodies.text.spaced

/**
 * [DockerCommandLine] that kills the specified [containers].
 */
public open class DockerKillCommandLine(
    /**
     * Containers to be killed.
     */
    public vararg val containers: String,
    /**
     * Signal to send to the container (default: KILL)
     */
    public val signal: String? = null,
) : DockerCommandLine(
    dockerCommand = "kill",
    arguments = buildArray {
        signal?.let {
            add("--signal")
            add(it)
        }
        addAll(containers)
    },
    name = "Killing ${containers.joinToString(FieldDelimiters.FIELD.spaced) { it.formattedAs.input }}",
)
