package com.bkahlert.kommons.docker

import com.bkahlert.kommons.collections.head
import com.bkahlert.kommons.collections.tail
import com.bkahlert.kommons.debug.asEmoji
import com.bkahlert.kommons.docker.DockerExitStateHandler.Failed.BadRequest
import com.bkahlert.kommons.docker.DockerExitStateHandler.Failed.ConnectivityProblem
import com.bkahlert.kommons.docker.DockerExitStateHandler.Failed.UnknownError
import com.bkahlert.kommons.exec.Exec
import com.bkahlert.kommons.exec.IO
import com.bkahlert.kommons.exec.IOSequence
import com.bkahlert.kommons.exec.Process.ExitState
import com.bkahlert.kommons.exec.Process.ExitState.ExitStateHandler
import com.bkahlert.kommons.exec.Process.State.Exited
import com.bkahlert.kommons.exec.Process.State.Exited.Succeeded
import com.bkahlert.kommons.exec.error
import com.bkahlert.kommons.exec.output
import com.bkahlert.kommons.exec.outputAndError
import com.bkahlert.kommons.lowerSentenceCaseName
import com.bkahlert.kommons.text.Semantics
import com.bkahlert.kommons.text.Semantics.Symbols
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.text.capitalize
import com.bkahlert.kommons.text.containsAll
import com.bkahlert.kommons.text.rightSpaced
import com.bkahlert.kommons.text.spaced
import com.bkahlert.kommons.text.splitPascalCase
import com.bkahlert.kommons.takeUnlessBlank
import com.bkahlert.kommons.time.Now
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.text.RegexOption.IGNORE_CASE

/**
 * Exit state handler that handles Docker processes.
 */
public object DockerExitStateHandler : ExitStateHandler {

    override fun Exec.handle(pid: Long, exitCode: Int, io: IOSequence<IO>): ExitState = kotlin.runCatching {
        if (exitCode == 0) handleSuccess(start, Now.instant, pid, io)
        else handleFailure(start, Now.instant, pid, exitCode, io)
    }.getOrElse { cause -> throw ParseException(io.outputAndError.ansiKept, cause) }

    private fun handleSuccess(start: Instant, end: Instant, pid: Long, io: IOSequence<IO>) = Succeeded(start, end, pid, io, "🐳 💭 ${true.asEmoji}")

    private val messageSplitRegex = Regex(": ")
    private val errorPrologue = Regex("Error(?:\\s+.*?\\s+daemon)?", IGNORE_CASE)

    private fun handleFailure(
        start: Instant,
        end: Instant,
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
            return ConnectivityProblem(start, end, pid, io, exitCode, errorMessage.removeSuffix("."))
        }

        val (error, message) = errorMessage.split(messageSplitRegex, limit = 2)

        if (!errorPrologue.matches(error)) throw ParseException(errorMessage)

