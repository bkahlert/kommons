package koodies.concurrent

import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Processors
import koodies.concurrent.process.containsDump
import koodies.concurrent.process.logged
import koodies.concurrent.process.process
import koodies.concurrent.process.processSynchronously
import koodies.test.UniqueId
import koodies.test.matchesCurlyPattern
import koodies.test.output.CapturedOutput
import koodies.test.output.OutputCaptureExtension
import koodies.test.testWithTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import java.nio.file.Path
import java.util.concurrent.CompletionException

@Execution(CONCURRENT)
class ProcessesKtTest {

    private val echoingCommands =
        ">&1 echo \"test output 1\"; sleep 1; >&2 echo \"test error 1\"; sleep 1; >&1 echo \"test output 2\"; sleep 1; >&2 echo \"test error 2\""

    private fun getFactories(command: String = echoingCommands) = listOf<Path.() -> ManagedProcess>(
        {
            process(CommandLine(
                environment = emptyMap(),
                workingDirectory = this,
                command = "/bin/sh", "-c", command))
        },
        {
            process("/bin/sh", "-c", command)
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
            val processed = mutableListOf<IO>()
            process.process { io -> processed.add(io) }.waitForTermination()
            expectThat(processed).contains(
                IO.Type.OUT typed "test output 1",
                IO.Type.ERR typed "test error 1",
                IO.Type.OUT typed "test output 2",
                IO.Type.ERR typed "test error 2",
            )
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

    @Nested
    inner class OutputFn {

        @TestFactory
        fun `should start process implicitly`(uniqueId: UniqueId) = getFactories().testWithTempDir(uniqueId) { processFactory ->
            val process = processFactory()
            process.output()

            expectThat(process.started).isTrue()
        }

        @TestFactory
        fun `should process the process synchronously`(uniqueId: UniqueId) = getFactories().testWithTempDir(uniqueId) { processFactory ->
            val process = processFactory()
            process.output()

            expectThat(process.exitValue).isEqualTo(0)
        }

        @TestFactory
        fun `should log IO`(uniqueId: UniqueId) = getFactories().testWithTempDir(uniqueId) { processFactory ->
            val process = processFactory()
            process.output()

            expectThat(process.ioLog.logged.drop(2).dropLast(1))
                .containsExactlyInAnyOrder(
                    IO.Type.OUT typed "test output 1",
                    IO.Type.ERR typed "test error 1",
                    IO.Type.OUT typed "test output 2",
                    IO.Type.ERR typed "test error 2",
                )
        }

        @TestFactory
        fun `should contain OUT`(uniqueId: UniqueId) = getFactories().testWithTempDir(uniqueId) { processFactory ->
            val output = processFactory().output()

            expectThat(output).isEqualTo("""
                test output 1
                test output 2
            """.trimIndent())
        }
    }

    @Nested
    @Execution(SAME_THREAD)
    @Isolated
    @ExtendWith(OutputCaptureExtension::class)
    inner class SynchronousExecution {

        @TestFactory
        fun `should process log to console by default`(output: CapturedOutput, uniqueId: UniqueId) =
            getFactories().testWithTempDir(uniqueId) { processFactory ->
                val process = processFactory()
                process.processSynchronously()
                expectThat(output).get { out }.matchesCurlyPattern("""
                ▶{}commandLine{}
                · test output 1
                · test output 2
                · Unfortunately an error occurred: test error 1
                · Unfortunately an error occurred: test error 2
            """.trimIndent())
                expectThat(output).get { err }.isEmpty()
            }

        @TestFactory
        fun `should process not log to console if specified`(output: CapturedOutput, uniqueId: UniqueId) =
            getFactories().testWithTempDir(uniqueId) { processFactory ->
                val process = processFactory()
                process.processSynchronously(Processors.noopProcessor())
                expectThat(output).get { out }.isEmpty()
                expectThat(output).get { err }.isEmpty()
            }

        @TestFactory
        fun `should format merged output`(output: CapturedOutput, uniqueId: UniqueId) =
            getFactories().testWithTempDir(uniqueId) { processFactory ->
                val process = processFactory()
                process.processSynchronously(Processors.noopProcessor())
                expectThat(process.logged).matchesCurlyPattern("""
                    Executing {}
                    {} file:{}
                    test output 1
                    test output 2
                    test error 1
                    test error 2
                    Process {} terminated successfully at {}.
                    """.trimIndent())
            }
    }
}
