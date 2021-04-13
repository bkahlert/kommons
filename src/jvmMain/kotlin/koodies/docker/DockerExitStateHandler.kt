package koodies.docker

import koodies.collections.head
import koodies.collections.tail
import koodies.concurrent.process.IO.ERR
import koodies.concurrent.process.IO.OUT
import koodies.concurrent.process.Process.ExitState
import koodies.concurrent.process.Process.ExitState.ExitStateHandler
import koodies.concurrent.process.Process.ProcessState.Terminated
import koodies.concurrent.process.err
import koodies.concurrent.process.merge
import koodies.debug.asEmoji
import koodies.docker.DockerExitStateHandler.Failure.BadRequest
import koodies.docker.DockerExitStateHandler.Failure.UnknownError
import koodies.lowerSentenceCaseName
import koodies.text.ANSI.ansiRemoved
import koodies.text.Semantics.Symbols
import koodies.text.Semantics.formattedAs
import koodies.text.containsAll
import koodies.text.rightSpaced
import koodies.text.spaced
import koodies.text.splitPascalCase
import kotlin.reflect.KClass
import kotlin.text.RegexOption.IGNORE_CASE

public object DockerExitStateHandler : ExitStateHandler {

    // TODO sealed interface so DaemonParser does no longer have to handle ExitState.Failure
    // public sealed interface DockerExitState

    override fun handle(terminated: Terminated): ExitState = kotlin.runCatching {
        if (terminated.exitCode == 0) handleSuccess(terminated)
        else handleFailure(terminated)
    }.getOrElse { cause -> throw ParseException(terminated.io.merge { it is OUT || it is ERR }, cause) }

    private fun handleSuccess(terminated: Terminated) = Success(terminated, "🐳 💭 ${true.asEmoji}")

    private val messageSplitRegex = Regex(": ")
    private val errorPrologue = Regex("Error(?:\\s+.*?\\s+daemon)?", IGNORE_CASE)

    private fun handleFailure(terminated: Terminated): Failure {
        val errorMessage = terminated.io.err.first().ansiRemoved
        val (error, message) = errorMessage.split(messageSplitRegex, limit = 2)

        if (!errorPrologue.matches(error)) throw ParseException(errorMessage)

        val parts = message.split(messageSplitRegex, limit = 2)
        return when (parts.size) {
            2 -> BadRequest.from(parts[0], parts[1], terminated)
            1 -> BadRequest.from(message, terminated)
            else -> null
        } ?: UnknownError(message, terminated)
    }

    public class Success(
        terminated: Terminated,
        status: String,
    ) : ExitState.Success(terminated.pid, terminated.io, status)

    public sealed class Failure(
        terminated: Terminated,
        exitCode: Int = terminated.exitCode,
        state: String? = null,
        status: String = terminated.status,
    ) : ExitState.Failure(exitCode,
        terminated.pid,
        io = terminated.io,
        status = listOfNotNull(state, status).let { messageParts ->
            messageParts.head.formattedAs.error + messageParts.tail.joinToString("") { ": $it" }
        }) {

        public sealed class BadRequest(
            terminated: Terminated,
            public val statusCode: Int,
            status: String,
        ) : DockerExitStateHandler.Failure(terminated, status = status) {

            override val successful: Boolean = false
            override val symbol: String = Symbols.Negative
            override val textRepresentation: String? get() = this::class.lowerSentenceCaseName.formattedAs.error
            override fun format(): String = textRepresentation + symbol.spaced + status
            override fun toString(): String = symbol.rightSpaced + textRepresentation


            public class NoSuchContainer(terminated: Terminated, status: String) : BadRequest(terminated, 404, status)

            // Error: No such image
            public class NoSuchImage(terminated: Terminated, status: String) : BadRequest(terminated, 404, status)
            public class PathDoesNotExistInsideTheContainer(terminated: Terminated, state: String, status: String) : BadRequest(terminated, 404, status)

            public class NameAlreadyInUse(terminated: Terminated, status: String) : BadRequest(terminated, 409, status)
            public open class Conflict(terminated: Terminated, status: String) : BadRequest(terminated, 409, status)

            public class CannotRemoveRunningContainer(terminated: Terminated, status: String) : BadRequest(terminated, 409, status = status) {
                override val textRepresentation: String get() = status.formattedAs.error
                override fun format(): String = symbol.rightSpaced + textRepresentation
                override fun toString(): String = format()
            }

            public class CannotKillContainer(terminated: Terminated, private val wrapped: Failure) :
                BadRequest(terminated, (wrapped as? BadRequest)?.statusCode ?: 500, wrapped.status) {
                override val textRepresentation: String? get() = wrapped.textRepresentation

                public companion object {
                    public fun parseWrapped(terminated: Terminated, status: String): Failure {
                        val (affected, innerStatus) = status.split(messageSplitRegex, limit = 2)
                        return when {
                            innerStatus.containsAll("container", "not", "running", ignoreCase = true) -> object : Conflict(terminated, affected) {
                                override val textRepresentation: String = "container is not running"
                            }

                            innerStatus.split(messageSplitRegex, limit = 2)
                                .let { it.size == 2 && it[0].containsAll("no", "such", "container", ignoreCase = true) } -> NoSuchContainer(terminated,
                                innerStatus.split(messageSplitRegex, limit = 2)[1])

                            else -> UnknownError(status, terminated)
                        }
                    }
                }
            }


            public companion object {
                // ThisIsAClassName -> This is a class name
                protected inline val <reified T : KClass<*>> T.expectedErrorMessage: String?
                    get() = simpleName?.splitPascalCase()?.joinToString(" ")?.capitalize()

                private inline fun <reified T : Any> String.matches() = equals(T::class.expectedErrorMessage, ignoreCase = true)

                public fun from(message: String, status: String, terminated: Terminated): BadRequest? = with(message) {
                    when {
                        matches<NoSuchContainer>() -> NoSuchContainer(terminated, status)
                        matches<NoSuchImage>() -> NoSuchImage(terminated, status)
                        matches<PathDoesNotExistInsideTheContainer>() -> PathDoesNotExistInsideTheContainer(terminated, message, status)
                        matches<NameAlreadyInUse>() -> NameAlreadyInUse(terminated, status)
                        matches<Conflict>() -> Conflict(terminated, status)
                        matches<CannotKillContainer>() -> CannotKillContainer(terminated, CannotKillContainer.parseWrapped(terminated, status))
                        else -> null
                    }
                }

                public fun from(message: String, terminated: Terminated): BadRequest? = with(message) {
                    when {
                        listOf("remove", "running container", "force").all { message.contains(it, ignoreCase = true) } -> {
                            CannotRemoveRunningContainer(terminated,
                                "You cannot remove a running container. Stop the container before attempting removal or force remove.")
                        }
                        else -> null
                    }
                }
            }
        }

        public class UnknownError(
            errorMessage: String,
            terminated: Terminated,
        ) : DockerExitStateHandler.Failure(terminated, state = "Unknown error from Docker daemon", status = errorMessage.formattedAs.error) {
            override fun toString(): String = status
        }
    }

    public class ParseException(
        errorMessage: String,
        cause: Throwable? = null,
    ) : RuntimeException("Error parsing response from Docker daemon: ${errorMessage.formattedAs.error}", cause)
}
