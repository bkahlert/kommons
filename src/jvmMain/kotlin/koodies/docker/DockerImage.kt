package koodies.docker

import koodies.docker.DockerImage.Builder

/**
 * Micro DSL to build a [DockerImage] in the style of:
 * - `dockerImage { "bkahlert" / "libguestfs" }`
 * - `dockerImage { "bkahlert" / "libguestfs" tag "latest" }`
 * - `dockerImage { "bkahlert" / "libguestfs" digest "sha256:f466595294e58c1c18efeb2bb56edb5a28a942b5ba82d3c3af70b80a50b4828a" }`
 */
@Suppress("SpellCheckingInspection")
@DockerCommandLineDsl
fun dockerImage(init: Builder.() -> DockerImage): DockerImage = (object : Builder {}).init()

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
@DockerCommandLineDsl
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
    @Suppress("SpellCheckingInspection") val specifier: String?,
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

    /**
     * Builder to provide DSL elements to create instances of [DockerImage].
     */
    @DockerCommandLineDsl
    interface Builder {

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
    @DockerCommandLineDsl
    class RepositoryWithPath(repository: String, path: List<String>) : DockerImage(repository, path, null) {
        constructor(repository: String, path: String) : this(repository, listOf(path))
    }

    companion object {
        /**
         * Pattern that the [repository] and all [path] elements match.
         */
        val PATH_REGEX: Regex = Regex("[a-z0-9]+(?:[._-][a-z0-9]+)*")
    }
}
