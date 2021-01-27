package koodies.concurrent

import koodies.concurrent.process.IO
import koodies.concurrent.process.IO.Type.ERR
import koodies.concurrent.process.IO.Type.OUT
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Processor
import koodies.concurrent.process.containsDump
import koodies.concurrent.process.processSynchronously
import koodies.logging.InMemoryLogger
import koodies.shell.ShellScript
import koodies.test.UniqueId
import koodies.test.matchesCurlyPattern
import koodies.test.output.CapturedOutput
import koodies.test.output.OutputCaptureExtension
import koodies.test.testWithTempDir
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
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
class ScriptsKtTest {

    private val echoingCommands =
        ">&1 echo \"test output 1\"; sleep 1; >&2 echo \"test error 1\"; sleep 1; >&1 echo \"test output 2\"; sleep 1; >&2 echo \"test error 2\""

    private fun getFactories(scriptContent: String = echoingCommands) = listOf<Path.() -> ManagedProcess>(
        {
            script(ShellScript { !scriptContent })
        },
        {
            script { !scriptContent }
        },
        {
            script(InMemoryLogger()) { !scriptContent }
        },
    )

    @Nested
    inner class ScriptFn {

        @TestFactory
        fun `should start implicitly`(uniqueId: UniqueId) = getFactories().testWithTempDir(uniqueId) { processFactory ->
            val process = processFactory()
            expectThat(process.started).isTrue()
        }

        @TestFactory
        fun `should start`(uniqueId: UniqueId) = getFactories().testWithTempDir(uniqueId) { processFactory ->
            val process = processFactory()
            process.start()
            expectThat(process.started).isTrue()
        }

        @TestFactory
        fun `should process`(uniqueId: UniqueId) = listOf<Path.(Processor<ManagedProcess>) -> ManagedProcess>(
            { script(ShellScript { !echoingCommands }, processor = it) },
            { script(it) { !echoingCommands } },
            { processor ->
                val logger = InMemoryLogger()
                val process = script(logger) { !echoingCommands }
                logger.logged.lines().forEach { line ->
                    if (line.contains("test output 1")) process.processor(OUT typed "test output 1")
                    if (line.contains("test output 2")) process.processor(OUT typed "test output 2")
                    if (line.contains("test error 1")) process.processor(ERR typed "test error 1")
                    if (line.contains("test error 2")) process.processor(ERR typed "test error 2")
                }
                process
            },
        ).testWithTempDir(uniqueId) { processFactory ->
            val processed = mutableListOf<IO>()
            processFactory { io -> processed.add(io) }
            expectThat(processed).contains(
                OUT typed "test output 1",
                OUT typed "test output 2",
                ERR typed "test error 1",
                ERR typed "test error 2",
            )
        }

        @TestFactory
        fun `should throw on unexpected exit value`(uniqueId: UniqueId) = getFactories("exit 42").testWithTempDir(uniqueId) { processFactory ->
            expectCatching { processFactory() }
                .isFailure()
                .isA<CompletionException>()
                .with({ message }) { isNotNull() and { containsDump() } }
        }
    }

    @Nested
    inner class OutputFn {

        @TestFactory
        fun `should run process synchronously implicitly`(uniqueId: UniqueId) = getFactories().testWithTempDir(uniqueId) { processFactory ->
            val process = processFactory()
            expectThat(process.exitValue).isEqualTo(0)
        }

        @TestFactory
        fun `should log IO`(uniqueId: UniqueId) = getFactories().testWithTempDir(uniqueId) { processFactory ->
            val process = processFactory()
            process.output()

            expectThat(process.ioLog.logged.drop(2).dropLast(1))
                .containsExactlyInAnyOrder(
                    OUT typed "test output 1",
                    ERR typed "test error 1",
                    OUT typed "test output 2",
                    ERR typed "test error 2",
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
    inner class ScriptOutputContains {

        @Test
        fun `should assert present string`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(scriptOutputContains("echo 'this is a test'", "Test", caseSensitive = false)).isTrue()
        }

        @Test
        fun `should assert missing string`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(scriptOutputContains("echo 'this is a test'", "Missing", caseSensitive = false)).isFalse()
        }

        @Test
        fun `should assert present string case-sensitive`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(scriptOutputContains("echo 'this is a test'", "test", caseSensitive = true)).isTrue()
        }

        @Test
        fun `should assert missing string case-sensitive`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(scriptOutputContains("echo 'this is a test'", "Test", caseSensitive = true)).isFalse()
        }
    }

    @Nested
    @ExtendWith(OutputCaptureExtension::class)
    inner class SynchronousExecution {

        @Test
        fun `should process without logging to System out or in`(output: CapturedOutput, uniqueId: UniqueId) = withTempDir(uniqueId) {
            script { line(">&1 echo \"test output\""); line(">&2 echo \"test error\"") }.processSynchronously()
            expectThat(output).get { out }.isEmpty()
            expectThat(output).get { err }.isEmpty()
        }

        @Test
        fun `should format merged output`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = script { line(">&1 echo \"test output\""); line(">&2 echo \"test error\"") }.processSynchronously()
            expectThat(process.ioLog.logged) {
                get { first().type }.isEqualTo(IO.Type.META)
                get { first().unformatted }.matchesCurlyPattern("Executing {}")
                contains(OUT typed "test output", ERR typed "test error")
            }
        }
    }
}
