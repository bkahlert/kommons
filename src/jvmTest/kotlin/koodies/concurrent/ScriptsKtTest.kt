package koodies.concurrent

import koodies.concurrent.process.IO
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Processor
import koodies.concurrent.process.Processors
import koodies.concurrent.process.containsDump
import koodies.logging.InMemoryLogger
import koodies.logging.RenderingLogger
import koodies.shell.ShellScript
import koodies.terminal.contains
import koodies.test.UniqueId
import koodies.test.testWithTempDir
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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

@Execution(CONCURRENT)
class ScriptsKtTest {

    private val echoingCommands =
        "echo \"test output ${'$'}TEST\"; sleep 1; >&2 echo \"test error 1\"; sleep 1; echo \"test output 2\"; sleep 1; >&2 echo \"test error 2\""

    private fun getFactories(
        scriptContent: String = echoingCommands,
        processor: Processor<ManagedProcess>? = Processors.noopProcessor(),
        logger: RenderingLogger? = InMemoryLogger(),
    ) = listOf<Path.() -> ManagedProcess>(
        {
            processor?.let { script(ShellScript { !scriptContent }, mapOf("TEST" to "env"), processor = processor) }
                ?: script(ShellScript { !scriptContent }, mapOf("TEST" to "env"))
        },
        {
            processor?.let { script(processor, mapOf("TEST" to "env")) { !scriptContent } }
                ?: script(environment = mapOf("TEST" to "env")) { !scriptContent }
        },
        {
            script(logger, mapOf("TEST" to "env")) { !scriptContent }
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
            { script(ShellScript { !echoingCommands }, mapOf("TEST" to "env"), processor = it) },
            { script(it, mapOf("TEST" to "env")) { !echoingCommands } },
            { processor ->
                val logger = InMemoryLogger()
                val process = script(logger, mapOf("TEST" to "env")) { !echoingCommands }
                logger.logged.lines().forEach { line ->
                    if (line.contains("test output env")) process.processor(IO.OUT typed "test output env")
                    if (line.contains("test output 2")) process.processor(IO.OUT typed "test output 2")
                    if (line.contains("test error 1")) process.processor(IO.ERR typed "test error 1")
                    if (line.contains("test error 2")) process.processor(IO.ERR typed "test error 2")
                }
                process
            },
        ).testWithTempDir(uniqueId) { processFactory ->
            val processed = mutableListOf<IO>()
            processFactory { io -> processed.add(io) }
            expectThat(processed).contains(
                IO.OUT typed "test output env",
                IO.OUT typed "test output 2",
                IO.ERR typed "test error 1",
                IO.ERR typed "test error 2",
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
}
