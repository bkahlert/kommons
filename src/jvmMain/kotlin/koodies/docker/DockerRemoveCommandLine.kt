package koodies.docker

import koodies.builder.buildArray
import koodies.text.Semantics.FieldDelimiters
import koodies.text.Semantics.formattedAs
import koodies.text.spaced

/**
 * [DockerCommandLine] that removes the specified [containers].
 */
public open class DockerRemoveCommandLine(
    /**
     * Containers to be removed.
     */
    public vararg val containers: String,
    /**
     * Force the removal of a running container (uses SIGKILL)
     */
    public val force: Boolean = false,
    /**
     * Remove the specified link associated with the container.
     */
    public val link: Boolean = false,
    /**
     * Remove anonymous volumes associated with the container
     */
    public val volumes: Boolean = false,
) : DockerCommandLine(
    dockerCommand = "rm",
    arguments = buildArray {
        if (force) +"--force"
        if (link) +"--link"
        if (volumes) +"--volumes"
        addAll(containers)
    },
    name = kotlin.run {
        val forcefully = if (force) " forcefully".formattedAs.warning else ""
        "Removing$forcefully ${containers.joinToString(FieldDelimiters.FIELD.spaced) { it.formattedAs.input }}"
    },
)
