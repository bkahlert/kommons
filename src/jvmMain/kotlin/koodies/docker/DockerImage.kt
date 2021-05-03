package koodies.docker

import koodies.builder.Init
import koodies.builder.StatelessBuilder
import koodies.collections.head
import koodies.collections.tail
import koodies.docker.DockerExitStateHandler.Failure
import koodies.docker.DockerImage.Companion.parse
import koodies.docker.DockerImage.ImageContext
import koodies.docker.DockerRunCommandLine.Options
import koodies.docker.DockerRunCommandLine.Options.Companion.OptionsContext
import koodies.exec.Exec
import koodies.exec.Executable
import koodies.exec.Process.ExitState
import koodies.exec.parse
import koodies.logging.FixedWidthRenderingLogger
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.logging.RenderingLogger
import koodies.or
import koodies.requireSaneInput
import koodies.text.Semantics.formattedAs
import koodies.text.takeUnlessBlank

/**
 * Descriptor of a [DockerImage] identified by the specified [repository],
 * a list of [path] elements and an optional [specifier]
 * than can either be a [tag] `@tag` or a [digest] `@hash`.
 *
 * Examples:
 * - `DockerImage { "bkahlert" / "libguestfs" }`
 * - `DockerImage { "bkahlert" / "libguestfs" tag "latest" }`
 * - `DockerImage { "bkahlert" / "libguestfs" digest "sha256:f466595294e58c1c18efeb2bb56edb5a28a942b5ba82d3c3af70b80a50b4828a" }`
 *
 * Parseable strings are allowed as well:
 * - `DockerImage { "bkahlert/libguestfs" }`
 * - `DockerImage { "bkahlert/libguestfs:latest" }`
 * - `DockerImage { "bkahlert/libguestfs@sha256:f466595294e58c1c18efeb2bb56edb5a28a942b5ba82d3c3af70b80a50b4828a" }`
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
    public val path: List<String>,

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
            it.requireSaneInput()
            require(PATH_REGEX.matches(it)) {
                "Specified path ${it.formattedAs.input} is not valid (only a-z, 0-9, period, underscore and hyphen; start with letter)"
            }
        }
        tag?.also {
            it.requireSaneInput()
            require(it.isNotBlank()) { "Specified tag must not be blank." }
        }
        digest?.also {
            it.requireSaneInput()
            require(it.isNotBlank()) { "Specified digest must not be blank." }
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
     * Lists locally available instances this image.
     */
    public fun list(ignoreIntermediateImages: Boolean = true): List<DockerImage> =
        DockerImageListCommandLine {
            options { all by !ignoreIntermediateImages }
            image by this@DockerImage
        }.exec.logging(BACKGROUND) {
            summary("Listing ${this@DockerImage.formattedAs.input} images")
        }.parseImages()

    /**
     * Checks if this image is pulled.
     */
    public val isPulled: Boolean get() = with(BACKGROUND) { isPulled }

    /**
     * Current pulled state of this image—queried using `this` [RenderingLogger].
     */
    public val RenderingLogger.isPulled: Boolean
        get() = DockerImageListCommandLine {
            image by this@DockerImage
        }.exec.logging(this) {
            summary("Checking if ${this@DockerImage.formattedAs.input} is pulled")
        }.parseImages().isNotEmpty()

    /**
     * Pulls this image from [Docker Hub](https://hub.docker.com/)
     * while logging progress using `this` logger.
     *
     * Enabled [allTags] to download all tagged images in the repository.
     */
    public fun pull(allTags: Boolean = false, logger: RenderingLogger = BACKGROUND): ExitState =
        DockerImagePullCommandLine {
            options { this.allTags by allTags }
            image by this@DockerImage
        }.exec.logging(logger) {
            summary("Pulling ${this@DockerImage.formattedAs.input}")
        }.waitFor()

    /**
     * Removes this image from the locally stored images.
     *
     * If [force] is specified, a force removal is triggered.
     */
    public fun remove(force: Boolean = false, logger: RenderingLogger = BACKGROUND): ExitState =
        DockerImageRemoveCommandLine {
            options { this.force by force }
            image by this@DockerImage
        }.exec.logging(logger) {
            noDetails("Removing ${this@DockerImage.formattedAs.input}")
        }.waitFor()

    /**
     * Returns a [DockerRunCommandLine] that runs `this` [Executable]
     * using `this` [DockerImage]
     * and default options [Options.autoCleanup], [Options.interactive] and [Options.name] derived from [Executable.summary].
     */
    public val Executable<Exec>.dockerized: DockerRunCommandLine
        get() = dockerized(this@DockerImage)

    /**
     * Returns a [DockerRunCommandLine] that runs `this` [Executable]
     * using `this` [DockerImage]
     * and the specified [options].
     */
    public fun Executable<Exec>.dockerized(options: Options): DockerRunCommandLine =
        dockerized(this@DockerImage, options)

    /**
     * Returns a [DockerRunCommandLine] that runs `this` [Executable]
     * using `this` [DockerImage]
     * and the [Options] built by [options].
     */
    public fun Executable<Exec>.dockerized(options: Init<OptionsContext>): DockerRunCommandLine =
        dockerized(this@DockerImage, Options(options))

    private val string by lazy { repoAndPath.joinToString("/") + specifier }
    override val length: Int = string.length
    override fun get(index: Int): Char = string[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = string.subSequence(startIndex, endIndex)
    override fun toString(): String = string
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
        result = 31 * result + specifier.hashCode()
        return result
    }

    /**
     * Builder to provide DSL elements to create instances of [DockerImage].
     */

    public object ImageContext {

        /**
         * Adds a [path] element to `this` repository.
         */
        public infix operator fun String.div(path: String): RepositoryWithPath = RepositoryWithPath(this, path)

        /**
         * Adds another [path] element to `this` [DockerImage].
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
     * - `DockerImage { "bkahlert" / "libguestfs" digest "sha256:f466595294e58c1c18efeb2bb56edb5a28a942b5ba82d3c3af70b80a50b4828a" }`
     *
     * If only a string is provided it will be parsed accordingly:
     * - `DockerImage { "bkahlert/libguestfs" }`
     * - `DockerImage { "bkahlert/libguestfs:latest" }`
     * - `DockerImage { "bkahlert/libguestfs@sha256:f466595294e58c1c18efeb2bb56edb5a28a942b5ba82d3c3af70b80a50b4828a" }`
     */
    @Suppress("SpellCheckingInspection")
    public companion object :
        StatelessBuilder.PostProcessing<ImageContext, CharSequence, DockerImage>(ImageContext, {
            if (this is DockerImage) DockerImage(repository, path, tag, digest)
            else parse(toString())
        }) {

        /**
         * Pattern that the [repository] and all [path] elements match.
         */
        public val PATH_REGEX: Regex = Regex("[a-z0-9]+(?:[._-][a-z0-9]+)*")

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
                    ?: null to null
            val (repository, path) = imageWithTag[0].split("/").map { it.trim() }.let { it[0] to it.drop(1) }
            return DockerImage(repository, path, tag, digest)
        }

        /**
         * Lists locally available instances this image.
         */
        public fun list(ignoreIntermediateImages: Boolean = true, logger: FixedWidthRenderingLogger = BACKGROUND): List<DockerImage> =
            DockerImageListCommandLine {
                options { all by !ignoreIntermediateImages }
            }.exec.logging(logger) {
                noDetails("Listing images")
            }.parseImages()
    }
}

/**
 * Type of the argument supported by [DockerImage] builder.
 *
 * @see DockerImage.Companion
 */
public typealias DockerImageInit = ImageContext.() -> CharSequence

private fun Exec.parseImages(): List<DockerImage> {
    return parse.columns<DockerImage, Failure>(3) { (repoAndPath, tag, digest) ->
        val (repository, path) = repoAndPath.split("/").let { it.head to it.tail }
        repository.takeUnlessBlank()?.let { repo ->
            DockerImage(repo, path, tag.takeUnlessBlank(), digest.takeUnlessBlank())
        }
    } or { emptyList() }
}
