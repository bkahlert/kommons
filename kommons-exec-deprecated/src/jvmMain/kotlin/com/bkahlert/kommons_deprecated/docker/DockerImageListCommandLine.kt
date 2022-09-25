package com.bkahlert.kommons_deprecated.docker

import com.bkahlert.kommons_deprecated.text.Semantics.formattedAs

/**
 * [DockerImageCommandLine] that lists locally available instances of [DockerImage].
 */
public open class DockerImageListCommandLine(
    /**
     * Optional image to restrict the listing to.
     */
    public val image: DockerImage? = null,
    /**
     * Show all images (default hides intermediate images)
     */
    public val all: Boolean = false,
) : DockerImageCommandLine(
    dockerImageCommand = "ls",
    dockerImageArguments = buildList {
        if (all) add("--all")
        add("--no-trunc")
        add("--format")
        add("{{.Repository}}\t{{.Tag}}\t{{.Digest}}")
        image?.also { add(it.toString()) }
    },
    name = kotlin.run {
        val allString = if (all) " all".formattedAs.warning else ""
        val imageString = if (image != null) " ${image.formattedAs.input}" else ""
        "Listing$allString$imageString images"
    },
)
