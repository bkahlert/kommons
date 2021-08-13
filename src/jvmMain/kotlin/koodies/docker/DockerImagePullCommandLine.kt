package koodies.docker

import koodies.builder.buildArray
import koodies.text.Semantics.formattedAs
import koodies.text.leftSpaced

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
    dockerImageArguments = buildArray {
        if (allTags) add("--all-tags")
        add(image.toString())
    },
    name = run {
        val allString = if (allTags) "all".formattedAs.warning.leftSpaced else ""
        val pluralString = if (allTags) "s" else ""
        "Pulling$allString ${image.formattedAs.input} image$pluralString"
    }
)
