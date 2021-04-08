package koodies.docker

import koodies.concurrent.process.IO
import koodies.concurrent.process.Process.ExitState
import koodies.concurrent.process.Process.ProcessState.Terminated
import koodies.concurrent.process.status
import koodies.docker.DockerExitStateHandler.Failure.BadRequest
import koodies.docker.DockerExitStateHandler.Failure.BadRequest.Conflict
import koodies.docker.DockerExitStateHandler.Failure.BadRequest.NameAlreadyInUse
import koodies.docker.DockerExitStateHandler.Failure.BadRequest.NoSuchContainer
import koodies.docker.DockerExitStateHandler.Failure.BadRequest.NoSuchImage
import koodies.docker.DockerExitStateHandler.Failure.BadRequest.PathDoesNotExistInsideTheContainer
import koodies.docker.DockerExitStateHandler.Failure.UnknownError
import koodies.docker.DockerExitStateHandler.ParseException
import koodies.test.testEach
import koodies.text.ansiRemoved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion.Builder
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import strikt.assertions.message
import kotlin.reflect.KClass

@Execution(SAME_THREAD)
class DockerExitStateHandlerTest {

    private fun getTerminated(errorMessage: String) = Terminated(12345L, 42, listOf(IO.ERR typed errorMessage))

    @TestFactory
    fun `should match error message`() = listOf(
        NoSuchContainer::class to "Error: No such container: abcd/defg",
        NoSuchImage::class to "Error: No such image: abcd/defg",
        PathDoesNotExistInsideTheContainer::class to "Error: Path does not exist inside the container: abcd/defg",
        NameAlreadyInUse::class to "Error: Name already in use: abcd/defg",
        Conflict::class to "Error: Conflict: abcd/defg",
    ).testEach { (clazz: KClass<out BadRequest>, errorMessage) ->
        expect { DockerExitStateHandler.handle(getTerminated(errorMessage)) }.that {
            get { this::class }.isEqualTo(clazz)
        }
        expect { DockerExitStateHandler.handle(getTerminated(errorMessage)) }.that {
            get { status }.isEqualTo("abcd/defg")
        }
    }

    @Test
    fun `should return unknown state for unknown error`() {
        expectThat(DockerExitStateHandler.handle(getTerminated("Error: Nothing I know of: status")))
            .isA<UnknownError>().and {
                status.ansiRemoved.isEqualTo("Unknown error from Docker daemon: Nothing I know of: status")
            }
    }

    @Test
    fun `should throw on error-like message without error prefix`() {
        expectCatching { DockerExitStateHandler.handle(getTerminated("No Error: No such container: abcd/defg")) }.isFailure().isA<ParseException>().and {
            message.isNotNull().ansiRemoved.isEqualTo("Error parsing response from Docker daemon: No Error: No such container: abcd/defg")
        }
    }

    @Test
    fun `should throw if exception is caught`() {
        expectCatching { DockerExitStateHandler.handle(getTerminated("Not the typical error")) }.isFailure().isA<ParseException>().and {
            message.isNotNull().ansiRemoved.isEqualTo("Error parsing response from Docker daemon: Not the typical error")
        }
    }
}

public inline fun Builder<ExitState>.isSuccessful(): Builder<ExitState> =
    assert("exit state represents success") { actual ->
        when (actual.successful) {
            true -> pass(actual.successful)
            null -> fail("process did not terminate,yet")
            false -> fail("process failed: $actual")
        }
    }
