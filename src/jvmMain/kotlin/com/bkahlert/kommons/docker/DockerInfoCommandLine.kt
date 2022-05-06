package com.bkahlert.kommons.docker

import com.bkahlert.kommons.text.Semantics
import com.bkahlert.kommons.text.Semantics.formattedAs

/**
 * [DockerCommandLine] that displays system wide information regarding the Docker installation.
 */
public open class DockerInfoCommandLine(
    /**
     * Format the output using the given Go template.
     */
    public val format: String? = null,
    /**
     * Optional information what is being queried. Only used for logging.
     */
    query: List<String> = emptyList(),
) : DockerCommandLine(
    dockerCommand = "info",
    arguments = buildList {
        format?.also {
            add("--format")
            add(it)
        }
    },
    name = "docker info" + if (query.isNotEmpty()) " " + query.joinToString(Semantics.FieldDelimiters.UNIT) { it.formattedAs.input } else ""
)
