package com.bkahlert.kommons_deprecated.docker

/**
 * [DockerCommandLine] that manages images.
 */
public abstract class DockerImageCommandLine(
    public val dockerImageCommand: String,
    public val dockerImageArguments: List<String>,
    /**
     * The name of this command line.
     */
    name: CharSequence?,
) : DockerCommandLine(
    dockerCommand = "image",
    arguments = buildList {
        add(dockerImageCommand)
        addAll(dockerImageArguments)
    },
    name = name,
)
