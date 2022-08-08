package com.bkahlert.kommons.docker

import com.bkahlert.kommons.Kommons
import com.bkahlert.kommons.builder.StatelessBuilder
import com.bkahlert.kommons.docker.DockerExitStateHandler.Failed
import com.bkahlert.kommons.docker.DockerImage.Companion.parse
import com.bkahlert.kommons.docker.DockerImage.ImageContext
import com.bkahlert.kommons.docker.DockerRunCommandLine.Options
import com.bkahlert.kommons.exec.Exec
import com.bkahlert.kommons.exec.Executable
import com.bkahlert.kommons.exec.Process.ExitState
import com.bkahlert.kommons.exec.RendererProviders.noDetails
import com.bkahlert.kommons.exec.output
import com.bkahlert.kommons.exec.parse
import com.bkahlert.kommons.fold
import com.bkahlert.kommons.head
import com.bkahlert.kommons.tail
import com.bkahlert.kommons.text.LineSeparators.lines
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.text.takeUnlessBlank

/**
 * Descriptor of a [DockerImage] identified by the specified [repository],
 * a list of [path] elements and an optional [specifier]
 * than can either be a [tag] `@tag` or a [digest] `@hash`.
 *
 * Examples:
 * - `DockerImage { "bkahlert" / "libguestfs" }`
 * - `DockerImage { "bkahlert" / "libguestfs" tag "latest" }`
 * - `DockerImage { "bkahlert" / "libguestfs" digest "sha256:e8fdf16c69a9155b0e30cdc9b2f872232507f5461be2e7dff307f4c1b50faa20" }`
 *
 * Parseable strings are allowed as well:
 * - `DockerImage { "bkahlert/libguestfs" }`
 * - `DockerImage { "bkahlert/libguestfs:latest" }`
 * - `DockerImage { "bkahlert/libguestfs@sha256:e8fdf16c69a9155b0e30cdc9b2f872232507f5461be2e7dff307f4c1b50faa20" }`
 */
