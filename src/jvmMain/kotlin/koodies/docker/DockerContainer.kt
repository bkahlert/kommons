package koodies.docker

import koodies.io.path.asString
import koodies.text.CharRanges.Alphanumeric
import koodies.text.randomString
import koodies.text.withRandomSuffix
import java.nio.file.Path


inline class DockerContainerName(val name: String) {
    val sanitized: String get() = name.sanitize()

    override fun toString(): String = sanitized

    companion object {
        /**
         * A [Regex] that matches valid Docker container names.
         */
        private val regex: Regex = Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{7,}")

        private fun isValid(name: String) = name.matches(regex)

        /**
         * Checks if this [String] is a valid [DockerContainerName]
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
         * Transforms this [String] to a valid [DockerContainerName].
         */
        public fun String.toContainerName(): DockerContainerName = DockerContainerName(sanitize())

        /**
         * Transforms this [Path] to a unique [DockerContainerName].
         */
        public fun Path.toUniqueContainerName(): DockerContainerName =
            DockerContainerName(fileName.asString().take(18).withRandomSuffix())
    }
}
