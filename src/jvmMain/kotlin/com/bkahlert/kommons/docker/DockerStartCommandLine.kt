package com.bkahlert.kommons.docker

import com.bkahlert.kommons.builder.buildArray
import com.bkahlert.kommons.text.Semantics.FieldDelimiters
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.text.spaced

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
