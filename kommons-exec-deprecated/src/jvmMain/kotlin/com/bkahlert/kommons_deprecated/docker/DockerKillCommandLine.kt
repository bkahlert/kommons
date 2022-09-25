package com.bkahlert.kommons_deprecated.docker

import com.bkahlert.kommons_deprecated.text.Semantics.FieldDelimiters
import com.bkahlert.kommons_deprecated.text.Semantics.formattedAs
import com.bkahlert.kommons.text.spaced

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
    arguments = buildList {
        signal?.also {
            add("--signal")
            add(it)
        }
        addAll(containers)
    },
    name = "Killing ${containers.joinToString(FieldDelimiters.FIELD.spaced) { it.formattedAs.input }}",
)
