package com.bkahlert.kommons_deprecated.docker

import com.bkahlert.kommons_deprecated.text.Semantics.FieldDelimiters
import com.bkahlert.kommons_deprecated.text.Semantics.formattedAs
import com.bkahlert.kommons.text.spaced

/**
 * [DockerImageCommandLine] that removes the specified [images].
 */
public open class DockerImageRemoveCommandLine(
    /**
     * Images to be removed.
     */
    public vararg val images: DockerImage,
    /**
     * Force removal of the image
     */
    public val force: Boolean = false,
) : DockerImageCommandLine(
    dockerImageCommand = "rm",
    dockerImageArguments = buildList {
        if (force) add("--force")
        images.forEach { add(it.toString()) }
    },
    name = kotlin.run {
        val forcefully = if (force) " forcefully".formattedAs.warning else ""
        "Removing$forcefully ${images.joinToString(FieldDelimiters.FIELD.spaced) { it.formattedAs.input }}"
    },
)
