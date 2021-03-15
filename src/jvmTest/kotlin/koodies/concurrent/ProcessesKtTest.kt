package koodies.concurrent

import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO
import koodies.concurrent.process.IO.Type.ERR
import koodies.concurrent.process.IO.Type.OUT
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Processor
import koodies.concurrent.process.Processors
import koodies.concurrent.process.containsDump
import koodies.concurrent.process.logged
import koodies.concurrent.process.process
import koodies.debug.CapturedOutput
import koodies.logging.InMemoryLogger
import koodies.logging.RenderingLogger
import koodies.logging.logging
import koodies.test.SystemIoExclusive
import koodies.test.SystemIoRead
import koodies.test.UniqueId
import koodies.test.testWithTempDir
import koodies.text.ANSI
import koodies.text.ANSI.Colors
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
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
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Execution(CONCURRENT)
class ProcessesKtTest {

    @Nested
    inner class WithProcessMethod {

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
                        OUT typed "test output env",
                        ERR typed "test error 1",
                        OUT typed "test output 2",
                        ERR typed "test error 2",
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
                        OUT typed "test output env",
                        ERR typed "test error 1",
                        OUT typed "test output 2",
                        ERR typed "test error 2",
                    )
            }

            @TestFactory
            fun `should contain OUT`(uniqueId: UniqueId) = getFactories().testWithTempDir(uniqueId) { processFactory ->
                val output = processFactory().output()

                expectThat(output).isEqualTo("""
                test output env
                test output 2
            """.trimIndent())
            }
        }

        @Nested
        inner class SynchronousExecution {

            @SystemIoExclusive
            @TestFactory
            fun `should process log to console by default`(output: CapturedOutput, uniqueId: UniqueId) =
                getFactories().testWithTempDir(uniqueId) { processFactory ->
                    val process = processFactory()
                    process.process()
                    expectThat(output).get { out }.matchesCurlyPattern("""
                ▶{}commandLine{{}}
                · Executing {{}}
                · {} file:{}
                · test output env
                · test output 2
                · test error 1
                · test error 2{{}}
            """.trimIndent())
                    expectThat(output).get { err }.isEmpty()
                }

            @SystemIoRead
            @TestFactory
            fun `should process without logging to console if specified`(output: CapturedOutput, uniqueId: UniqueId) =
                getFactories().testWithTempDir(uniqueId) { processFactory ->
                    val process = processFactory()
                    process.process({ sync }, Processors.noopProcessor())
                    expectThat(output).get { out }.isEmpty()
                    expectThat(output).get { err }.isEmpty()
                }

            @SystemIoExclusive
            @TestFactory
            fun `should format merged output`(uniqueId: UniqueId) =
                getFactories().testWithTempDir(uniqueId) { processFactory ->
                    val process = processFactory()
                    process.process({ sync }, Processors.noopProcessor())
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
    }


    @Nested
    inner class WithExecuteMethod {

        private val echoingCommands =
            "echo \"test output env\"; sleep 1; >&2 echo \"test error 1\"; sleep 1; echo \"test output 2\"; sleep 1; >&2 echo \"test error 2\""

        private fun getFactories(
            commandLineContent: String = echoingCommands,
            processor: Processor<ManagedProcess>? = Processors.noopProcessor(),
            logger: RenderingLogger? = InMemoryLogger(),
        ) = listOf<Path.() -> ManagedProcess>(
            {
                processor?.let { CommandLine { command by "/bin/sh"; arguments { +"-c" + commandLineContent } }.execute(null) { processor } }
                    ?: CommandLine { command by "/bin/sh"; arguments { +"-c" + commandLineContent } }.execute(null) { null }
            },
        )

        @Nested
        inner class ExecuteFn {

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
                { CommandLine { command by "/bin/sh"; arguments { +"-c" + echoingCommands } }.execute(null) { it } },
            ).testWithTempDir(uniqueId) { processFactory ->
                val processed = mutableListOf<IO>()
                processFactory { io -> processed.add(io) }
                expectThat(processed).contains(
                    OUT typed "test output env",
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
                    .with({ message }) { isNotNull() and { containsDump("/bin/sh") } }
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
                        OUT typed "test output env",
                        ERR typed "test error 1",
                        OUT typed "test output 2",
                        ERR typed "test error 2",
                    )
            }

            @TestFactory
            fun `should contain OUT`(uniqueId: UniqueId) = getFactories().testWithTempDir(uniqueId) { processFactory ->
                val output = processFactory().output()

                expectThat(output).isEqualTo("""
                test output env
                test output 2
            """.trimIndent())
            }
        }

        @Nested
        inner class SynchronousExecution {

            @SystemIoExclusive
            @TestFactory
            fun `should process log to console by default`(output: CapturedOutput, uniqueId: UniqueId) =
                getFactories(processor = null, logger = null).testWithTempDir(uniqueId) { processFactory ->
                    processFactory()
                    expectThat(output).get { out }.matchesCurlyPattern("""
                ▶{}commandLine{{}}
                · Executing {{}}
                · {} file:{}
                · test output env
                · test output 2
                · test error 1
                · test error 2{{}}
                """.trimIndent())
                    expectThat(output).get { err }.isEmpty()
                }

            @SystemIoRead
            @TestFactory
            fun `should process not log to console if specified`(output: CapturedOutput, uniqueId: UniqueId) =
                getFactories(processor = {}, logger = InMemoryLogger()).testWithTempDir(uniqueId) { processFactory ->
                    val process = processFactory()
                    process.process({ sync }, Processors.noopProcessor())
                    expectThat(output).get { out }.isEmpty()
                    expectThat(output).get { err }.isEmpty()
                }

            @SystemIoExclusive
            @TestFactory
            fun `should format merged output`(uniqueId: UniqueId) =
                getFactories(processor = {}, logger = null).testWithTempDir(uniqueId) { processFactory ->
                    val process = processFactory()
                    process.process({ sync }, Processors.noopProcessor())
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
    }

    @Test
    fun InMemoryLogger.`should execute using existing logger`() {

        val commandLine = CommandLine("echo", "test")

        with(commandLine) {
            logging("existing logging context") {
                execute { loggingOptions { caption by "command line logging context"; formatter by Colors.brightBlue; border by true };null }
            }
        }

        logging("existing logging context", bordered = true, formatter = ANSI.Colors.brightMagenta) {
            with(commandLine) {
                logLine { "abc" }
                execute { loggingOptions { caption by "command line logging context"; formatter by ANSI.Colors.magenta; border by true };null }
            }
        }
        logging("existing logging context", bordered = true, formatter = ANSI.Colors.brightBlue) {
            with(commandLine) {
                logLine { "abc" }
                execute { loggingOptions { caption by "command line logging context"; formatter by ANSI.Colors.blue; border by false };null }
            }
        }
        logging("existing logging context", bordered = false, formatter = ANSI.Colors.brightMagenta) {
            with(commandLine) {
                logLine { "abc" }
                execute { loggingOptions { caption by "command line logging context"; formatter by ANSI.Colors.magenta; border by true };null }
            }
        }
        logging("existing logging context", bordered = false, formatter = ANSI.Colors.brightBlue) {
            with(commandLine) {
                logLine { "abc" }
                execute { loggingOptions { caption by "command line logging context"; formatter by ANSI.Colors.blue; border by false };null }
            }
        }

        expectThat(logged).matchesCurlyPattern("""
            ╭──╴ProcessesKtTest ➜ {}
            │   
            │   ╭──╴existing logging context
            │   │   
            │   │   ╭──╴command line logging context
            │   │   │   
            │   │   │   Executing echo test
            │   │   │   test
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
            │   │   ✔︎
            │   │
            │   ╰──╴✔︎
            │   ▶ existing logging context
            │   · abc
            │   · ╭──╴command line logging context
            │   · │   
            │   · │   Executing echo test
            │   · │   test
            │   · │
            │   · ╰──╴✔︎
            │   ✔︎
            │   ▶ existing logging context
            │   · abc
            │   · ▶ command line logging context
            │   · · Executing echo test
            │   · · test
            │   · ✔︎
            │   ✔︎
        """.trimIndent())
    }
}
