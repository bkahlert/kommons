package koodies.docker

import koodies.builder.SlipThroughBuilder
import koodies.docker.DockerImage.ImageContext

@Deprecated("use DockerImage{...}", ReplaceWith("DockerImage(this)"))
fun dockerImage(init: ImageContext.() -> DockerImage): DockerImage = DockerImage(init)

/**
 * Descriptor of a [DockerImage] identified by the specified [repository],
 * a non-empty list of [path] elements and an optional [specifier]
 * than can either be a tag `@tag` or a digest `@hash`.
 *
 * Examples:
 * - `dockerImage { "bkahlert" / "libguestfs" }`
 * - `dockerImage { "bkahlert" / "libguestfs" tag "latest" }`
 * - `dockerImage { "bkahlert" / "libguestfs" digest "sha256:f466595294e58c1c18efeb2bb56edb5a28a942b5ba82d3c3af70b80a50b4828a" }`
 */
@Suppress("SpellCheckingInspection")
open class DockerImage(
    /**
     * The repository name
     */
    val repository: String,
    /**
     * Non-empty list of path elements
     */
    val path: List<String>,
    /**
     * Optional tag or digest.
     */
    val specifier: String?,
) {

    private val repoAndPath = listOf(repository, *path.toTypedArray())

    init {
        repoAndPath.forEach {
            require(PATH_REGEX.matches(it)) { "$it is not valid (only a-z, 0-9, period, underscore and hyphen; start with letter)" }
        }
        specifier?.also {
            require(specifier.startsWith(":") || specifier.startsWith("@")) { "The specifier must either describe a tag `:tagname` or be a digest `@hash`." }
            require(specifier.length > 1) { "The specifier is too short." }
        }
    }

    override fun toString(): String = repoAndPath.joinToString("/") + (specifier ?: "")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DockerImage

        if (repository != other.repository) return false
        if (path != other.path) return false
        if (specifier != other.specifier) return false

        return true
    }

    override fun hashCode(): Int {
        var result = repository.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + (specifier?.hashCode() ?: 0)
        return result
    }


    /**
     * Builder to provide DSL elements to create instances of [DockerImage].
     */
    @DockerCommandLineDsl
    object ImageContext {

        /**
         * Describes an official [DockerImage](https://docs.docker.com/docker-hub/official_images/).
         */
        fun official(repository: String): RepositoryWithPath = RepositoryWithPath(repository, emptyList())

        /**
         * Adds a [path] element to `this` repository.
         */
        infix operator fun String.div(path: String): RepositoryWithPath = RepositoryWithPath(this, path)

        /**
         * Adds another [path] element to `this` [DockerImage].
         */
        infix operator fun RepositoryWithPath.div(path: String): RepositoryWithPath = RepositoryWithPath(repository, this.path + path)

        /**
         * Specifies the [tag] for this [DockerImage].
         */
        infix fun RepositoryWithPath.tag(tag: String): DockerImage = DockerImage(repository, path, ":$tag")

        /**
         * Specifies the [digest] for this [DockerImage].
         */
        infix fun RepositoryWithPath.digest(digest: String): DockerImage = DockerImage(repository, path, "@$digest")
    }

    /**
     * Helper class to enforce consecutive [RepositoryWithPath.path] calls.
     */
    class RepositoryWithPath(repository: String, path: List<String>) : DockerImage(repository, path, null) {
        constructor(repository: String, path: String) : this(repository, listOf(path))
    }

    /**
     * Micro DSL to build a [DockerImage] in the style of:
     * - `dockerImage { "bkahlert" / "libguestfs" }`
     * - `dockerImage { "bkahlert" / "libguestfs" tag "latest" }`
     * - `dockerImage { "bkahlert" / "libguestfs" digest "sha256:f466595294e58c1c18efeb2bb56edb5a28a942b5ba82d3c3af70b80a50b4828a" }`
     */
    @Suppress("SpellCheckingInspection")
    companion object : SlipThroughBuilder<ImageContext, DockerImage, DockerImage> {
        override val context: ImageContext = ImageContext
        override val transform: DockerImage.() -> DockerImage = { DockerImage(repository, path, specifier) }

        /**
         * Pattern that the [repository] and all [path] elements match.
         */
        val PATH_REGEX: Regex = Regex("[a-z0-9]+(?:[._-][a-z0-9]+)*")

        /**
         * Parses any valid [DockerImage] identifier and returns it.
         *
         * If the input is invalid, an [IllegalArgumentException] with details is thrown.
         */
        fun parse(image: String): DockerImage {
            val imageWithTag = image.substringBeforeLast("@").split(":").also { require(it.size <= 2) { "Invalid format. More than one tag found: $it" } }
            val imageWithDigest = image.split("@").also { require(it.size <= 2) { "Invalid format. More than one digest found: $it" } }
            require(!(imageWithTag.size > 1 && imageWithDigest.size > 1)) { "Invalid format. Both tag ${imageWithTag[1]} and digest ${imageWithDigest[1]} found." }
            val specifier: String? = imageWithTag.takeIf { it.size == 2 }?.let { ":${it[1]}" } ?: imageWithDigest.takeIf { it.size == 2 }?.let { "@${it[1]}" }
            val (repository, path) = imageWithTag[0].split("/").map { it.trim() }.let { it[0] to it.drop(1) }
            return DockerImage(repository, path, specifier)
        }
    }
}
