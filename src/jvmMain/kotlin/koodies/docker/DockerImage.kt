package koodies.docker

import koodies.builder.StatelessBuilder
import koodies.concurrent.execute
import koodies.concurrent.process.Process.ExitState
import koodies.docker.DockerImage.ImageContext
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.text.Semantics.formattedAs

/**
 * Descriptor of a [DockerImage] identified by the specified [repository],
 * a non-empty list of [path] elements and an optional [specifier]
 * than can either be a tag `@tag` or a digest `@hash`.
 *
 * Examples:
 * - `DockerImage { "bkahlert" / "libguestfs" }`
 * - `DockerImage { "bkahlert" / "libguestfs" tag "latest" }`
 * - `DockerImage { "bkahlert" / "libguestfs" digest "sha256:f466595294e58c1c18efeb2bb56edb5a28a942b5ba82d3c3af70b80a50b4828a" }`
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
) {

    private val repoAndPath = listOf(repository, *path.toTypedArray())

    init {
        repoAndPath.forEach {
            require(PATH_REGEX.matches(it)) {
                "Specified path ${it.formattedAs.input} is not valid (only a-z, 0-9, period, underscore and hyphen; start with letter)"
            }
        }
        tag?.also { require(it.isNotBlank()) { "Specified tag must not be blank." } }
        digest?.also { require(it.isNotBlank()) { "Specified digest must not be blank." } }
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
        with(BACKGROUND) {
            DockerImageListCommandLine {
                options { all by !ignoreIntermediateImages }
                image by this@DockerImage
            }.execute {
                summary("Listing ${this@DockerImage.formattedAs.input} images")
                null
            }.parseImages()
        }

    /**
     * Checks if this image is pulled.
     */
    public val isPulled: Boolean
        get() = with(BACKGROUND) {
            DockerImageListCommandLine {
                image by this@DockerImage
            }.execute {
                summary("Checking if ${this@DockerImage.formattedAs.input} is pulled")
                null
            }.parseImages()
        }.isNotEmpty()

    /**
     * Pulls this image from [Docker Hub](https://hub.docker.com/)
     * while logging progress using `this` logger.
     *
     * Enabled [allTags] to download all tagged images in the repository.
     */
    public fun pull(allTags: Boolean = false): ExitState = with(BACKGROUND) {
        DockerImagePullCommandLine {
            options { this.allTags by allTags }
            image by this@DockerImage
        }.execute {
            summary("Pulling ${this@DockerImage.formattedAs.input}")
            null
        }.waitFor()
    }

    /**
     * Removes this image from the locally stored images.
     *
     * If [force] is specified, a force removal is triggered.
     */
    public fun remove(force: Boolean = false): ExitState = with(BACKGROUND) {
        DockerImageRemoveCommandLine {
            options { this.force by force }
            image by this@DockerImage
        }.execute {
            noDetails("Removing ${this@DockerImage.formattedAs.input}")
            null
        }.waitFor()
    }

    override fun toString(): String = repoAndPath.joinToString("/") + specifier
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DockerImage

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
    @DockerCommandLineDsl
    public object ImageContext {

        /**
         * Describes an official [DockerImage](https://docs.docker.com/docker-hub/official_images/).
         */
        public fun official(repository: String): RepositoryWithPath = RepositoryWithPath(repository, emptyList())

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
     */
    @Suppress("SpellCheckingInspection")
    public companion object :
        StatelessBuilder.PostProcessing<ImageContext, DockerImage, DockerImage>(ImageContext, { DockerImage(repository, path, tag, digest) }) {

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
        public fun list(ignoreIntermediateImages: Boolean = true): List<DockerImage> =
            with(BACKGROUND) {
                DockerImageListCommandLine {
                    options { all by !ignoreIntermediateImages }
                }.execute {
                    noDetails("Listing images")
                    null
                }.parseImages()
            }
    }
}

public typealias DockerImageInit = ImageContext.() -> DockerImage
