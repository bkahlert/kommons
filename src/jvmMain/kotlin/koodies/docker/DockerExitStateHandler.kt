package koodies.docker

import koodies.collections.head
import koodies.collections.tail
import koodies.debug.asEmoji
import koodies.docker.DockerExitStateHandler.Failed.BadRequest
import koodies.docker.DockerExitStateHandler.Failed.ConnectivityProblem
import koodies.docker.DockerExitStateHandler.Failed.UnknownError
import koodies.exec.Exec
import koodies.exec.IO
import koodies.exec.IOSequence
import koodies.exec.Process.ExitState
import koodies.exec.Process.ExitState.ExitStateHandler
import koodies.exec.Process.State.Exited
import koodies.exec.Process.State.Exited.Succeeded
import koodies.exec.error
import koodies.exec.output
import koodies.exec.outputAndError
import koodies.lowerSentenceCaseName
import koodies.text.ANSI.ansiRemoved
import koodies.text.Semantics
import koodies.text.Semantics.Symbols
import koodies.text.Semantics.formattedAs
import koodies.text.containsAll
import koodies.text.rightSpaced
import koodies.text.spaced
import koodies.text.splitPascalCase
import koodies.text.takeUnlessBlank
import koodies.text.withoutSuffix
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.text.RegexOption.IGNORE_CASE

/**
 * Exit state handler that handles Docker processes.
 */
public object DockerExitStateHandler : ExitStateHandler {

    override fun Exec.handle(pid: Long, exitCode: Int, io: IOSequence<IO>): ExitState = kotlin.runCatching {
        if (exitCode == 0) handleSuccess(pid, io)
        else handleFailure(pid, exitCode, io)
    }.getOrElse { cause -> throw ParseException(io.outputAndError.ansiKept, cause) }

    private fun handleSuccess(pid: Long, io: IOSequence<IO>) = Succeeded(pid, io, "🐳 💭 ${true.asEmoji}")

    private val messageSplitRegex = Regex(": ")
    private val errorPrologue = Regex("Error(?:\\s+.*?\\s+daemon)?", IGNORE_CASE)

    private fun handleFailure(
        pid: Long,
        exitCode: Int,
        io: IOSequence<IO>,
    ): Failed {
        val errorMessage = io.run {
            output.lastOrNull()
                ?.takeIf { it.startsWith("error:", ignoreCase = true) }
                ?: error.first()
        }.ansiRemoved

        if (errorMessage.containsAll("connect", "Docker", "daemon", ignoreCase = true)) {
            return ConnectivityProblem(pid, io, exitCode, errorMessage.withoutSuffix("."))
        }

        val (error, message) = errorMessage.split(messageSplitRegex, limit = 2)

        if (!errorPrologue.matches(error)) throw ParseException(errorMessage)

        val parts = message.split(messageSplitRegex, limit = 2)
        return when (parts.size) {
            2 -> BadRequest.from(pid, exitCode, io, parts[1], parts[0])
            1 -> BadRequest.from(pid, exitCode, io, message)
            else -> null
        } ?: UnknownError(pid, exitCode, io, message)
    }

