package com.bkahlert.kommons.docker

import com.bkahlert.kommons.collections.too
import com.bkahlert.kommons.docker.DockerExitStateHandler.Failed.BadRequest
import com.bkahlert.kommons.docker.DockerExitStateHandler.Failed.BadRequest.CannotKillContainer
import com.bkahlert.kommons.docker.DockerExitStateHandler.Failed.BadRequest.CannotRemoveRunningContainer
import com.bkahlert.kommons.docker.DockerExitStateHandler.Failed.BadRequest.Conflict
import com.bkahlert.kommons.docker.DockerExitStateHandler.Failed.BadRequest.NameAlreadyInUse
import com.bkahlert.kommons.docker.DockerExitStateHandler.Failed.BadRequest.NoSuchContainer
import com.bkahlert.kommons.docker.DockerExitStateHandler.Failed.BadRequest.NoSuchImage
import com.bkahlert.kommons.docker.DockerExitStateHandler.Failed.BadRequest.PathDoesNotExistInsideTheContainer
import com.bkahlert.kommons.docker.DockerExitStateHandler.Failed.ConnectivityProblem
import com.bkahlert.kommons.docker.DockerExitStateHandler.Failed.UnknownError
import com.bkahlert.kommons.docker.DockerExitStateHandler.ParseException
import com.bkahlert.kommons.exec.IO
import com.bkahlert.kommons.exec.IO.Error
import com.bkahlert.kommons.exec.IO.Output
import com.bkahlert.kommons.exec.IOSequence
import com.bkahlert.kommons.exec.Process.ExitState
import com.bkahlert.kommons.exec.mock.ExecMock
import com.bkahlert.kommons.exec.status
import com.bkahlert.kommons.test.test
import com.bkahlert.kommons.test.testEach
import com.bkahlert.kommons.test.tests
import com.bkahlert.kommons.text.ANSI.ansiRemoved
import com.bkahlert.kommons.text.Semantics
import com.bkahlert.kommons.text.Semantics.Symbols.Negative
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.text.ansiRemoved
import com.bkahlert.kommons.text.rightSpaced
import com.bkahlert.kommons.text.spaced
import com.bkahlert.kommons.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion.Builder
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import strikt.assertions.message
import kotlin.reflect.KClass

class DockerExitStateHandlerTest {

    private val exec = ExecMock.FAILED_EXEC
    private fun handleTermination(errorMessage: String) = with(DockerExitStateHandler) { exec.handle(12345L, 42, IOSequence(Error typed errorMessage)) }
    private fun handleTermination(vararg messages: IO) = with(DockerExitStateHandler) { exec.handle(12345L, 42, IOSequence(*messages)) }

    @TestFactory
    fun `should match docker engine not running error message`() = tests {
        val connectivityProblemState = handleTermination("Cannot connect to the Docker daemon at unix:///var/run/docker.sock. Is the docker daemon running?")

        val delimiter = Semantics.FieldDelimiters.FIELD.spaced.ansiRemoved

        expecting { connectivityProblemState } that { isA<ConnectivityProblem>() }
        expecting { connectivityProblemState.status.ansiRemoved } that { isEqualTo("Is the docker daemon running?") }
        expecting { connectivityProblemState.textRepresentation!!.ansiRemoved } that { isEqualTo("connectivity problem${delimiter}Is the docker daemon running?") }
        expecting { connectivityProblemState.format().ansiRemoved } that { isEqualTo("connectivity problem${delimiter}Is the docker daemon running?") }
        expecting { connectivityProblemState } that { toStringMatchesCurlyPattern("connectivity problem${delimiter}Is the docker daemon running?") }
    }

    @Test
    fun `should match out errors`() {
        val errorMessage = "Cannot connect to the Docker daemon at unix:///var/run/docker.sock. Is the docker daemon running?"
        val exitState = handleTermination(
            Output typed "out",
            Output typed "error: $errorMessage",
            Error typed "error: err",
        )

        val delimiter = Semantics.FieldDelimiters.FIELD.spaced.ansiRemoved

        expectThat(exitState).toStringMatchesCurlyPattern("connectivity problem${delimiter}Is the docker daemon running?")
    }

