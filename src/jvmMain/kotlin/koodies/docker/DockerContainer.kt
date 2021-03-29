package koodies.docker

import koodies.io.path.asString
import koodies.text.CharRanges.Alphanumeric
import koodies.text.randomString
import koodies.text.withRandomSuffix
import java.nio.file.Path

// TODO make proper container
public inline class DockerContainer(public val name: String) {
    public val sanitized: String get() = name.sanitize()

    override fun toString(): String = sanitized

    public companion object {
        /**
         * A [Regex] that matches valid Docker container names.
         */
        private val regex: Regex = Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{7,}")

        private fun isValid(name: String) = name.matches(regex)

        /**
         * Checks if this [String] is a valid [DockerContainer]
         * and if not transforms it.
         */
        private fun String.sanitize(): String {
            if (isValid(this)) return this
            var replaceWithXToGuaranteeAValidName = true
            return map { c ->
                val isAlphaNumeric = Alphanumeric.contains(c)
                if (isAlphaNumeric) replaceWithXToGuaranteeAValidName = false
                if (replaceWithXToGuaranteeAValidName) return@map "X"
                when {
                    isAlphaNumeric -> c
                    "._-".contains(c) -> c
                    c.isWhitespace() -> "-"
                    else -> '_'
                }
            }.joinToString("",
                postfix = (8 - length)
                    .takeIf { it > 0 }?.let {
                        randomString(it, Alphanumeric)
                    } ?: "")
                .also { check(isValid(it)) }
        }

        /**
         * Transforms this [String] to a valid [DockerContainer].
         */
        public fun String.toContainerName(): DockerContainer = DockerContainer(sanitize())

        /**
         * Transforms this [Path] to a unique [DockerContainer].
         */
        public fun Path.toUniqueContainerName(): DockerContainer =
            DockerContainer(fileName.asString().take(18).withRandomSuffix())
    }
}
