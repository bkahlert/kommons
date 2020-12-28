package koodies.concurrent

import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.process
import koodies.test.UniqueId
import koodies.test.testWithTempDir
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.nio.file.Path

@Execution(CONCURRENT)
class ProcessesKtTest {

    private val echoingCommands =
        ">&1 echo \"test output\"; sleep 1; >&2 echo \"test error\"; sleep 1; >&1 echo \"test output\"; sleep 1; >&2 echo \"test error\""

    @Nested
    inner class ProcessFn {
        private val factories = listOf<Path.() -> ManagedProcess>(
            {
                process(CommandLine(
                    environment = emptyMap(),
                    workingDirectory = this,
                    command = "/bin/sh", "-c", echoingCommands))
            },
            {
                process("/bin/sh", "-c", echoingCommands)
            },
        )

        @TestFactory
        fun `should not start`(uniqueId: UniqueId) = factories.testWithTempDir(uniqueId) {
            val process = it()
            expectThat(process.started).isFalse()
        }

        @TestFactory
        fun `should start`(uniqueId: UniqueId) = factories.testWithTempDir(uniqueId) {
            val process = it()
            process.start()
            expectThat(process.started).isTrue()
        }

        @TestFactory
        fun `should process`(uniqueId: UniqueId) = factories.testWithTempDir(uniqueId) {
            val process = it()
            val processed = mutableListOf<IO>()
            process.process { io -> processed.add(io) }.waitForTermination()
            expectThat(processed).contains(
                IO.Type.OUT typed "test output",
                IO.Type.ERR typed "test error"
            )
        }
    }

    @Nested
    inner class OutputFn {

        @Test
        fun `should start process`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = process(CommandLine(
                environment = emptyMap(),
                workingDirectory = this,
                command = "/bin/sh", "-c", echoingCommands))
            process.output()

            expectThat(process.started).isTrue()
        }

        @Test
        fun `should run synchronously`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = process(CommandLine(
                environment = emptyMap(),
                workingDirectory = this,
                command = "/bin/sh", "-c", echoingCommands))
            process.output()

            expectThat(process.exitValue).isEqualTo(0)
        }

        @Test
        fun `should log IO`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = process(CommandLine(
                environment = emptyMap(),
                workingDirectory = this,
                command = "/bin/sh", "-c", echoingCommands))
            process.output()

            expectThat(process.ioLog.logged.drop(2))
                .containsExactlyInAnyOrder(
                    IO.Type.OUT typed "test output",
                    IO.Type.ERR typed "test error",
                    IO.Type.OUT typed "test output",
                    IO.Type.ERR typed "test error",
                )
        }

        @Test
        fun `should contain OUT`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val output = process(CommandLine(
                environment = emptyMap(),
                workingDirectory = this,
                command = "/bin/sh", "-c", echoingCommands)).output()

            expectThat(output).isEqualTo("""
                test output
                test output
            """.trimIndent())
        }
    }
}
