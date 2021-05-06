package koodies.exec.mock

import koodies.exec.Process.ExitState
import koodies.exec.Process.ExitState.Success
import koodies.exec.Process.ProcessState
import koodies.exec.ProcessingMode.Interactivity.NonInteractive
import koodies.exec.enter
import koodies.exec.exitCode
import koodies.exec.fails
import koodies.exec.hasState
import koodies.exec.mock.ExecMock.Companion.FAILED_EXEC
import koodies.exec.mock.ExecMock.Companion.RUNNING_EXEC
import koodies.exec.mock.ExecMock.Companion.SUCCEEDED_EXEC
import koodies.exec.mock.JavaProcessMock.Companion.FAILING_PROCESS
import koodies.exec.mock.JavaProcessMock.Companion.RUNNING_PROCESS
import koodies.exec.mock.JavaProcessMock.Companion.SUCCEEDING_PROCESS
import koodies.exec.mock.JavaProcessMock.Companion.processMock
import koodies.exec.mock.JavaProcessMock.Companion.withIndividuallySlowInput
import koodies.exec.mock.JavaProcessMock.Companion.withSlowInput
import koodies.exec.mock.SlowInputStream.Companion.prompt
import koodies.exec.mock.SlowInputStream.Companion.slowInputStream
import koodies.exec.process
import koodies.exec.starts
import koodies.exec.status
import koodies.exec.succeeds
import koodies.exec.terminationLoggingProcessor
import koodies.io.ByteArrayOutputStream
import koodies.io.path.isEqualToByteWise
import koodies.jvm.daemon
import koodies.logging.InMemoryLogger
import koodies.nio.NonBlockingReader
import koodies.test.Slow
import koodies.test.assertTimeoutPreemptively
import koodies.test.expectThrows
import koodies.test.expecting
import koodies.test.test
import koodies.test.testEach
import koodies.text.LineSeparators.LF
import koodies.time.poll
import koodies.time.sleep
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isLessThanOrEqualTo
import strikt.assertions.isTrue
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.measureTime
import kotlin.time.seconds

@Execution(CONCURRENT)
class JavaExecMockTest {

    @Nested
    inner class ProcessMockFixtures {

        @Nested
        inner class ProcessMocks {

            @TestFactory
            fun `should run`() = test({ RUNNING_PROCESS }) {
                expecting { it().isAlive } that { isTrue() }
                expecting("stays running for 5s") {
                    val p = it(); poll { !p.isAlive }.every(Duration.milliseconds(500)).forAtMost(Duration.seconds(5))
                } that { isFalse() }
            }

            @TestFactory
            fun `should have completed successfully`() = test({ SUCCEEDING_PROCESS }) {
                expecting { it().isAlive } that { isFalse() }
                expecting { it().exitValue() } that { isEqualTo(0) }
            }

            @TestFactory
            fun `should have failed`() = test({ FAILING_PROCESS }) {
                expecting { it().isAlive } that { isFalse() }
                expecting { it().exitValue() } that { isEqualTo(42) }
            }
        }

        @Nested
        inner class ExecMocks {

            @TestFactory
            fun `should run`() = test({ RUNNING_EXEC }) {
                expecting { it() } that { starts() }
                expecting { it() } that { hasState<ProcessState.Running> { status.contains("running") } }
                expecting("stays running for 5s") {
                    val p = it(); poll { p.state !is ProcessState.Running }.every(Duration.milliseconds(500)).forAtMost(Duration.seconds(5))
                } that { isFalse() }
            }

            @TestFactory
            fun `should succeed`() = test({ SUCCEEDED_EXEC }) {
                expecting { it() } that { succeeds() }
                expecting { it() } that { hasState<Success> { status.contains("terminated successfully") } }
            }

            @TestFactory
            fun `should fail`() = test({ FAILED_EXEC }) {
                expecting { it() } that { fails() }
                expecting { it() } that { hasState<ExitState.Failure> { status.contains("terminated with exit code") } }
            }
        }
    }

