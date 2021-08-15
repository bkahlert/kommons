package com.bkahlert.kommons.docker

import com.bkahlert.kommons.builder.buildArray

/**
 * [DockerCommandLine] that manages images.
 */
public abstract class DockerImageCommandLine(
    public val dockerImageCommand: String,
    public val dockerImageArguments: Array<String>,
    /**
     * The name of this command line.
     */
    name: CharSequence?,
) : DockerCommandLine(
    dockerCommand = "image",
    arguments = buildArray {
        add(dockerImageCommand)
        addAll(dockerImageArguments)
    },
    name = name,
)
