package koodies.docker

import koodies.concurrent.execute
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO.OUT
import koodies.concurrent.process.logged
import koodies.debug.CapturedOutput
import koodies.logging.InMemoryLogger
import koodies.logging.RenderingLogger.Companion.withUnclosedWarningDisabled
import koodies.logging.expectThatLogged
import koodies.shell.ShellExecutable
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.test.SystemIoExclusive
import koodies.test.SystemIoRead
import koodies.test.UniqueId
import koodies.test.withTempDir
import koodies.text.ANSI
import koodies.text.ANSI.Colors.magenta
import koodies.text.matchesCurlyPattern
import koodies.text.withRandomSuffix
import koodies.time.IntervalPolling
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.measureTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@DockerTestImageExclusive
@Nested
class DockerizedExecutionTest {

    private val shellExecutable: ShellExecutable = CommandLine("echo", "Hello World!")

    @BeforeEach
    fun setUp() {
//            DOCKER_TEST_CONTAINER.start()
    }

    @Test
    fun InMemoryLogger.`should run container without options and log`() {
        expectThat(shellExecutable.executeDockerized(DockerTestImageExclusive.DOCKER_TEST_CONTAINER.image, null)).isA<DockerProcess>()
        expectThat(logged).matchesCurlyPattern("""
                {{}}
                {}▶ Executing dockerized with ubuntu: echo "Hello World!"
                {}· Executing docker run --name echo-_Hello-World__ --rm -i ubuntu echo "Hello World!"
                {}· Hello World!
                {}✔︎
                """.trimIndent())
    }

    @SystemIoExclusive
    @Test
    fun `should run command line without options and print`(capturedOutput: CapturedOutput) {
        expectThat(shellExecutable.executeDockerized(DockerTestImageExclusive.DOCKER_TEST_CONTAINER.image, null)).isA<DockerProcess>()
        expectThat(capturedOutput).matchesCurlyPattern("""
                ▶ Executing dockerized with ubuntu: echo "Hello World!"
                · Executing docker run --name echo-_Hello-World__ --rm -i ubuntu echo "Hello World!"
                · Hello World!
                ✔︎
                """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should run command line and log`() {
        expectThat(shellExecutable.executeDockerized(DockerTestImageExclusive.DOCKER_TEST_CONTAINER.image) {
            dockerOptions { name { "container-name".withRandomSuffix() } }
            executionOptions {
                block {
                    caption { "test" }
                    contentFormatter { ANSI.Formatter { (it as? OUT)?.reversed()?.magenta() ?: "" } }
                }
            }
            null
        }).isA<DockerProcess>()
        expectThat(logged).matchesCurlyPattern("""
                {{}}
                {}▶ test
                {}· !dlroW olleH
                {}✔︎
                """.trimIndent())
    }

    @SystemIoExclusive
    @Test
    fun `should run command line and print`(capturedOutput: CapturedOutput) {
        expectThat(shellExecutable.executeDockerized(DockerTestImageExclusive.DOCKER_TEST_CONTAINER.image) {
            dockerOptions { name { "container-name".withRandomSuffix() } }
            executionOptions {
                block {
                    caption { "test" }
                    contentFormatter { ANSI.Formatter { (it as? OUT)?.reversed()?.magenta() ?: "" } }
                }
            }
            null
        }).isA<DockerProcess>()
        expectThat(capturedOutput).matchesCurlyPattern("""
                ▶ test
                · !dlroW olleH
                ✔︎
                """.trimIndent())
    }


// TODO copied from Processes ; adapt to Docker


    @Nested
    inner class SynchronousExecution {

        @Test
        fun InMemoryLogger.`should process synchronously by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val timePassed = measureTime { shellExecutable.execute { null } }
            expectThat(timePassed).isGreaterThan(2.seconds)
        }

        @SystemIoExclusive
        @Test
        fun `should process by logging to console by default`(output: CapturedOutput, uniqueId: UniqueId) = withTempDir(uniqueId) {
            shellExecutable.execute { null }
            expectThat(output).get { out }.matchesCurlyPattern("""
                    ▶ {{}}
                    · Executing {{}}
                    · {} file:{}
                    · test output env
                    · test output 2
                    · test error 1
                    · test error 2
                    · Process {} terminated successfully at {}.
                    ✔︎
                    """.trimIndent())
            expectThat(output).get { err }.isEmpty()
        }

        @Test
        fun InMemoryLogger.`should process by logging using existing logger`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            shellExecutable.execute { null }
            expectThatLogged().matchesCurlyPattern("""
                    {{}}
                    {}▶ {{}}
                    {}· Executing {{}}
                    {}· {} file:{}
                    {}· test output env
                    {}· test output 2
                    {}· test error 1
                    {}· test error 2
                    {}· Process {} terminated successfully at {}.
                    {}✔︎
                    {{}}
                    """.trimIndent())
        }

