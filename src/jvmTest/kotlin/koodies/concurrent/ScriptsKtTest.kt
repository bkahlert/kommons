package koodies.concurrent

import koodies.concurrent.process.IO
import koodies.concurrent.process.IO.Type.ERR
import koodies.concurrent.process.IO.Type.OUT
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Processor
import koodies.concurrent.process.processSynchronously
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
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.nio.file.Path

@Execution(CONCURRENT)
class ScriptsKtTest {

    private val echoingCommands =
        ">&1 echo \"test output\"; sleep 1; >&2 echo \"test error\"; sleep 1; >&1 echo \"test output\"; sleep 1; >&2 echo \"test error\""

    @Nested
    inner class ScriptFn {
        private val factories = listOf<Path.() -> ManagedProcess>(
            {
                script(shellScript = ShellScript { !echoingCommands })
            },
            {
                script { !echoingCommands }
            },
        )

        @TestFactory
        fun `should start implicitly`(uniqueId: UniqueId) = factories.testWithTempDir(uniqueId) {
            val process = it()
            expectThat(process.started).isTrue()
        }

        @TestFactory
        fun `should start`(uniqueId: UniqueId) = factories.testWithTempDir(uniqueId) {
            val process = it()
            process.start()
            expectThat(process.started).isTrue()
        }

        @TestFactory
        fun `should process`(uniqueId: UniqueId) = listOf<Path.(Processor<ManagedProcess>) -> ManagedProcess>(
            {
                script(shellScript = ShellScript { !echoingCommands }, processor = it)
            },
            {
                script(processor = it) { !echoingCommands }
            },
        ).testWithTempDir(uniqueId) {
            val processed = mutableListOf<IO>()
            it { io -> processed.add(io) }
            expectThat(processed).contains(
                OUT typed "test output",
                ERR typed "test error"
            )
        }
    }

    @Nested
    inner class OutputFn {

        @Test
        fun `should run synchronously`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = script { !echoingCommands }
            expectThat(process.exitValue).isEqualTo(0)
        }

        @Test
        fun `should log IO`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = script { !echoingCommands }
            process.output()

            expectThat(process.ioLog.logged.drop(2))
                .containsExactlyInAnyOrder(
                    OUT typed "test output",
                    ERR typed "test error",
                    OUT typed "test output",
                    ERR typed "test error",
                )
        }

        @Test
        fun `should contain OUT`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val output = script { !echoingCommands }.output()

            expectThat(output).isEqualTo("""
                test output
                test output
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
        fun `should format merged output`(output: CapturedOutput, uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = script { line(">&1 echo \"test output\""); line(">&2 echo \"test error\"") }.processSynchronously()
            expectThat(process.ioLog.logged) {
                get { first().type }.isEqualTo(IO.Type.META)
                get { first().unformatted }.matchesCurlyPattern("Executing {}")
                contains(OUT typed "test output", ERR typed "test error")
            }
        }
    }
}