        val parts = message.split(messageSplitRegex, limit = 2)
        return when (parts.size) {
            2 -> BadRequest.from(start, end, pid, exitCode, io, parts[1], parts[0])
            1 -> BadRequest.from(start, end, pid, exitCode, io, message)
            else -> null
        } ?: UnknownError(start, end, pid, exitCode, io, message)
    }

    public sealed class Failed(
        start: Instant,
        end: Instant,
        pid: Long,
        exitCode: Int,
        io: IOSequence<IO>,
        state: String?,
        status: String,
    ) : Exited.Failed(
        start,
        end,
        pid,
        exitCode,
        io = io,
        status = listOfNotNull(state, status).let { messageParts ->
            messageParts.head.formattedAs.error + messageParts.tail.joinToString("") { ": $it" }
        }) {

        override val successful: Boolean = false

        public class ConnectivityProblem(
            start: Instant,
            end: Instant,
            pid: Long,
            io: IOSequence<IO>,
            exitCode: Int,
            errorMessage: String,
        ) : DockerExitStateHandler.Failed(start, end, pid, exitCode, io, null, errorMessage.substringAfterLast(".").trim().formattedAs.warning) {
            override val symbol: String = Symbols.Negative
            override val textRepresentation: String get() = this::class.lowerSentenceCaseName.formattedAs.error + Semantics.FieldDelimiters.FIELD.spaced + status
            override fun format(): String = textRepresentation
            override fun toString(): String = textRepresentation
        }

        public sealed class BadRequest(
            start: Instant,
            end: Instant,
            pid: Long,
            exitCode: Int,
            io: IOSequence<IO>,
            public val statusCode: Int,
            status: String,
        ) : DockerExitStateHandler.Failed(start, end, pid, exitCode, io, null, status) {

            override val symbol: String = Symbols.Negative
            override val textRepresentation: String? get() = this::class.lowerSentenceCaseName.formattedAs.error
            override fun format(): String = symbol.rightSpaced + textRepresentation
            override fun toString(): String = format()

            public class NoSuchContainer(
                start: Instant,
                end: Instant,
                pid: Long,
                exitCode: Int,
                io: IOSequence<IO>,
                status: String,
            ) : BadRequest(start, end, pid, exitCode, io, 404, status)

            // Error: No such image
            public class NoSuchImage(
                start: Instant,
                end: Instant,
                pid: Long,
                exitCode: Int,
                io: IOSequence<IO>,
                status: String,
            ) : BadRequest(start, end, pid, exitCode, io, 404, status)

            public class PathDoesNotExistInsideTheContainer(
                start: Instant,
                end: Instant,
                pid: Long,
                exitCode: Int,
                io: IOSequence<IO>,
                status: String,
            ) :
                BadRequest(start, end, pid, exitCode, io, 404, status)

            public class NameAlreadyInUse(
                start: Instant,
                end: Instant,
                pid: Long,
                exitCode: Int,
                io: IOSequence<IO>,
                status: String,
            ) : BadRequest(start, end, pid, exitCode, io, 409, status)

            public open class Conflict(
                start: Instant,
                end: Instant,
                pid: Long,
                exitCode: Int,
                io: IOSequence<IO>,
                status: String,
            ) : BadRequest(start, end, pid, exitCode, io, 409, status)

            public class CannotRemoveRunningContainer(
                start: Instant,
                end: Instant,
                pid: Long,
                exitCode: Int,
                io: IOSequence<IO>,
                status: String,
            ) : BadRequest(start, end, pid, exitCode, io, 409, status = status) {
                override val textRepresentation: String get() = status.formattedAs.error
            }

            public class CannotKillContainer(
                start: Instant,
                end: Instant,
                pid: Long,
                exitCode: Int,
                io: IOSequence<IO>,
                private val wrapped: DockerExitStateHandler.Failed,
            ) : BadRequest(start, end, pid, exitCode, io, (wrapped as? BadRequest)?.statusCode ?: 500, wrapped.status) {
                override val textRepresentation: String? get() = wrapped.textRepresentation

                public companion object {
                    public fun parseWrapped(
                        start: Instant,
                        end: Instant,
                        pid: Long,
                        exitCode: Int,
                        io: IOSequence<IO>,
                        status: String,
                    ): DockerExitStateHandler.Failed {
                        val (affected, innerStatus) = status.split(messageSplitRegex, limit = 2)
                        return when {
                            innerStatus.containsAll("container", "not", "running", ignoreCase = true) -> object :
                                Conflict(start, end, pid, exitCode, io, affected) {
                                override val textRepresentation: String = "container is not running"
                            }

                            innerStatus.split(messageSplitRegex, limit = 2)
                                .let { it.size == 2 && it[0].containsAll("no", "such", "container", ignoreCase = true) } -> NoSuchContainer(
                                start,
                                end,
                                pid,
                                exitCode,
                                io,
                                innerStatus.split(messageSplitRegex, limit = 2)[1]
                            )

                            else -> UnknownError(start, end, pid, exitCode, io, status)
                        }
                    }
                }
            }

            public companion object {
                // ThisIsAClassName -> This is a class name
                protected inline val <reified T : KClass<*>> T.expectedErrorMessage: String?
                    get() = simpleName?.splitPascalCase()?.joinToString(" ")
                        ?.capitalize()

                private inline fun <reified T : Any> String.matches() = equals(T::class.expectedErrorMessage, ignoreCase = true)

                public fun from(
                    start: Instant,
                    end: Instant,
                    pid: Long,
                    exitCode: Int,
                    io: IOSequence<IO>,
                    status: String,
                    message: String,
                ): BadRequest? = with(message) {
                    when {
                        matches<NoSuchContainer>() -> NoSuchContainer(start, end, pid, exitCode, io, status)
                        matches<NoSuchImage>() -> NoSuchImage(start, end, pid, exitCode, io, status)
                        matches<PathDoesNotExistInsideTheContainer>() -> PathDoesNotExistInsideTheContainer(start, end, pid, exitCode, io, status)
                        matches<NameAlreadyInUse>() -> NameAlreadyInUse(start, end, pid, exitCode, io, status)
                        matches<Conflict>() -> Conflict(start, end, pid, exitCode, io, status)
                        matches<CannotKillContainer>() -> CannotKillContainer(
                            start,
                            end,
                            pid,
                            exitCode,
                            io,
                            CannotKillContainer.parseWrapped(start, end, pid, exitCode, io, status)
                        )
                        else -> null
                    }
                }

                public fun from(
                    start: Instant,
                    end: Instant,
                    pid: Long,
                    exitCode: Int,
                    io: IOSequence<IO>,
                    message: String,
                ): BadRequest? = when {
                    listOf("remove", "running container", "force").all { message.contains(it, ignoreCase = true) } -> {
                        CannotRemoveRunningContainer(
                            start, end, pid, exitCode, io,
                            "You cannot remove a running container. Stop the container before attempting removal or force remove."
                        )
                    }
                    else -> null
                }
            }
        }

        public class UnknownError(
            start: Instant,
            end: Instant,
            pid: Long,
            exitCode: Int,
            io: IOSequence<IO>,
            errorMessage: String,
        ) : DockerExitStateHandler.Failed(start, end, pid, exitCode, io, "Unknown error from Docker daemon", errorMessage.formattedAs.error) {
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
