package com.bkahlert.kommons.docker

import com.bkahlert.kommons.spaced
import com.bkahlert.kommons.text.Semantics.FieldDelimiters
import com.bkahlert.kommons.text.Semantics.formattedAs

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
    arguments = buildList {
        if (attach) add("--attach")
        if (interactive) add("--interactive")
        addAll(containers)
    },
    name = "Starting ${containers.joinToString(FieldDelimiters.FIELD.spaced) { it.formattedAs.input }}",
)
