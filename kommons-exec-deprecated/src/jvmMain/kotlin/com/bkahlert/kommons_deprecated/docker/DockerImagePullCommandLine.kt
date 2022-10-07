package com.bkahlert.kommons_deprecated.docker

import com.bkahlert.kommons_deprecated.text.Semantics.formattedAs
import com.bkahlert.kommons.text.startSpaced

/**
 * [DockerCommandLine] that pulls the specified [image].
 */
public open class DockerImagePullCommandLine(
    /**
     * Image to be pulled.
     */
    public val image: DockerImage,
    /**
     * Download all tagged images in the repository
     */
    public val allTags: Boolean = false,
) : DockerImageCommandLine(
    dockerImageCommand = "pull",
    dockerImageArguments = buildList {
        if (allTags) add("--all-tags")
        add(image.toString())
    },
    name = run {
        val allString = if (allTags) "all".formattedAs.warning.startSpaced else ""
        val pluralString = if (allTags) "s" else ""
        "Pulling$allString ${image.formattedAs.input} image$pluralString"
    }
)