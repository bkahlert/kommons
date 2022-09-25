package com.bkahlert.kommons_deprecated.docker

import com.bkahlert.kommons.ansiRemoved
import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons.text.endSpaced
import com.bkahlert.kommons.text.spaced
import com.bkahlert.kommons_deprecated.docker.DockerExitStateHandler.Failed.BadRequest
import com.bkahlert.kommons_deprecated.docker.DockerExitStateHandler.Failed.BadRequest.CannotKillContainer
import com.bkahlert.kommons_deprecated.docker.DockerExitStateHandler.Failed.BadRequest.CannotRemoveRunningContainer
import com.bkahlert.kommons_deprecated.docker.DockerExitStateHandler.Failed.BadRequest.Conflict
import com.bkahlert.kommons_deprecated.docker.DockerExitStateHandler.Failed.BadRequest.NameAlreadyInUse
import com.bkahlert.kommons_deprecated.docker.DockerExitStateHandler.Failed.BadRequest.NoSuchContainer
import com.bkahlert.kommons_deprecated.docker.DockerExitStateHandler.Failed.BadRequest.NoSuchImage
import com.bkahlert.kommons_deprecated.docker.DockerExitStateHandler.Failed.BadRequest.PathDoesNotExistInsideTheContainer
import com.bkahlert.kommons_deprecated.docker.DockerExitStateHandler.Failed.ConnectivityProblem
import com.bkahlert.kommons_deprecated.docker.DockerExitStateHandler.Failed.UnknownError
import com.bkahlert.kommons_deprecated.docker.DockerExitStateHandler.ParseException
import com.bkahlert.kommons_deprecated.exec.IO
import com.bkahlert.kommons_deprecated.exec.IO.Error
import com.bkahlert.kommons_deprecated.exec.IO.Output
import com.bkahlert.kommons_deprecated.exec.IOSequence
import com.bkahlert.kommons_deprecated.exec.Process.ExitState
import com.bkahlert.kommons_deprecated.exec.mock.ExecMock
import com.bkahlert.kommons_deprecated.exec.status
import com.bkahlert.kommons_deprecated.test.testEachOld
import com.bkahlert.kommons_deprecated.test.testOld
import com.bkahlert.kommons_deprecated.text.Semantics
import com.bkahlert.kommons_deprecated.text.Semantics.Symbols.Negative
import com.bkahlert.kommons_deprecated.text.Semantics.formattedAs
import com.bkahlert.kommons_deprecated.text.removeAnsi
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
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

    @Test
    fun `should match docker engine not running error message`() = testAll {
        val connectivityProblemState = handleTermination("Cannot connect to the Docker daemon at unix:///var/run/docker.sock. Is the docker daemon running?")

        val delimiter = Semantics.FieldDelimiters.FIELD.spaced.ansiRemoved

        connectivityProblemState.shouldBeInstanceOf<ConnectivityProblem>()
        connectivityProblemState.status.ansiRemoved shouldBe "Is the docker daemon running?"
        connectivityProblemState.textRepresentation.ansiRemoved shouldBe "connectivity problem${delimiter}Is the docker daemon running?"
        connectivityProblemState.format().ansiRemoved shouldBe "connectivity problem${delimiter}Is the docker daemon running?"
        connectivityProblemState.toString().ansiRemoved shouldMatchGlob "connectivity problem${delimiter}Is the docker daemon running?"
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

        exitState.toString().ansiRemoved shouldMatchGlob "connectivity problem${delimiter}Is the docker daemon running?"
    }

    @TestFactory
    fun `should match bad request error message`() = listOf(
        Triple(NoSuchContainer::class, "Error: No such container: AFFECTED", "no such container"),
        Triple(NoSuchImage::class, "Error: No such image: AFFECTED", "no such image"),
        Triple(
            PathDoesNotExistInsideTheContainer::class,
            "Error: Path does not exist inside the container: AFFECTED",
            "path does not exist inside the container"
        ),
        Triple(NameAlreadyInUse::class, "Error: Name already in use: AFFECTED", "name already in use"),
        Triple(Conflict::class, "Error: Conflict: AFFECTED", "conflict"),
    ).testEachOld { (clazz: KClass<out BadRequest>, errorMessage: String, status: String) ->
        val badRequestState = handleTermination(errorMessage)

        expecting("matches ${clazz.simpleName}") { badRequestState::class } that { isEqualTo(clazz) }
        expecting("status is AFFECTED") { badRequestState.status.ansiRemoved } that { isEqualTo("AFFECTED") }
        expecting("formatted state ${badRequestState.format()}") { badRequestState.format().ansiRemoved } that { isEqualTo("${Negative.ansiRemoved} $status") }
        expecting("toString() is ${toString()}") { badRequestState } that { get { toString().ansiRemoved shouldMatchGlob "${Negative.ansiRemoved} ${status.formattedAs.error.ansiRemoved}" } }
    }

    @TestFactory
    fun `should match cannot remove running container error messages`() = testOld(
        "Error response from daemon: You cannot remove a running container 2c5e082a462134. " +
            "Stop the container before attempting removal or force remove"
    ) { errorMessage ->

        val badRequestState = handleTermination(errorMessage)

        val status = "You cannot remove a running container. Stop the container before attempting removal or force remove."
        expecting("matches ${CannotRemoveRunningContainer::class.simpleName}") { badRequestState::class } that { isEqualTo(CannotRemoveRunningContainer::class) }
        expecting("status is AFFECTED") { badRequestState.status.ansiRemoved } that { isEqualTo(status) }
        expecting("formatted state ${badRequestState.format()}") { badRequestState.format().ansiRemoved } that { isEqualTo("${Negative.endSpaced.ansiRemoved}$status") }
        expecting("toString() is ${toString()}") { badRequestState } that { get { toString().ansiRemoved shouldMatchGlob "${Negative.ansiRemoved} $status" } }
    }

    @TestFactory
    fun `should match cannot kill container`() = testEachOld(
        "Error response from daemon: Cannot kill container: AFFECTED: No such container: AFFECTED" to "no such container",
        "Error response from daemon: Cannot kill container: AFFECTED: Container AFFECTED is not running" to "container is not running",
    ) { (errorMessage, status) ->

        val badRequestState = handleTermination(errorMessage)

        expecting("matches ${CannotKillContainer::class.simpleName}") { CannotKillContainer::class } that { isEqualTo(CannotKillContainer::class) }
        expecting("status is AFFECTED") { badRequestState.status.ansiRemoved } that { isEqualTo("AFFECTED") }
        expecting("formatted state ${badRequestState.format()}") { badRequestState.format().ansiRemoved } that { isEqualTo("${Negative.ansiRemoved} $status") }
        expecting("toString() is ${toString()}") { badRequestState } that { get { toString().ansiRemoved shouldMatchGlob "${Negative.ansiRemoved} ${status.formattedAs.error.ansiRemoved}" } }
    }

    @Test
    fun `should return unknown state for unknown error`() {
        val unknownError = handleTermination("Error: Nothing I know of: status")
        expectThat(unknownError).isA<UnknownError>().and {
            status.removeAnsi.isEqualTo("Unknown error from Docker daemon: Nothing I know of: status")
            get { format() }.removeAnsi.isEqualTo("ϟ Unknown error from Docker daemon: Nothing I know of: status")
            get { toString().ansiRemoved shouldMatchGlob "Unknown error from Docker daemon: Nothing I know of: status" }
        }
    }

    @Test
    fun `should throw on error-like message without error prefix`() {
        expectCatching { handleTermination("No Error: No such container: AFFECTED") }.isFailure().isA<ParseException>().and {
            message.isNotNull().removeAnsi.isEqualTo("Error parsing response from Docker daemon: No Error: No such container: AFFECTED")
        }
    }

    @Test
    fun `should throw if exception is caught`() {
        expectCatching { handleTermination("Not the typical error") }.isFailure().isA<ParseException>().and {
            message.isNotNull().removeAnsi.isEqualTo("Error parsing response from Docker daemon: Not the typical error")
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
