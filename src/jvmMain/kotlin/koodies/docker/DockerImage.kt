package koodies.docker

import koodies.text.quoted

/**
 * Client side usage: `"repo" / "path" [ / ... ] [tag "tag" | digest "sha..."]`,
 *
 * e.g. `"bkahlert" / "guestfish" tag "latest"`
 */
@DockerCommandDsl
interface DockerImageBuilder {
    companion object {
        fun build(init: DockerImageBuilder.() -> Any): DockerImage {
            return when (val rawResult = object : DockerImageBuilder {}.run(init)) {
                is String -> DockerImage(DockerRepository.of(rawResult), OptionalTagOrDigest.none())
                is List<*> -> DockerImage(DockerRepository(rawResult.map { PathComponent.of(it as String) }), OptionalTagOrDigest.none())
                is Pair<*, *> -> {
                    val pathComponents = rawResult.first?.let { it as List<*> }?.map { PathComponent.of(it as String) } ?: error("unknown $rawResult")
                    val optionalTagOrDigest = rawResult.second?.let { it as? OptionalTagOrDigest } ?: error("unknown $rawResult")
                    DockerImage(DockerRepository(pathComponents), optionalTagOrDigest)
                }
                else -> error("unknown $rawResult")
            }
        }
    }

    infix operator fun String.div(next: String): List<String> = listOf(this@div, next)
    infix operator fun List<String>.div(next: String): List<String> = this + next
    infix fun List<String>.tag(tag: String): Pair<List<String>, OptionalTagOrDigest> = this to OptionalTagOrDigest.of(Tag(tag))
    infix fun List<String>.digest(digest: String): Pair<List<String>, OptionalTagOrDigest> = this to OptionalTagOrDigest.of(Digest(digest))
}

@DockerCommandDsl
data class DockerImage(val repository: DockerRepository, val optionalTagOrDigest: OptionalTagOrDigest) {
    companion object {
        fun image(repository: DockerRepository) = DockerImage(repository, OptionalTagOrDigest.none())
        fun imageWithTag(repository: DockerRepository, tag: Tag) = DockerImage(repository, OptionalTagOrDigest.of(tag))
        fun imageWithDigest(repository: DockerRepository, digest: Digest) = DockerImage(repository, OptionalTagOrDigest.of(digest))
    }

    init {
        require(repository.components.isNotEmpty()) { "At least one path components must be present." }
    }

    val formatted: String by lazy {
        "${repository.format()}${optionalTagOrDigest.format()}"
    }

    override fun toString(): String = formatted
}

inline class DockerRepository(val components: List<PathComponent>) {
    companion object {
        fun of(components: List<String>) = DockerRepository(components.run {
            require(isNotEmpty()) { "At least one path components must be present." }
            map { PathComponent.of(it) }
        })

        fun of(vararg components: String) = DockerRepository(components.run {
            require(isNotEmpty()) { "At least one path components must be present." }
            map { PathComponent.of(it) }
        })
    }

    fun format(): String {
        require(components.isNotEmpty()) { "At least one path components must be present." }
        return components.joinToString("/") { it.component }
    }
}

inline class PathComponent(val component: String) {
    companion object {
        /**
         * Pattern a valid [PathComponent] matches.
         */
        val REGEX: Regex = Regex("[a-z0-9]+(?:[._-][a-z0-9]+)*")
        fun isValid(component: String) = REGEX.matches(component)
        fun of(component: String): PathComponent {
            require(isValid(component)) { "${component.quoted} is invalid as it does not match: ${REGEX.pattern}" }
            return PathComponent(component)
        }
    }
}

inline class OptionalTagOrDigest(val tagOrDigest: Pair<Tag?, Digest?>) {
    companion object {
        private val NONE = OptionalTagOrDigest(null to null)
        fun none() = NONE
        fun of(tag: Tag) = OptionalTagOrDigest(tag to null)
        fun of(digest: Digest) = OptionalTagOrDigest(null to digest)
    }

    fun format(): String = tagOrDigest.let { (tag, digest) ->
        tag?.format() ?: digest?.format() ?: ""
    }
}

inline class Tag(val tag: String) {
    fun format() = ":$tag"
}

inline class Digest(val digest: String) {
    fun format() = "@$digest"
}