    @Nested
    inner class WithSlowInputStream {

        @Test
        fun InMemoryLogger.`should provide input correctly`() {
            val slowInputStream = slowInputStream(Duration.seconds(1), "Hello$LF", "World!$LF")

            assertTimeoutPreemptively(Duration.seconds(10)) {
                val read = String(slowInputStream.readBytes())

                expectThat(read).isEqualTo("Hello\nWorld!$LF")
            }
        }

        @Test
        fun InMemoryLogger.`should provide input slowly`() {
            val delay = Duration.seconds(1)
            val slowInputStream = slowInputStream(delay, "Hello$LF", "World!$LF")

            assertTimeoutPreemptively(delay * 5) {
                val duration = measureTime {
                    String(slowInputStream.readBytes())
                }
                expectThat(duration).assertThat("is slow") { it > delay }
            }
        }

        @TestFactory
        fun InMemoryLogger.`should provide 'block on prompt' behavior`() = listOf(
            "with echoed input" to true,
            "without echoed input" to false,
        ).testEach("{}") { (_, echoOption) ->
            val byteArrayOutputStream = ByteArrayOutputStream()
            val slowInputStream = slowInputStream(
                Duration.seconds(0),
                Duration.seconds(0) to "Password? ",
                prompt(),
                Duration.seconds(0) to "\r",
                Duration.seconds(0) to "Correct!$LF",
                byteArrayOutputStream = byteArrayOutputStream,
                echoInput = echoOption,
            )

            val input = "password1234"
            val output = StringBuilder()
            val start = System.currentTimeMillis()
            while (!slowInputStream.terminated) {
                if (Duration.milliseconds((System.currentTimeMillis() - start)) > .8.seconds) {
                    byteArrayOutputStream.write("password1234\r".toByteArray())
                    byteArrayOutputStream.flush()
                }
                val available = slowInputStream.available()
                if (available > 0) {
                    val byteArray = ByteArray(available)
                    val read = slowInputStream.read(byteArray, 0, available)
                    expecting { read } that { isGreaterThan(0) }
                    output.append(String(byteArray))
                }
                Thread.sleep(10)
            }
            if (echoOption) expecting { output } that { isEqualToByteWise("Password? $input\r\rCorrect!$LF") }
            else expecting { output } that { isEqualToByteWise("Password? \rCorrect!$LF") }
        }


        @Test
        fun InMemoryLogger.`should produce same byte sequence as ByteArrayInputStream`() {
            val input = "A𝌪𝌫𝌬𝌭𝌮Z"
            val inputStream = slowInputStream(Duration.seconds(2), input)
            expectThat(input.byteInputStream().readAllBytes()).isEqualTo(inputStream.readAllBytes())
        }

        @TestFactory
        fun InMemoryLogger.`should never apply delay at at end stream`() = "𝌪".let { input ->
            listOf(
                slowInputStream(Duration.seconds(5), input, echoInput = true),
                slowInputStream(Duration.seconds(5), Duration.seconds(0) to input, echoInput = true),
            ).testEach("{}") { inputStream ->
                val duration = measureTime {
                    @Suppress("ControlFlowWithEmptyBody")
                    while (inputStream.read() > -1) {
                    }
                    inputStream.available()
                    inputStream.read()
                    inputStream.available()
                    inputStream.read()
                    inputStream.available()
                    inputStream.read()
                }
                expecting { duration } that { isLessThanOrEqualTo(Duration.seconds(2)) }
            }
        }
    }

    @Nested
    inner class ReadingExitValue {

        @Test
        fun InMemoryLogger.`should return 0 by default`() {
            expectThat(processMock().exitValue()).isEqualTo(0)
        }

        @Test
        fun InMemoryLogger.`should throw exception`() {
            expectThrows<IllegalStateException> {
                processMock { throw IllegalStateException() }.exitValue()
            }
        }

        @Test
        fun InMemoryLogger.`should delay exit`() {
            expecting { measureTime { processMock(exitDelay = Duration.milliseconds(50)).waitFor() } } that {
                isGreaterThan(Duration.milliseconds(40))
                isLessThan(Duration.milliseconds(80))
            }
        }

        @Test
        fun InMemoryLogger.`should return true if process exits in time`() {
            expecting { processMock(exitDelay = Duration.milliseconds(50)).waitFor(100, TimeUnit.MILLISECONDS) } that { isTrue() }
        }

        @Test
        fun InMemoryLogger.`should return false if process not exits in time`() {
            expecting { processMock(exitDelay = Duration.milliseconds(50)).waitFor(25, TimeUnit.MILLISECONDS) } that { isFalse() }
        }

        @Nested
        inner class Liveliness {

            @Nested
            inner class WithDefaultInputStream {

                @Test
                fun InMemoryLogger.`should not be alive if exit is not delayed`() {
                    expectThat(processMock().isAlive).isFalse()
                }

                @Test
                fun InMemoryLogger.`should be alive if exit is delayed`() {
                    expectThat(processMock(exitDelay = Duration.milliseconds(50)).isAlive).isTrue()
                }
            }

            @Nested
            inner class WithSlowInputStream {
                @Test
                fun InMemoryLogger.`should be finished if all read`() {
                    val p = withSlowInput(echoInput = true)
                    expectThat(p.isAlive).isFalse()
                }

                @Test
                fun InMemoryLogger.`should be alive if not all read`() {
                    val p = withSlowInput(
                        "unread",
                        echoInput = true,
                    )
                    expectThat(p.isAlive).isTrue()
                }
            }
        }
    }