    @TestFactory
    fun `should match bad request error message`() = listOf(
        NoSuchContainer::class to "Error: No such container: AFFECTED" too "no such container",
        NoSuchImage::class to "Error: No such image: AFFECTED" too "no such image",
        PathDoesNotExistInsideTheContainer::class to "Error: Path does not exist inside the container: AFFECTED" too "path does not exist inside the container",
        NameAlreadyInUse::class to "Error: Name already in use: AFFECTED" too "name already in use",
        Conflict::class to "Error: Conflict: AFFECTED" too "conflict"
    ).testEach { (clazz: KClass<out BadRequest>, errorMessage: String, status: String) ->
        val badRequestState = handleTermination(errorMessage)

        expecting("matches ${clazz.simpleName}") { badRequestState::class } that { isEqualTo(clazz) }
        expecting("status is AFFECTED") { badRequestState.status.ansiRemoved } that { isEqualTo("AFFECTED") }
        expecting("formatted state ${badRequestState.format()}") { badRequestState.format().ansiRemoved } that { isEqualTo("${Negative.ansiRemoved} $status") }
        expecting("toString() is ${toString()}") { badRequestState } that { toStringMatchesCurlyPattern("${Negative.ansiRemoved} ${status.formattedAs.error}") }
    }

    @TestFactory
    fun `should match cannot remove running container error messages`() = test(
        "Error response from daemon: You cannot remove a running container 2c5e082a462134. " +
            "Stop the container before attempting removal or force remove") { errorMessage ->

        val badRequestState = handleTermination(errorMessage)

        val status = "You cannot remove a running container. Stop the container before attempting removal or force remove."
        expecting("matches ${CannotRemoveRunningContainer::class.simpleName}") { badRequestState::class } that { isEqualTo(CannotRemoveRunningContainer::class) }
        expecting("status is AFFECTED") { badRequestState.status.ansiRemoved } that { isEqualTo(status) }
        expecting("formatted state ${badRequestState.format()}") { badRequestState.format().ansiRemoved } that { isEqualTo("${Negative.rightSpaced.ansiRemoved}$status") }
        expecting("toString() is ${toString()}") { badRequestState } that { toStringMatchesCurlyPattern("${Negative.ansiRemoved} $status") }
    }

    @TestFactory
    fun `should match cannot kill container`() = testEach(
        "Error response from daemon: Cannot kill container: AFFECTED: No such container: AFFECTED" to "no such container",
        "Error response from daemon: Cannot kill container: AFFECTED: Container AFFECTED is not running" to "container is not running",
    ) { (errorMessage, status) ->

        val badRequestState = handleTermination(errorMessage)

        expecting("matches ${CannotKillContainer::class.simpleName}") { CannotKillContainer::class } that { isEqualTo(CannotKillContainer::class) }
        expecting("status is AFFECTED") { badRequestState.status.ansiRemoved } that { isEqualTo("AFFECTED") }
        expecting("formatted state ${badRequestState.format()}") { badRequestState.format().ansiRemoved } that { isEqualTo("${Negative.ansiRemoved} $status") }
        expecting("toString() is ${toString()}") { badRequestState } that { toStringMatchesCurlyPattern("${Negative.ansiRemoved} ${status.formattedAs.error}") }
    }

    @Test
    fun `should return unknown state for unknown error`() {
        val unknownError = handleTermination("Error: Nothing I know of: status")
        expectThat(unknownError).isA<UnknownError>().and {
            status.ansiRemoved.isEqualTo("Unknown error from Docker daemon: Nothing I know of: status")
            get { format() }.ansiRemoved.isEqualTo("ϟ Unknown error from Docker daemon: Nothing I know of: status")
            toStringMatchesCurlyPattern("Unknown error from Docker daemon: Nothing I know of: status")
        }
    }

    @Test
    fun `should throw on error-like message without error prefix`() {
        expectCatching { handleTermination("No Error: No such container: AFFECTED") }.isFailure().isA<ParseException>().and {
            message.isNotNull().ansiRemoved.isEqualTo("Error parsing response from Docker daemon: No Error: No such container: AFFECTED")
        }
    }

    @Test
    fun `should throw if exception is caught`() {
        expectCatching { handleTermination("Not the typical error") }.isFailure().isA<ParseException>().and {
            message.isNotNull().ansiRemoved.isEqualTo("Error parsing response from Docker daemon: Not the typical error")
        }
    }
}

fun Builder<ExitState>.isSuccessful(): Builder<ExitState> =
    assert("exit state represents success") { actual ->
        when (actual.successful) {
            true -> pass(actual.successful)
            false -> fail("process failed: $actual")
        }
    }


fun Builder<ExitState>.isFailed(): Builder<ExitState> =
    assert("exit state represents failed") { actual ->
        when (actual.successful) {
            true -> fail("process did not fail: $actual")
            false -> pass(actual.successful)
        }
    }