        @Test
        fun InMemoryLogger.`should process by using specified processor`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val output = StringBuilder()
            val lock = ReentrantLock()
            shellExecutable.execute { { lock.withLock { output.appendLine(it.string) } } }
            expectThat(output).matchesCurlyPattern("""
                    Executing {{}}
                    {} file:{}
                    test output env
                    test output 2
                    test error 1
                    test error 2
                    Process {} terminated successfully at {}.
                    """.trimIndent())
        }

        @SystemIoRead
        @Test
        fun `should not print to console if logging with logger`(output: CapturedOutput, uniqueId: UniqueId) = withTempDir(uniqueId) {
            with(InMemoryLogger().withUnclosedWarningDisabled) { shellExecutable.execute { { } } }
            expectThat(output).get { out }.isEmpty()
            expectThat(output).get { err }.isEmpty()
        }

        @Test
        fun `should provide recorded output`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = shellExecutable.execute { {} }
            expectThat(process.logged).matchesCurlyPattern("""
                    Executing {}
                    {} file:{}
                    test output env
                    test output 2
                    test error 1
                    test error 2
                    Process {} terminated successfully at {}.
                    """.trimIndent())
        }
    }

    @Nested
    inner class AsynchronousExecution {

        private fun IntervalPolling.withDefaults() =
            every(1000.milliseconds).forAtMost(5.seconds) { fail("Did not finish logging within 5 seconds.") }

        private val defaultPredicate: String.() -> Boolean = { contains("Process") and contains("terminated") }

        private fun InMemoryLogger.poll(predicate: String.() -> Boolean = defaultPredicate) =
            koodies.time.poll { toString().predicate() }.withDefaults()

        private fun CapturedOutput.poll(predicate: String.() -> Boolean = defaultPredicate) =
            koodies.time.poll { all.removeEscapeSequences().predicate() }.withDefaults()

        private fun StringBuilder.poll(predicate: String.() -> Boolean = defaultPredicate) =
            koodies.time.poll { toString().removeEscapeSequences().predicate() }.withDefaults()

        @Test
        fun InMemoryLogger.`should process asynchronously if specified`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val timePassed = measureTime { shellExecutable.execute { processing { async }; null } }
            expectThat(timePassed).isLessThan(500.milliseconds)
        }

        @SystemIoExclusive
        @Test
        fun `should process by logging to console by default`(output: CapturedOutput, uniqueId: UniqueId) = withTempDir(uniqueId) {
            shellExecutable.execute { processing { async }; null }
            output.poll()
            expectThat(output).get { all }.matchesCurlyPattern("""
                        {}echo {} Executing {} ⌛️
                        {}echo {} ⌛️ test output env
                        {}echo {} ⌛️ test error 1
                        {}echo {} ⌛️ test output 2
                        {}echo {} ⌛️ test error 2
                        {}echo {} ⌛️ Process {} terminated successfully at {}.
                     """.trimIndent())
            expectThat(output).get { err }.isEmpty()
        }

        @Test
        fun InMemoryLogger.`should process by logging using existing logger`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            shellExecutable.execute { processing { async }; null }
            poll()
            expectThatLogged().matchesCurlyPattern("""
                    {{}}
                    {}echo {} Executing {} ⌛️
                    {}echo {} ⌛️ test output env
                    {}echo {} ⌛️ test error 1
                    {}echo {} ⌛️ test output 2
                    {}echo {} ⌛️ test error 2
                    {}echo {} ⌛️ Process {} terminated successfully at {}.
                    {{}}
                    """.trimIndent())
        }

        @Test
        fun InMemoryLogger.`should process by using specified processor`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val output = StringBuilder()
            val lock = ReentrantLock(false)
            shellExecutable.execute { processing { async }; { lock.withLock { output.appendLine(it.string) } } }
            output.poll()
            expectThat(output).matchesCurlyPattern("""
                    Executing {{}}
                    {} file:{}
                    test output env
                    test error 1
                    test output 2
                    test error 2
                    Process {} terminated successfully at {}.
                    """.trimIndent())
        }

        @SystemIoRead
        @Test
        fun `should not print to console if logging with logger`(output: CapturedOutput, uniqueId: UniqueId) = withTempDir(uniqueId) {
            with(InMemoryLogger().withUnclosedWarningDisabled) {
                shellExecutable.execute { processing { async }; { logLine { it } } }
                poll()
            }
            expectThat(output).get { out }.isEmpty()
            expectThat(output).get { err }.isEmpty()
        }

        @Test
        fun InMemoryLogger.`should provide recorded output`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = shellExecutable.execute { processing { async }; { logLine { it } } }
            poll()
            expectThat(process.logged).matchesCurlyPattern("""
                    Executing {}
                    {} file:{}
                    test output env
                    test error 1
                    test output 2
                    test error 2
                    Process {} terminated successfully at {}.
                    """.trimIndent())
        }

        // TODO DELETE BELOW
        @Test
        fun `should process with non-blocking reader`() {

            val timePassed = measureTime {
                CommandLine("sleep", "10").execute {
                    processing { async }
                    null
                }
            }
            expectThat(timePassed).isLessThan(250.milliseconds)
        }

        @Test
        fun `should process with non-blocking reader2`() {
            val timePassed = measureTime {
                CommandLine("sleep", "10").executeDockerized(DockerTestImageExclusive.DOCKER_TEST_CONTAINER.image) {
                    dockerOptions { name { "test" } }
                    executionOptions { processing { async } }
                    null
                }
            }
            expectThat(timePassed).isLessThan(250.milliseconds)
        }
    }
}
