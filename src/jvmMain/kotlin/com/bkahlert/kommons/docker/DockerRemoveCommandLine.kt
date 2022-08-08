package com.bkahlert.kommons.docker

import com.bkahlert.kommons.text.Semantics.FieldDelimiters
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.text.spaced

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
    arguments = buildList {
        if (force) add("--force")
        if (link) add("--link")
        if (volumes) add("--volumes")
        addAll(containers)
    },
    name = kotlin.run {
        val forcefully = if (force) " forcefully".formattedAs.warning else ""
        "Removing$forcefully ${containers.joinToString(FieldDelimiters.FIELD.spaced) { it.formattedAs.input }}"
    },
)
