package koodies.concurrent

import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.containsDump
import koodies.concurrent.process.process
import koodies.test.UniqueId
import koodies.test.testWithTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import java.nio.file.Path
import java.util.concurrent.CompletionException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Execution(CONCURRENT)
class ProcessesKtTest {

    private val echoingCommands =
        "echo \"test output ${'$'}TEST\"; sleep 1; >&2 echo \"test error 1\"; sleep 1; echo \"test output 2\"; >&2 echo \"test error 2\"; sleep 1"

    private fun getFactories(command: String = echoingCommands) = listOf<Path.() -> ManagedProcess>(
        {
            process(CommandLine(
                environment = mapOf("TEST" to "env"),
                workingDirectory = this,
                command = "/bin/sh", "-c", command))
        },
        {
            process("/bin/sh", "-c", command, environment = mapOf("TEST" to "env"))
        },
    )

    @Nested
    inner class ProcessFn {

        @TestFactory
        fun `should not start`(uniqueId: UniqueId) = getFactories().testWithTempDir(uniqueId) { processFactory ->
            val process = processFactory()
            expectThat(process.started).isFalse()
        }

        @TestFactory
        fun `should start`(uniqueId: UniqueId) = getFactories().testWithTempDir(uniqueId) { processFactory ->
            val process = processFactory()
            process.start()
            expectThat(process.started).isTrue()
        }

        @TestFactory
        fun `should process`(uniqueId: UniqueId) = getFactories().testWithTempDir(uniqueId) { processFactory ->
            val process = processFactory()
            val lock = ReentrantLock()
            val processed = mutableListOf<IO>()
            process.process { io -> lock.withLock { processed.add(io) } }.waitForTermination()

            lock.withLock {
                expectThat(processed).contains(
                    IO.OUT typed "test output env",
                    IO.ERR typed "test error 1",
                    IO.OUT typed "test output 2",
                    IO.ERR typed "test error 2",
                )
            }
        }

        @TestFactory
        fun `should not throw on unexpected exit value`(uniqueId: UniqueId) = getFactories("exit 42").testWithTempDir(uniqueId) { processFactory ->
            val process = processFactory()
            process.start()
            expectThat(process.started).isTrue()
        }

        @TestFactory
        fun `should throw on unexpected exit value if using joining function`(uniqueId: UniqueId) =
            getFactories("exit 42").testWithTempDir(uniqueId) { processFactory ->
                val process = processFactory().start()
                expectCatching { process.waitForTermination() }
                    .isFailure()
                    .isA<CompletionException>()
                    .with({ message }) { isNotNull() and { containsDump() } }
            }
    }
}
