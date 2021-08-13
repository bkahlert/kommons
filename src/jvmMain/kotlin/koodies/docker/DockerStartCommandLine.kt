package koodies.docker

import koodies.builder.buildArray
import koodies.text.Semantics.FieldDelimiters
import koodies.text.Semantics.formattedAs
import koodies.text.spaced

/**
 * [DockerCommandLine] that starts the specified [containers].
 */
public open class DockerStartCommandLine(
    /**
     * Containers to be started.
     */
    public vararg val containers: String,
    /**
     * Attach STDOUT/STDERR and forward signals
     */
    public val attach: Boolean = true,
    /**
     * Attach container's STDIN
     */
    public val interactive: Boolean = false,
) : DockerCommandLine(
    dockerCommand = "start",
    arguments = buildArray {
        if (attach) +"--attach"
        if (interactive) +"--interactive"
        addAll(containers)
    },
    name = "Starting ${containers.joinToString(FieldDelimiters.FIELD.spaced) { it.formattedAs.input }}",
)
