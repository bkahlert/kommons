package koodies.docker

import koodies.builder.StatelessBuilder
import koodies.concurrent.execute
import koodies.docker.DockerContainer.Companion.ContainerContext
import koodies.docker.DockerContainer.State.Error
import koodies.docker.DockerContainer.State.Existent.Created
import koodies.docker.DockerContainer.State.Existent.Dead
import koodies.docker.DockerContainer.State.Existent.Exited
import koodies.docker.DockerContainer.State.Existent.Paused
import koodies.docker.DockerContainer.State.Existent.Removing
import koodies.docker.DockerContainer.State.Existent.Restarting
import koodies.docker.DockerContainer.State.Existent.Running
import koodies.docker.DockerContainer.State.NotExistent
import koodies.io.path.asString
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.map
import koodies.or
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

    public val state: State get() = queryState(this)

    public val exists: Boolean get() = state !is NotExistent
    public val isCreated: Boolean get() = state is Created
    public val isRestarting: Boolean get() = state is Restarting
    public val isRunning: Boolean get() = state is Running
    public val isRemoving: Boolean get() = state is Removing
    public val isPaused: Boolean get() = state is Paused
    public val isExited: Boolean get() = state is Exited
    public val isDead: Boolean get() = state is Dead

    public sealed class State {
        public object NotExistent : State()
        public sealed class Existent(public val status: String) : State() {
            public class Created(status: String) : Existent(status)
            public class Restarting(status: String) : Existent(status)
            public class Running(status: String) : Existent(status)
            public class Removing(status: String) : Existent(status)
            public class Paused(status: String) : Existent(status)
            public class Exited(status: String) : Existent(status)
            public class Dead(status: String) : Existent(status)
        }

        public data class Error(val code: Int, val message: String) : State()
    }

    override fun toString(): String = name


    public companion object : StatelessBuilder.Returning<ContainerContext, DockerContainer>(ContainerContext), Iterable<DockerContainer> {

        override fun iterator(): Iterator<DockerContainer> = query().iterator()

        private fun queryState(container: DockerContainer): State = with(BACKGROUND) {
            DockerPsCommandLine {
                options { all by true; container.run { exactName by name } }
            }.execute {
                noDetails("Checking status of ${container.name.formattedAs.input}")
                null
            }.parse.columns(3) { (_, state, status) ->
                when (state.capitalize()) {
                    Created::class.simpleName -> Created(status)
                    Restarting::class.simpleName -> Restarting(status)
                    Running::class.simpleName -> Running(status)
                    Removing::class.simpleName -> Removing(status)
                    Paused::class.simpleName -> Paused(status)
                    Exited::class.simpleName -> Exited(status)
                    Dead::class.simpleName -> Dead(status)
                    else -> Error(-1, "Unknown status $state: $status")
                }
            }.map { singleOrNull() ?: NotExistent }.or { error(it) }
        }

        private fun query(): List<DockerContainer> = with(BACKGROUND) {
            DockerPsCommandLine {
                options { all by true }
            }.execute {
                noDetails("Listing ${"all".formattedAs.input} containers")
                null
            }.parse.columns(3) { (name, _, _) ->
                DockerContainer(name)
            }.or { error(it) }
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