@Suppress("SpellCheckingInspection")
public open class DockerImage(

    /**
     * The repository name
     */
    public val repository: String,

    /**
     * Non-empty list of path elements
     */
    public val path: List<String> = emptyList(),

    /**
     * Optional tag.
     */
    public val tag: String? = null,

    /**
     * Optional digest.
     */
    public val digest: String? = null,

    ) : CharSequence {

    private val repoAndPath = listOf(repository, *path.toTypedArray())

    init {
        repoAndPath.forEach {
            require(PATH_REGEX.matches(it)) {
                "Specified path ${it.formattedAs.input} is not valid (only a-z, 0-9, period, underscore and hyphen; start with letter)."
            }
        }
        tag?.also {
            require(TAG_REGEX.matches(it)) {
                "Specified tag ${it.formattedAs.input} is not valid (only a-z, A-Z, 0-9, period, underscore and hyphen; not start with period or hyphen)."
            }
        }
        digest?.also {
            require(DIGEST_REGEX.matches(it)) {
                "Specified digest ${it.formattedAs.input} is not valid."
            }
        }
    }

    /**
     * Synthetic property which defaults to the formatted [digest] and only returns
     * the formatted [tag] if no [digest] is specified.
     *
     * If neither [digest] nor [tag] are specified, this string is empty.
     */
    public val specifier: String get() = digest?.let { "@$it" } ?: tag?.let { ":$it" } ?: ""

    /**
     * Lists locally available instances of this image.
     */
    public fun list(
        ignoreIntermediateImages: Boolean = true,
    ): Set<DockerImage> =
        DockerImageListCommandLine(this, !ignoreIntermediateImages)
            .exec.logging(renderer = noDetails())
            .parseImages()

    /**
     * Checks if this image is pulled.
     */
    public val isPulled: Boolean
        get() = DockerImageListCommandLine(this)
            .exec.logging(renderer = noDetails())
            .parseImages()
            .isNotEmpty()

    /**
     * Pulls this image from [Docker Hub](https://hub.docker.com/).
     *
     * Enabled [allTags] to download all tagged images in the repository.
     */
    public fun pull(
        allTags: Boolean = false,
    ): ExitState =
        DockerImagePullCommandLine(this, allTags)
            .exec.logging(renderer = noDetails()).waitFor()

    /**
     * Removes this image from the locally stored images.
     *
     * If [force] is specified, a force removal is triggered.
     */
    public fun remove(
        force: Boolean = false,
    ): ExitState =
        DockerImageRemoveCommandLine(this, force = force)
            .exec.logging(renderer = noDetails()).waitFor()

    /**
     * Contains a list of tags available on [Docker Hub](https://registry.hub.docker.com).
     */
    public val tagsOnDockerHub: List<String>
        get() {
            val fullPath = repoAndPath.joinToString("/")
            val page = 1
            val pageSize = 100
            val url = "https://registry.hub.docker.com/api/content/v1/repositories/public/library/$fullPath/tags?page=$page&page_size=$pageSize"
            return Kommons.ExecTemp.curlJq("hub.docker.com tags") {
                "curl '$url' 2>/dev/null | jq -r '.results[].name' | sort"
            }.io.output.ansiRemoved.lines()
        }

    /**
     * Returns a [DockerRunCommandLine] that runs this [Executable]
     * using this [DockerImage]
     * and default options [Options.autoCleanup], [Options.interactive] and [Options.name] derived from [Executable.content].
     */
    public val Executable<Exec>.dockerized: DockerRunCommandLine
        get() = dockerized(this@DockerImage)

    /**
     * Returns a [DockerRunCommandLine] that runs this [Executable]
     * using this [DockerImage]
     * and the specified [options].
     */
    public fun Executable<Exec>.dockerized(options: Options): DockerRunCommandLine =
        dockerized(this@DockerImage, options)

    private val string = repoAndPath.joinToString("/") + specifier
    override val length: Int = string.length
    override fun get(index: Int): Char = string[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = string.subSequence(startIndex, endIndex)
    override fun toString(): String = string
    public fun toString(includeSpecifier: Boolean): String =
        if (includeSpecifier) string
        else repoAndPath.joinToString("/")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DockerImage) return false

        if (repository != other.repository) return false
        if (path != other.path) return false
        if (digest != null && other.digest != null) return digest == other.digest
        if (tag != null && other.tag != null) return tag == other.tag
        return true
    }

    override fun hashCode(): Int {
        var result = repository.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }

    /**
     * Builder to provide DSL elements to create instances of [DockerImage].
     */

    public object ImageContext {

        /**
         * Adds a [path] element to this repository.
         */
        public infix operator fun String.div(path: String): RepositoryWithPath = RepositoryWithPath(this, path)

        /**
         * Adds another [path] element to this [DockerImage].
         */
        public infix operator fun RepositoryWithPath.div(path: String): RepositoryWithPath = RepositoryWithPath(repository, this.path + path)

        /**
         * Specifies the [tag] for this [DockerImage].
         */
        public infix fun RepositoryWithPath.tag(tag: String): DockerImage = DockerImage(repository, path, tag = tag)

        /**
         * Specifies the [digest] for this [DockerImage].
         */
        public infix fun RepositoryWithPath.digest(digest: String): DockerImage = DockerImage(repository, path, digest = digest)
    }

    /**
     * Helper class to enforce consecutive [RepositoryWithPath.path] calls.
     */
    public class RepositoryWithPath(repository: String, path: List<String>) : DockerImage(repository, path) {

        public constructor(repository: String, path: String) : this(repository, listOf(path))
    }

    /**
     * Micro DSL to build a [DockerImage] in the style of:
     * - `DockerImage { "bkahlert" / "libguestfs" }`
     * - `DockerImage { "bkahlert" / "libguestfs" tag "latest" }`
     * - `DockerImage { "bkahlert" / "libguestfs" digest "sha256:e8fdf16c69a9155b0e30cdc9b2f872232507f5461be2e7dff307f4c1b50faa20" }`
     *
     * If only a string is provided it will be parsed accordingly:
     * - `DockerImage { "bkahlert/libguestfs" }`
     * - `DockerImage { "bkahlert/libguestfs:latest" }`
     * - `DockerImage { "bkahlert/libguestfs@sha256:e8fdf16c69a9155b0e30cdc9b2f872232507f5461be2e7dff307f4c1b50faa20" }`
     */
    @Suppress("SpellCheckingInspection")
    public companion object : StatelessBuilder.PostProcessing<ImageContext, CharSequence, DockerImage>(ImageContext, {
        if (this is DockerImage) DockerImage(repository, path, tag, digest)
        else parse(toString())
    }) {

        /** Pattern that the [repository] and all [path] elements match. */
        public val PATH_REGEX: Regex = Regex("[a-z\\d][a-z\\d._-]*")

        /** Pattern that the [tag]. */
        public val TAG_REGEX: Regex = Regex("\\w[\\w.-]{0,127}")

        /** Pattern that the [digest]. */
        public val DIGEST_REGEX: Regex = Regex("\\w+:[a-z\\d]+")

        /**
         * Parses any valid [DockerImage] identifier and returns it.
         *
         * If the input is invalid, an [IllegalArgumentException] with details is thrown.
         */
        public fun parse(image: String): DockerImage {
            val imageWithTag = image.substringBeforeLast("@").split(":").also { require(it.size <= 2) { "Invalid format. More than one tag found: $it" } }
            val imageWithDigest = image.split("@").also { require(it.size <= 2) { "Invalid format. More than one digest found: $it" } }
            require(!(imageWithTag.size > 1 && imageWithDigest.size > 1)) { "Invalid format. Both tag ${imageWithTag[1]} and digest ${imageWithDigest[1]} found." }
            val (tag: String?, digest: String?) =
                imageWithTag.takeIf { it.size == 2 }?.let { it[1] to null }
                    ?: imageWithDigest.takeIf { it.size == 2 }?.let { null to it[1] }
                    ?: (null to null)
            val (repository, path) = imageWithTag[0].split("/").map { it.trim() }.let { it[0] to it.drop(1) }
            return DockerImage(repository, path, tag, digest)
        }

        /**
         * Lists locally available images.
         */
        public fun list(
            ignoreIntermediateImages: Boolean = true,
        ): Set<DockerImage> =
            DockerImageListCommandLine(all = !ignoreIntermediateImages)
                .exec.logging(renderer = noDetails())
                .parseImages()
    }
}

/**
 * Type of the argument supported by [DockerImage] builder.
 *
 * @see DockerImage.Companion
 */
public typealias DockerImageInit = ImageContext.() -> CharSequence

private fun Exec.parseImages(): Set<DockerImage> {
    return parse.columns<DockerImage, Failed>(3) { (repoAndPath, tag, digest) ->
        val (repository, path) = repoAndPath.split("/").let { it.head to it.tail }
        repository.takeUnlessBlank()?.let { repo ->
            DockerImage(repo, path, tag.takeUnlessBlank(), digest.takeUnlessBlank())
        }
    }.fold({ it.toSet() }, { emptySet() })
}