    public sealed class Failed(
        pid: Long,
        exitCode: Int,
        io: IOSequence<IO>,
        state: String?,
        status: String,
    ) : Exited.Failed(pid,
        exitCode,
        io = io,
        status = listOfNotNull(state, status).let { messageParts ->
            messageParts.head.formattedAs.error + messageParts.tail.joinToString("") { ": $it" }
        }) {

        override val successful: Boolean = false

        public class ConnectivityProblem(
            pid: Long,
            io: IOSequence<IO>,
            exitCode: Int,
            errorMessage: String,
        ) : DockerExitStateHandler.Failed(pid, exitCode, io, null, errorMessage.substringAfterLast(".").trim().formattedAs.warning) {
            override val symbol: String = Symbols.Negative
            override val textRepresentation: String get() = this::class.lowerSentenceCaseName.formattedAs.error + Semantics.FieldDelimiters.FIELD.spaced + status
            override fun format(): String = textRepresentation
            override fun toString(): String = textRepresentation
        }

        public sealed class BadRequest(
            pid: Long,
            exitCode: Int,
            io: IOSequence<IO>,
            public val statusCode: Int,
            status: String,
        ) : DockerExitStateHandler.Failed(pid, exitCode, io, null, status) {

            override val symbol: String = Symbols.Negative
            override val textRepresentation: String? get() = this::class.lowerSentenceCaseName.formattedAs.error
            override fun format(): String = textRepresentation + symbol.spaced + status
            override fun toString(): String = symbol.rightSpaced + textRepresentation

            public class NoSuchContainer(pid: Long, exitCode: Int, io: IOSequence<IO>, status: String) : BadRequest(pid, exitCode, io, 404, status)

            // Error: No such image
            public class NoSuchImage(pid: Long, exitCode: Int, io: IOSequence<IO>, status: String) : BadRequest(pid, exitCode, io, 404, status)

            public class PathDoesNotExistInsideTheContainer(pid: Long, exitCode: Int, io: IOSequence<IO>, status: String) :
                BadRequest(pid, exitCode, io, 404, status)

            public class NameAlreadyInUse(pid: Long, exitCode: Int, io: IOSequence<IO>, status: String) : BadRequest(pid, exitCode, io, 409, status)

            public open class Conflict(pid: Long, exitCode: Int, io: IOSequence<IO>, status: String) : BadRequest(pid, exitCode, io, 409, status)

            public class CannotRemoveRunningContainer(pid: Long, exitCode: Int, io: IOSequence<IO>, status: String) :
                BadRequest(pid, exitCode, io, 409, status = status) {
                override val textRepresentation: String get() = status.formattedAs.error
                override fun format(): String = symbol.rightSpaced + textRepresentation
                override fun toString(): String = format()
            }

            public class CannotKillContainer(pid: Long, exitCode: Int, io: IOSequence<IO>, private val wrapped: koodies.docker.DockerExitStateHandler.Failed) :
                BadRequest(pid, exitCode, io, (wrapped as? BadRequest)?.statusCode ?: 500, wrapped.status) {
                override val textRepresentation: String? get() = wrapped.textRepresentation

                public companion object {
                    public fun parseWrapped(pid: Long, exitCode: Int, io: IOSequence<IO>, status: String): koodies.docker.DockerExitStateHandler.Failed {
                        val (affected, innerStatus) = status.split(messageSplitRegex, limit = 2)
                        return when {
                            innerStatus.containsAll("container", "not", "running", ignoreCase = true) -> object : Conflict(pid, exitCode, io, affected) {
                                override val textRepresentation: String = "container is not running"
                            }

                            innerStatus.split(messageSplitRegex, limit = 2)
                                .let { it.size == 2 && it[0].containsAll("no", "such", "container", ignoreCase = true) } -> NoSuchContainer(pid, exitCode, io,
                                innerStatus.split(messageSplitRegex, limit = 2)[1])

                            else -> UnknownError(pid, exitCode, io, status)
                        }
                    }
                }
            }

            public companion object {
                // ThisIsAClassName -> This is a class name
                protected inline val <reified T : KClass<*>> T.expectedErrorMessage: String?
                    get() = simpleName?.splitPascalCase()?.joinToString(" ")
                        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                private inline fun <reified T : Any> String.matches() = equals(T::class.expectedErrorMessage, ignoreCase = true)

                public fun from(pid: Long, exitCode: Int, io: IOSequence<IO>, status: String, message: String): BadRequest? = with(message) {
                    when {
                        matches<NoSuchContainer>() -> NoSuchContainer(pid, exitCode, io, status)
                        matches<NoSuchImage>() -> NoSuchImage(pid, exitCode, io, status)
                        matches<PathDoesNotExistInsideTheContainer>() -> PathDoesNotExistInsideTheContainer(pid, exitCode, io, status)
                        matches<NameAlreadyInUse>() -> NameAlreadyInUse(pid, exitCode, io, status)
                        matches<Conflict>() -> Conflict(pid, exitCode, io, status)
                        matches<CannotKillContainer>() -> CannotKillContainer(pid, exitCode, io, CannotKillContainer.parseWrapped(pid, exitCode, io, status))
                        else -> null
                    }
                }

                public fun from(pid: Long, exitCode: Int, io: IOSequence<IO>, message: String): BadRequest? = when {
                    listOf("remove", "running container", "force").all { message.contains(it, ignoreCase = true) } -> {
                        CannotRemoveRunningContainer(pid, exitCode, io,
                            "You cannot remove a running container. Stop the container before attempting removal or force remove.")
                    }
                    else -> null
                }
            }
        }

        public class UnknownError(pid: Long, exitCode: Int, io: IOSequence<IO>, errorMessage: String) :
            DockerExitStateHandler.Failed(pid, exitCode, io, "Unknown error from Docker daemon", errorMessage.formattedAs.error) {
            override val symbol: String = Symbols.Error
            override fun format(): String = textRepresentation?.takeUnlessBlank()?.let { "$symbol $it" } ?: symbol
            override fun toString(): String = status
        }
    }

    public class ParseException(
        errorMessage: String,
        cause: Throwable? = null,
    ) : RuntimeException("Error parsing response from Docker daemon: ${errorMessage.formattedAs.error}", cause)
}
