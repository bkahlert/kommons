package koodies.concurrent

import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO
import koodies.concurrent.process.IO.ERR
import koodies.concurrent.process.IO.OUT
import koodies.concurrent.process.Processors
import koodies.concurrent.process.containsDump
import koodies.concurrent.process.logged
import koodies.concurrent.process.output
import koodies.debug.CapturedOutput
import koodies.logging.InMemoryLogger
import koodies.logging.RenderingLogger.Companion.withUnclosedWarningDisabled
import koodies.logging.expectThatLogged
import koodies.shell.ShellScript
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.test.SystemIoExclusive
import koodies.test.SystemIoRead
import koodies.test.UniqueId
import koodies.test.withTempDir
import koodies.text.ANSI.Colors
import koodies.text.matchesCurlyPattern
import koodies.time.IntervalPolling
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
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
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import java.util.concurrent.CompletionException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.measureTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class ExecutionTest {

    private val echoingCommands =
        "echo \"test output env\"; sleep 1; >&2 echo \"test error 1\"; sleep 1; echo \"test output 2\"; sleep 1; >&2 echo \"test error 2\""

    private val executable get() = ShellScript { !echoingCommands }

    @Nested
    inner class ExecuteFn {

        @Test
        fun InMemoryLogger.`should start implicitly`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = executable.execute { Processors.noopProcessor() }
            expectThat(process.started).isTrue()
        }

        @Test
        fun InMemoryLogger.`should start`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = executable.execute { Processors.noopProcessor() }
            process.start()
            expectThat(process.started).isTrue()
        }

        @Test
        fun InMemoryLogger.`should process`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val processed = mutableListOf<IO>()
            executable.execute { { io -> processed.add(io) } }
            expectThat(processed).contains(
                OUT typed "test output env",
                OUT typed "test output 2",
                ERR typed "test error 1",
                ERR typed "test error 2",
            )
        }

        @Test
        fun InMemoryLogger.`should throw on unexpected exit value`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching { CommandLine("exit", "42").execute { Processors.noopProcessor() } }
                .isFailure()
                .isA<CompletionException>()
                .with({ message }) {
                    isNotNull() and {
                        @Suppress("RemoveRedundantSpreadOperator")
                        containsDump(*emptyArray())
                    }
                }
        }
    }

    @Nested
    inner class OutputFn {

        @Test
        fun InMemoryLogger.`should run process synchronously implicitly`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = executable.execute { Processors.noopProcessor() }
            expectThat(process.exitValue).isEqualTo(0)
        }

        @Test
        fun InMemoryLogger.`should log IO`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = executable.execute { Processors.noopProcessor() }
            process.output()

            expectThat(process.ioLog.logged.drop(2).dropLast(1))
                .containsExactlyInAnyOrder(
                    OUT typed "test output env",
                    ERR typed "test error 1",
                    OUT typed "test output 2",
                    ERR typed "test error 2",
                )
        }

        @Test
        fun InMemoryLogger.`should contain OUT`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val output = executable.execute { Processors.noopProcessor() }.output()

            expectThat(output).isEqualTo(
                """
                    test output env
                    test output 2
                    """.trimIndent())
        }
    }

    @Nested
    inner class SynchronousExecution {

        @Test
        fun InMemoryLogger.`should process synchronously by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val timePassed = measureTime { executable.execute { null } }
            expectThat(timePassed).isGreaterThan(2.seconds)
        }

        @SystemIoExclusive
        @Test
        fun `should process by logging to console by default`(output: CapturedOutput, uniqueId: UniqueId) = withTempDir(uniqueId) {
            executable.execute { null }
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
            executable.execute { null }
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
            executable.execute { { lock.withLock { output.appendLine(it.string) } } }
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
            with(InMemoryLogger().withUnclosedWarningDisabled) { executable.execute { { } } }
            expectThat(output).get { out }.isEmpty()
            expectThat(output).get { err }.isEmpty()
        }

        @Test
        fun `should provide recorded output`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = executable.execute { {} }
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
            val timePassed = measureTime { executable.execute { processing { async }; null } }
            expectThat(timePassed).isLessThan(500.milliseconds)
        }

        @SystemIoExclusive
        @Test
        fun `should process by logging to console by default`(output: CapturedOutput, uniqueId: UniqueId) = withTempDir(uniqueId) {
            executable.execute { processing { async }; null }
            output.poll()
            expectThat(output).get { all }.matchesCurlyPattern("""
                        ▶ Script{}
                        ⌛️ async computation
                        ⌛️ Executing {{}}
                        ⌛️ {} file:{}
                        ⌛️ test output env
                        ⌛️ test error 1
                        ⌛️ test output 2
                        ⌛️ test error 2
                        ⌛️ Process {} terminated successfully at {}
                        ⌛️ ✔︎
                     """.trimIndent())
            expectThat(output).get { err }.isEmpty()
        }

        @Test
        fun InMemoryLogger.`should process by logging using existing logger`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            executable.execute { processing { async }; null }
            poll()
            expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   ▶ Script{}
                    │   ⌛️ async computation
                    │   ⌛️ Executing {}
                    │   ⌛️ 📄 file://{}
                    │   ⌛️ test output env
                    │   ⌛️ test error 1
                    │   ⌛️ test output 2
                    │   ⌛️ test error 2
                    │   ⌛️ Process {} terminated successfully at {}
                    │   ⌛️ ✔︎
                    │
                    ╰──╴✔︎
                    """.trimIndent())
        }

        @Test
        fun InMemoryLogger.`should process by using specified processor`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val output = StringBuilder()
            val lock = ReentrantLock(false)
            executable.execute { processing { async }; { lock.withLock { output.appendLine(it.string) } } }
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
                executable.execute { processing { async }; { logLine { it } } }
                poll()
            }
            expectThat(output).get { out }.isEmpty()
            expectThat(output).get { err }.isEmpty()
        }

        @Test
        fun InMemoryLogger.`should provide recorded output`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = executable.execute { processing { async }; { logLine { it } } }
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
    }

    @Test
    fun InMemoryLogger.`should execute using existing logger`() {

        val executable: Executable = CommandLine("echo", "test")

        with(executable) {
            logging("existing logging context") {
                execute { smart { caption by "command line logging context"; decorationFormatter by { Colors.brightBlue(it) }; border by true };null }
            }
        }

        logging("existing logging context", bordered = true, decorationFormatter = { Colors.brightMagenta(it) }) {
            logLine { "abc" }
            executable.execute { smart { caption by "command line logging context"; decorationFormatter by { Colors.magenta(it) }; border by true };null }
        }
        logging("existing logging context", bordered = true, decorationFormatter = { Colors.brightBlue(it) }) {
            logLine { "abc" }
            executable.execute { smart { caption by "command line logging context"; decorationFormatter by { Colors.blue(it) }; border by false };null }
        }
        logging("existing logging context", bordered = false, decorationFormatter = { Colors.brightMagenta(it) }) {
            logLine { "abc" }
            executable.execute { smart { caption by "command line logging context"; decorationFormatter by { Colors.magenta(it) }; border by true };null }
        }
        logging("existing logging context", bordered = false, decorationFormatter = { Colors.brightBlue(it) }) {
            logLine { "abc" }
            executable.execute { smart { caption by "command line logging context"; decorationFormatter by { Colors.blue(it) }; border by false };null }
        }

        expectThatLogged().matchesCurlyPattern("""
            ╭──╴{}
            │   
            │   ╭──╴existing logging context
            │   │   
            │   │   ╭──╴command line logging context
            │   │   │   
            │   │   │   Executing echo test
            │   │   │   test
            │   │   │   Process {} terminated successfully at {}.
            │   │   │
            │   │   ╰──╴✔︎
            │   │
            │   ╰──╴✔︎
            │   ╭──╴existing logging context
            │   │   
            │   │   abc
            │   │   ╭──╴command line logging context
            │   │   │   
            │   │   │   Executing echo test
            │   │   │   test
            │   │   │   Process {} terminated successfully at {}.
            │   │   │
            │   │   ╰──╴✔︎
            │   │
            │   ╰──╴✔︎
            │   ╭──╴existing logging context
            │   │   
            │   │   abc
            │   │   ▶ command line logging context
            │   │   · Executing echo test
            │   │   · test
            │   │   · Process {} terminated successfully at {}.
            │   │   ✔︎
            │   │
            │   ╰──╴✔︎
            │   ▶ existing logging context
            │   · abc
            │   · ╭──╴command line logging context
            │   · │   
            │   · │   Executing echo test
            │   · │   test
            │   · │   Process {} terminated successfully at {}.
            │   · │
            │   · ╰──╴✔︎
            │   ✔︎
            │   ▶ existing logging context
            │   · abc
            │   · ▶ command line logging context
            │   · · Executing echo test
            │   · · test
            │   · · Process {} terminated successfully at {}.
            │   · ✔︎
            │   ✔︎
            │
            ╰──╴✔︎
        """.trimIndent())
    }
}