    @Nested
    inner class OutputStreamWiring {
        @Test
        fun InMemoryLogger.`should allow SlowInputStream to read process's input stream`() {
            val p = withIndividuallySlowInput(prompt(), echoInput = true)
            with(p.outputStream.writer()) {
                expectThat(p.received).isEmpty()
                expectThat(p.inputStream.available()).isEqualTo(0)

                write("user input")
                flush() // !

                expectThat(p.received).isEqualTo("user input")
                logging("???") {
                    (p.inputStream as SlowInputStream).processInput(this)
                }
                expectThat(p.inputStream.available()).isEqualTo("user input".length)
            }
        }
    }

    @Slow
    @Test
    fun InMemoryLogger.`should terminate if all output is manually read`() {
        val p = withIndividuallySlowInput(
            Duration.milliseconds(500) to "Welcome!$LF",
            Duration.milliseconds(500) to "Password? ",
            prompt(),
            Duration.milliseconds(500) to "\r",
            Duration.milliseconds(500) to "Correct!$LF",
            baseDelayPerInput = Duration.seconds(1),
            echoInput = true,
        )

        val reader = NonBlockingReader(p.inputStream, logger = this)

        daemon {
            Thread.sleep(5000)
            p.outputStream.enter("password")
        }

        expectThat(reader.readLine()).isEqualTo("Welcome!")
        expectThat(reader.readLine()).isEqualTo("Password? password")
        expectThat(reader.readLine()).isEqualTo("")
        expectThat(reader.readLine()).isEqualTo("Correct!")
        expectThat(reader.readLine()).isEqualTo(null)
        expectThat(p.isAlive).isFalse()
    }

    @Slow
    @Test
    fun InMemoryLogger.`should terminate if all output is consumed`() {
        val p = withIndividuallySlowInput(
            Duration.milliseconds(500) to "Welcome!$LF",
            Duration.milliseconds(500) to "Password? ",
            prompt(),
            Duration.milliseconds(500) to "\r",
            Duration.milliseconds(500) to "Correct!$LF",
            baseDelayPerInput = Duration.seconds(1),
            echoInput = true,
        )

        val reader = NonBlockingReader(p.inputStream, logger = this)

        daemon {
            Thread.sleep(5000)
            p.outputStream.enter("password1234")
        }

        expectThat(reader.readLines().joinToString(LF)).isEqualToByteWise("""
            Welcome!
            Password? password1234
            
            Correct!
        
            """.trimIndent())
        expectThat(p.isAlive).isFalse()
    }

    @Test
    fun InMemoryLogger.`should provide access to unfiltered output stream`() {
        val p = withIndividuallySlowInput(
            baseDelayPerInput = Duration.seconds(1),
            echoInput = true,
        )

        p.outputStream.write("Test1234\r".toByteArray())
        p.outputStream.write("Just in case$LF".toByteArray())
        p.outputStream.flush()

        (p.inputStream as SlowInputStream).available()
        expectThat(p.received).isEqualTo("Test1234\rJust in case$LF")
    }

    @Test
    fun InMemoryLogger.`should read zero bytes without exception and delay onexit`() {
        val process = withIndividuallySlowInput(
            Duration.milliseconds(0) to "[  OK  ] Started Update UTMP about System Runlevel Changes.$LF",
            prompt(),
            Duration.milliseconds(100) to "Shutting down",
            baseDelayPerInput = Duration.milliseconds(100),
            echoInput = true,
            exitCode = {
                while (!outputStream.toString().contains("shutdown")) {
                    Duration.milliseconds(100).sleep()
                }
                0
            }).start()

        daemon {
            Duration.seconds(3).sleep()
            process.enter("shutdown")
        }

        val status = process.process({ async(NonInteractive(null)) }, process.terminationLoggingProcessor(this)).waitFor()

        expectThat(status) {
            isA<Success>().exitCode.isEqualTo(0)
        }
    }
}
