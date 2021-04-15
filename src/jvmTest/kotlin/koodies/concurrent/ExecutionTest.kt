package koodies.concurrent

import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO
import koodies.concurrent.process.IO.ERR
import koodies.concurrent.process.IO.OUT
import koodies.exec.Process.ExitState.Failure
import koodies.concurrent.process.Processors
import koodies.exec.containsDump
import koodies.exec.hasState
import koodies.exec.io
import koodies.concurrent.process.merged
import koodies.concurrent.process.output
import koodies.debug.CapturedOutput
import koodies.logging.FixedWidthRenderingLogger.Border.DOTTED
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
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
import koodies.text.ANSI.ansiRemoved
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
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
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isSuccess
import strikt.assertions.isTrue
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.measureTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class ExecutionTest {

    private val echoingCommands =
        "echo \"test output env\"; sleep 1; >&2 echo \"test error 1\"; sleep 1; echo \"test output 2\"; sleep 1; >&2 echo \"test error 2\""

    private fun Path.getExecutable(uniqueId: UniqueId): ShellScript = ShellScript(uniqueId.simplified) { !echoingCommands }

    @Nested
    inner class ExecuteFn {

        @Test
        fun InMemoryLogger.`should start implicitly`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = getExecutable(uniqueId).execute { Processors.noopProcessor() }
            expectThat(process.started).isTrue()
        }

        @Test
        fun InMemoryLogger.`should start`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = getExecutable(uniqueId).execute { Processors.noopProcessor() }
            process.start()
            expectThat(process.started).isTrue()
        }

        @Test
        fun InMemoryLogger.`should process`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val processed = mutableListOf<IO>()
            getExecutable(uniqueId).execute { { io -> processed.add(io) } }
            expectThat(processed).contains(
                OUT typed "test output env",
                OUT typed "test output 2",
                ERR typed "test error 1",
                ERR typed "test error 2",
            )
        }

        @Test
        fun InMemoryLogger.`should not throw on unexpected exit value`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching { CommandLine(this, "exit", "42").execute { Processors.noopProcessor() } }.isSuccess()
                .hasState<Failure> { io<IO>().containsDump(containedStrings = emptyArray()) }
        }
    }

    @Nested
    inner class OutputFn {

        @Test
        fun InMemoryLogger.`should run process synchronously implicitly`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = getExecutable(uniqueId).execute { Processors.noopProcessor() }
            expectThat(process.exitValue).isEqualTo(0)
        }

        @Test
        fun InMemoryLogger.`should log IO`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = getExecutable(uniqueId).execute { Processors.noopProcessor() }
            process.output()

            expectThat(process.io.drop(3).toList().dropLast(1))
                .containsExactlyInAnyOrder(
                    OUT typed "test output env",
                    ERR typed "test error 1",
                    OUT typed "test output 2",
                    ERR typed "test error 2",
                )
        }

        @Test
        fun InMemoryLogger.`should contain OUT`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val output = getExecutable(uniqueId).execute { Processors.noopProcessor() }.output()

            expectThat(output).toStringMatchesCurlyPattern("""
                {{}}
                test output env
                test output 2
                """.trimIndent())
        }
    }

    @Nested
    inner class SynchronousExecution {

        @Test
        fun InMemoryLogger.`should process synchronously by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val timePassed = measureTime { getExecutable(uniqueId).execute { null } }
            expectThat(timePassed).isGreaterThan(2.seconds)
        }

        @SystemIoExclusive
        @Test
        fun `should process by logging to console by default`(output: CapturedOutput, uniqueId: UniqueId) = withTempDir(uniqueId) {
            getExecutable(uniqueId).execute { null }
            expectThat(output).get { out }.matchesCurlyPattern("""
                    ▶ {{}}
                    · Executing {{}}
                    · {} file:{}
                    · {{}}
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
            getExecutable(uniqueId).execute { null }
            expectThatLogged().matchesCurlyPattern("""
                    {{}}
                    {}▶ {{}}
                    {}· Executing {{}}
                    {}· {} file:{}
                    {}· {{}}
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
            getExecutable(uniqueId).execute { { lock.withLock { output.appendLine(it.string) } } }
            expectThat(output).matchesCurlyPattern("""
                    Executing {{}}
                    {} file:{}
                    {{}}
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
            with(InMemoryLogger().withUnclosedWarningDisabled) { getExecutable(uniqueId).execute { { } } }
            expectThat(output).get { out }.isEmpty()
            expectThat(output).get { err }.isEmpty()
        }

        @SystemIoExclusive // prints log start to console
        @Test
        fun `should provide recorded output`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = getExecutable(uniqueId).execute { {} }
            expectThat(process.io.merged.ansiRemoved).matchesCurlyPattern("""
                    Executing {}
                    {} file:{}
                    {{}}
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
            val timePassed = measureTime { getExecutable(uniqueId).execute { processing { async }; null } }
            expectThat(timePassed).isLessThan(500.milliseconds)
        }

        @SystemIoExclusive
        @Test
        fun `should process by logging to console by default`(output: CapturedOutput, uniqueId: UniqueId) = withTempDir(uniqueId) {
            getExecutable(uniqueId).execute { processing { async }; null }
            output.poll()
            expectThat(output).get { all }.matchesCurlyPattern("""
                        ▶ Script{}
                        ⌛️ async computation
                        ⌛️ Executing {{}}
                        ⌛️ {} file:{}
                        ⌛️ {{}}
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
            getExecutable(uniqueId).execute { processing { async }; null }
            poll()
            expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   ▶ Script{}
                    │   ⌛️ async computation
                    │   ⌛️ Executing {}
                    │   ⌛️ 📄 file://{}
                    │   ⌛️ {{}}
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
            getExecutable(uniqueId).execute { processing { async }; { lock.withLock { output.appendLine(it.string) } } }
            output.poll()
            expectThat(output).matchesCurlyPattern("""
                    Executing {{}}
                    {} file:{}
                    {{}}
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
                getExecutable(uniqueId).execute { processing { async }; { logLine { it } } }
                poll()
            }
            expectThat(output).get { out }.isEmpty()
            expectThat(output).get { err }.isEmpty()
        }

        @SystemIoExclusive // prints log start to console
        @Test
        fun InMemoryLogger.`should provide recorded output`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = getExecutable(uniqueId).execute { processing { async }; { logLine { it } } }
            poll()
            expectThat(process.io.merged.ansiRemoved).matchesCurlyPattern("""
                    Executing {}
                    {} file:{}
                    {{}}
                    test output env
                    test error 1
                    test output 2
                    test error 2
                    Process {} terminated successfully at {}.
                    """.trimIndent())
        }
    }

    @Test
    fun InMemoryLogger.`should execute using existing logger`(uniqueId: UniqueId) = withTempDir(uniqueId){

        val executable: Executable = CommandLine(this, "echo", "test")

        with(executable) {
            logging("existing logging context") {
                execute { smart { caption by "command line logging context"; decorationFormatter by { Colors.brightBlue(it) }; border = SOLID };null }
            }
        }

        logging("existing logging context", border = SOLID, decorationFormatter = { Colors.brightMagenta(it) }) {
            logLine { "abc" }
            executable.execute { smart { caption by "command line logging context"; decorationFormatter by { Colors.magenta(it) }; border = SOLID };null }
        }
        logging("existing logging context", border = SOLID, decorationFormatter = { Colors.brightBlue(it) }) {
            logLine { "abc" }
            executable.execute { smart { caption by "command line logging context"; decorationFormatter by { Colors.blue(it) }; border = DOTTED };null }
        }
        logging("existing logging context", border = DOTTED, decorationFormatter = { Colors.brightMagenta(it) }) {
            logLine { "abc" }
            executable.execute { smart { caption by "command line logging context"; decorationFormatter by { Colors.magenta(it) }; border = SOLID };null }
        }
        logging("existing logging context", border = DOTTED, decorationFormatter = { Colors.brightBlue(it) }) {
            logLine { "abc" }
            executable.execute { smart { caption by "command line logging context"; decorationFormatter by { Colors.blue(it) }; border = DOTTED };null }
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
