package koodies.docker

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
import koodies.text.ANSI.ansiRemoved
import koodies.text.Semantics.formattedAs
import koodies.text.splitPascalCase
import kotlin.reflect.KClass

public object DockerExitStateHandler : ExitStateHandler {

    // TODO sealed interface so DaemonParser does no longer have to handle ExitState.Failure
    // public sealed interface DockerExitState

    override fun handle(terminated: Terminated): ExitState = kotlin.runCatching {
        if (terminated.exitCode == 0) handleSuccess(terminated)
        else handleFailure(terminated.io.err.first().ansiRemoved, terminated)
    }.getOrElse { cause -> throw ParseException(terminated.io.merge { it is OUT || it is ERR }, cause) }

    private fun handleSuccess(terminated: Terminated) = Success(terminated, "🐳 💭 ${true.asEmoji}")

    private val regex = Regex(": ")

    private fun handleFailure(errorMessage: String, terminated: Terminated): Failure {
        val (error, message) = errorMessage.split(regex, limit = 2)

        if (!error.equals("Error", ignoreCase = true)) {
            throw ParseException(errorMessage)
        }

        val (state, status) = message.split(regex, limit = 2)

        return BadRequest.from(state, status, terminated) ?: UnknownError(message, terminated)
    }

    public class Success(
        terminated: Terminated,
        status: String,
    ) : ExitState.Success(terminated.pid, terminated.io, status)

    public sealed class Failure(
        terminated: Terminated,
        exitCode: Int = terminated.exitCode,
        state: String,
        status: String = terminated.status,
    ) : ExitState.Failure(exitCode, terminated.pid, io = terminated.io, status = "🐳 ${state.formattedAs.warning}") {

        public sealed class BadRequest(
            terminated: Terminated,
            public val statusCode: Int,
            state: String,
            status: String,
        ) : DockerExitStateHandler.Failure(terminated, state = state, status = status) {

            public class NoSuchContainer(terminated: Terminated, state: String, status: String) : BadRequest(terminated, 404, state, status)

            // Error: No such image
            public class NoSuchImage(terminated: Terminated, state: String, status: String) : BadRequest(terminated, 404, state, status)
            public class PathDoesNotExistInsideTheContainer(terminated: Terminated, state: String, status: String) : BadRequest(terminated, 404, state, status)

            public class NameAlreadyInUse(terminated: Terminated, state: String, status: String) : BadRequest(terminated, 409, state, status)
            public class Conflict(terminated: Terminated, state: String, status: String) : BadRequest(terminated, 409, state, status)

            public companion object {
                // ThisIsAClassName -> This is a class name
                private inline val <reified T : KClass<*>> T.expectedErrorMessage
                    get() = simpleName?.splitPascalCase()?.joinToString(" ")?.capitalize()

                private inline fun <reified T : Any> String.matches() = equals(T::class.expectedErrorMessage, ignoreCase = true)

                public fun from(message: String, status: String, terminated: Terminated): BadRequest? = with(message) {
                    when {
                        matches<NoSuchContainer>() -> NoSuchContainer(terminated, message, status)
                        matches<NoSuchImage>() -> NoSuchImage(terminated, message, status)
                        matches<PathDoesNotExistInsideTheContainer>() -> PathDoesNotExistInsideTheContainer(terminated, message, status)
                        matches<NameAlreadyInUse>() -> NameAlreadyInUse(terminated, message, status)
                        matches<Conflict>() -> Conflict(terminated, message, status)
                        else -> null
                    }
                }
            }
        }

        public class UnknownError(
            errorMessage: String,
            terminated: Terminated,
        ) : DockerExitStateHandler.Failure(terminated, state = "Unknown error from Docker daemon", status = errorMessage.formattedAs.error)
    }

    public class ParseException(
        errorMessage: String,
        cause: Throwable? = null,
    ) : RuntimeException("Error parsing response from Docker daemon: ${errorMessage.formattedAs.error}", cause)
}
