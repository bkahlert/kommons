package koodies.docker

import koodies.builder.StatelessBuilder
import koodies.concurrent.execute
import koodies.concurrent.process.output
import koodies.docker.DockerContainer.Companion.ContainerContext
import koodies.docker.DockerContainer.Status.Existent.Created
import koodies.docker.DockerContainer.Status.Existent.Dead
import koodies.docker.DockerContainer.Status.Existent.Exited
import koodies.docker.DockerContainer.Status.Existent.Paused
import koodies.docker.DockerContainer.Status.Existent.Removing
import koodies.docker.DockerContainer.Status.Existent.Restarting
import koodies.docker.DockerContainer.Status.Existent.Running
import koodies.docker.DockerContainer.Status.NotExistent
import koodies.io.path.asString
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.text.CharRanges.Alphanumeric
import koodies.text.Semantics.formattedAs
import koodies.text.randomString
import koodies.text.withRandomSuffix
import koodies.text.wrap
import java.nio.file.Path

public inline class DockerContainer(public val name: String) {

    init {
        require(isValid(name)) { "${name.formattedAs.input} is invalid. It needs to match ${REGEX.formattedAs.input}." }
    }

    public val status: Status get() = queryStatus(this)

    public val exists: Boolean get() = status !is NotExistent
    public val isCreated: Boolean get() = status is Created
    public val isRestarting: Boolean get() = status is Restarting
    public val isRunning: Boolean get() = status is Running
    public val isRemoving: Boolean get() = status is Removing
    public val isPaused: Boolean get() = status is Paused
    public val isExited: Boolean get() = status is Exited
    public val isDead: Boolean get() = status is Dead

    public sealed class Status {
        public object NotExistent : Status()
        public sealed class Existent(public val details: String) : Status() {
            public class Created(details: String) : Existent(details)
            public class Restarting(details: String) : Existent(details)
            public class Running(details: String) : Existent(details)
            public class Removing(details: String) : Existent(details)
            public class Paused(details: String) : Existent(details)
            public class Exited(details: String) : Existent(details)
            public class Dead(details: String) : Existent(details)
        }

        public data class Error(val code: Int, val message: String) : Status()
    }

    override fun toString(): String = name


    public companion object : StatelessBuilder.Returning<ContainerContext, DockerContainer>(ContainerContext), Iterable<DockerContainer> {

        override fun iterator(): Iterator<DockerContainer> = query().iterator()

        private fun queryStatus(container: DockerContainer): Status = with(BACKGROUND) {
            DockerPsCommandLine {
                options { all by true; container.run { exactName by name } }
            }.execute {
                noDetails("Checking status of ${container.name.formattedAs.input}")
                null
            }.output {
                split("\t").takeIf { it.size == 3 }?.let { row ->
                    val status = row[1].capitalize()
                    val details = row[2]
                    when (status) {
                        Created::class.simpleName -> Created(details)
                        Restarting::class.simpleName -> Restarting(details)
                        Running::class.simpleName -> Running(details)
                        Removing::class.simpleName -> Removing(details)
                        Paused::class.simpleName -> Paused(details)
                        Exited::class.simpleName -> Exited(details)
                        Dead::class.simpleName -> Dead(details)
                        else -> Status.Error(-1, "Unknown status $status: $details")
                    }
                } ?: Status.Error(-1, "Unknown response: $this")
            }.singleOrNull() ?: NotExistent
        }

        private fun query(): List<DockerContainer> = with(BACKGROUND) {
            DockerPsCommandLine {
                options { all by true }
            }.execute {
                noDetails("Listing ${"all".formattedAs.input} containers")
                null
            }.output {
                split("\t").takeIf { it.size == 3 }?.let {
                    DockerContainer(it[0])
                }
            }
        }

        /**
         * Builder to provide DSL elements to create instances of [DockerImage].
         */
        @DockerCommandLineDsl
        public object ContainerContext {

            /**
             * Sanitizes this char sequence and returns it as a Docker container.
             */
            public val <T : CharSequence> T.sanitized: DockerContainer get() = from(this.toString(), randomSuffix = false)

            /**
             * Sanitizes and appends a random suffix to this char sequence before
             * returning the result as a Docker container.
             */
            public val <T : CharSequence> T.withRandomSuffix: DockerContainer get() = from(this.toString(), randomSuffix = true)

            /**
             * Sanitizes the [Path.getFileName] of this [Path] and returns it as a Docker container.
             */
            public val <T : Path> T.sanitized: DockerContainer get() = from(this, randomSuffix = false)

            /**
             * Sanitizes and appends a random suffix to the [Path.getFileName] of this [Path] before
             * returning the result as a Docker container.
             */
            public val <T : Path> T.withRandomSuffix: DockerContainer get() = from(this, randomSuffix = true)

            /**
             * Appends a random suffix to this docker container (name) and returns it.
             */
            public val DockerContainer.withRandomSuffix: DockerContainer get() = from(name, randomSuffix = true)
        }

        /**
         * Valid length of a Docker container (name).
         */
        private val LENGTH_RANGE: IntRange = 8..128


        /**
         * Pattern that matches a valid Docker container (name).
         */
        public val REGEX: Regex = Regex("[a-zA-Z0-9][a-zA-Z0-9._-]" + "${LENGTH_RANGE.first - 1},${LENGTH_RANGE.last}".wrap("{", "}"))

        private fun isValid(name: String) = name.length in LENGTH_RANGE && name.matches(REGEX)


        /**
         * Checks if the given [name] and the given [suffix] form a valid [DockerContainer]
         * name and if not transforms it to a valid one.
         */
        private fun sanitize(name: String, suffix: String = ""): String {
            val nameWithSuffix = name.take((LENGTH_RANGE.last - suffix.length).coerceAtLeast(0)) + suffix
            if (isValid(nameWithSuffix)) return nameWithSuffix
            var replaceWithXToGuaranteeAValidName = true
            return nameWithSuffix.map { c ->
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
                postfix = (8 - nameWithSuffix.length)
                    .takeIf { it > 0 }?.let {
                        randomString(it, Alphanumeric)
                    } ?: "")
                .let {
                    it.take(it.length.coerceAtMost(LENGTH_RANGE.last - suffix.length)) + suffix
                }
                .also { check(isValid(it)) }
        }

        /**
         * Transforms the given [name] to a valid [DockerContainer] name
         * and adds a [randomSuffix] to it by default.
         */
        public fun from(name: String, randomSuffix: Boolean = false): DockerContainer =
            DockerContainer(sanitize(name, if (randomSuffix) "".withRandomSuffix() else ""))

        /**
         * Transforms the given [Path] to a valid [DockerContainer] name
         * and adds a [randomSuffix] to it by default.
         */
        public fun from(path: Path, randomSuffix: Boolean = true): DockerContainer =
            from(path.fileName.asString(), randomSuffix)
    }
}
