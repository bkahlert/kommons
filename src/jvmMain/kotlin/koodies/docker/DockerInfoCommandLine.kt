package koodies.docker

import koodies.builder.buildArray
import koodies.text.Semantics
import koodies.text.Semantics.formattedAs

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
    arguments = buildArray {
        format?.also {
            add("--format")
            add(it)
        }
    },
    name = "Querying info" + if (query.isNotEmpty()) " " + query.joinToString(Semantics.FieldDelimiters.UNIT) { it.formattedAs.input } else ""
)
