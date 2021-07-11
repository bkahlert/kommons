package koodies.exec.mock

import koodies.exec.Process.State.Exited.Failed
import koodies.exec.Process.State.Exited.Succeeded
import koodies.exec.Process.State.Running
import koodies.exec.ProcessingMode
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
import koodies.io.ByteArrayOutputStream
import koodies.jvm.daemon
import koodies.nio.NonBlockingReader
import koodies.test.Slow
import koodies.test.expectThrows
import koodies.test.expecting
import koodies.test.isEqualToByteWise
import koodies.test.test
import koodies.test.testEach
import koodies.text.LineSeparators.LF
import koodies.text.joinLinesToString
import koodies.time.poll
import koodies.time.seconds
import koodies.time.sleep
import koodies.unit.milli
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
                    val p = it(); poll { !p.isAlive }.every(0.5.seconds).forAtMost(5.seconds)
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

            @Slow @TestFactory
            fun `should run`() = test({ RUNNING_EXEC }) {
                expecting { it() } that { starts() }
                expecting { it() } that { hasState<Running> { status.contains("running") } }
                expecting("stays running for 5s") {
                    val p = it(); poll { p.state !is Running }.every(0.5.seconds).forAtMost(5.seconds)
                } that { isFalse() }
            }

            @TestFactory
            fun `should succeed`() = test({ SUCCEEDED_EXEC }) {
                expecting { it() } that { succeeds() }
                expecting { it() } that { hasState<Succeeded> { status.contains("terminated successfully") } }
            }

            @TestFactory
            fun `should fail`() = test({ FAILED_EXEC }) {
                expecting { it() } that { fails() }
                expecting { it() } that { hasState<Failed> { status.contains("terminated with exit code") } }
            }
        }
    }

    @Nested
    inner class WithSlowInputStream {

        @Test
        fun `should provide input correctly`() {
            val slowInputStream = slowInputStream(1.seconds, "Hello$LF", "World!$LF")

            val read = String(slowInputStream.readBytes())

            expectThat(read).isEqualTo("Hello\nWorld!$LF")
        }

        @Test
        fun `should provide input slowly`() {
            val delay = 1.seconds
            val slowInputStream = slowInputStream(delay, "Hello$LF", "World!$LF")

            val duration = measureTime {
                String(slowInputStream.readBytes())
            }
            expectThat(duration).assertThat("is slow") { it > delay }
        }

        @TestFactory
        fun `should provide 'block on prompt' behavior`() = listOf(
            "with echoed input" to true,
            "without echoed input" to false,
        ).testEach("{}") { (_, echoOption) ->
            val byteArrayOutputStream = ByteArrayOutputStream()
            val slowInputStream = slowInputStream(
                Duration.ZERO,
                Duration.ZERO to "Password? ",
                prompt(),
                Duration.ZERO to "\r",
                Duration.ZERO to "Correct!$LF",
                byteArrayOutputStream = byteArrayOutputStream,
                echoInput = echoOption,
            )

            val input = "password1234"
            val output = StringBuilder()
            val start = System.currentTimeMillis()
            while (!slowInputStream.terminated) {
                if ((System.currentTimeMillis() - start).milli.seconds > 0.8.seconds) {
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
        fun `should produce same byte sequence as ByteArrayInputStream`() {
            val input = "A𝌪𝌫𝌬𝌭𝌮Z"
            val inputStream = slowInputStream(2.seconds, input)
            expectThat(input.byteInputStream().readAllBytes()).isEqualTo(inputStream.readAllBytes())
        }

        @TestFactory
        fun `should never apply delay at at end stream`() = "𝌪".let { input ->
            listOf(
                slowInputStream(5.seconds, input, echoInput = true),
                slowInputStream(5.seconds, Duration.ZERO to input, echoInput = true),
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
                expecting { duration } that { isLessThanOrEqualTo(4.seconds) }
            }
        }
    }

    @Nested
    inner class ReadingExitValue {

        @Test
        fun `should return 0 by default`() {
            expectThat(processMock().exitValue()).isEqualTo(0)
        }

        @Test
        fun `should throw exception`() {
            expectThrows<IllegalStateException> {
                processMock { throw IllegalStateException() }.exitValue()
            }
        }

        @Test
        fun `should delay exit`() {
            expecting { measureTime { processMock(exitDelay = 50.milli.seconds).waitFor() } } that {
                isGreaterThan(40.milli.seconds)
                isLessThan(80.milli.seconds)
            }
        }

        @Test
        fun `should return true if process exits in time`() {
            expecting { processMock(exitDelay = 50.milli.seconds).waitFor(100, TimeUnit.MILLISECONDS) } that { isTrue() }
        }

        @Test
        fun `should return false if process not exits in time`() {
            expecting { processMock(exitDelay = 50.milli.seconds).waitFor(25, TimeUnit.MILLISECONDS) } that { isFalse() }
        }

        @Nested
        inner class Liveliness {

            @Nested
            inner class WithDefaultInputStream {

                @Test
                fun `should not be alive if exit is not delayed`() {
                    expectThat(processMock().isAlive).isFalse()
                }

                @Test
                fun `should be alive if exit is delayed`() {
                    expectThat(processMock(exitDelay = 50.milli.seconds).isAlive).isTrue()
                }
            }

            @Nested
            inner class WithSlowInputStream {
                @Test
                fun `should be finished if all read`() {
                    val p = withSlowInput(echoInput = true)
                    expectThat(p.isAlive).isFalse()
                }

                @Test
                fun `should be alive if not all read`() {
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
        fun `should allow SlowInputStream to read process's input stream`() {
            val p = withIndividuallySlowInput(prompt(), echoInput = true)
            with(p.outputStream.writer()) {
                expectThat(p.received).isEmpty()
                expectThat(p.inputStream.available()).isEqualTo(0)

                write("user input")
                flush() // !

                expectThat(p.received).isEqualTo("user input")
                (p.inputStream as SlowInputStream).processInput()
                expectThat(p.inputStream.available()).isEqualTo("user input".length)
            }
        }
    }

    @Slow
    @Test
    fun `should terminate if all output is manually read`() {
        val p = withIndividuallySlowInput(
            0.5.seconds to "Welcome!$LF",
            0.5.seconds to "Password? ",
            prompt(),
            0.5.seconds to "\r",
            0.5.seconds to "Correct!$LF",
            baseDelayPerInput = 1.seconds,
            echoInput = true,
        )

        val reader = NonBlockingReader(p.inputStream)

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
    fun `should terminate if all output is consumed`() {
        val p = withIndividuallySlowInput(
            0.5.seconds to "Welcome!$LF",
            0.5.seconds to "Password? ",
            prompt(),
            0.5.seconds to "\r",
            0.5.seconds to "Correct!$LF",
            baseDelayPerInput = 1.seconds,
            echoInput = true,
        )

        val reader = NonBlockingReader(p.inputStream)

        daemon {
            Thread.sleep(5000)
            p.outputStream.enter("password1234")
        }

        expectThat(reader.readLines().joinLinesToString()).isEqualToByteWise("""
            Welcome!
            Password? password1234
            
            Correct!
        
            """.trimIndent())
        expectThat(p.isAlive).isFalse()
    }

    @Test
    fun `should provide access to unfiltered output stream`() {
        val p = withIndividuallySlowInput(
            baseDelayPerInput = 1.seconds,
            echoInput = true,
        )

        p.outputStream.write("Test1234\r".toByteArray())
        p.outputStream.write("Just in case$LF".toByteArray())
        p.outputStream.flush()

        (p.inputStream as SlowInputStream).available()
        expectThat(p.received).isEqualTo("Test1234\rJust in case$LF")
    }

    @Test
    fun `should read zero bytes without exception and delay onexit`() {
        val process = withIndividuallySlowInput(
            Duration.ZERO to "[  OK  ] Started Update UTMP about System Runlevel Changes.$LF",
            prompt(),
            100.milli.seconds to "Shutting down",
            baseDelayPerInput = 100.milli.seconds,
            echoInput = true,
            exitCode = {
                while (!outputStream.toString().contains("shutdown")) {
                    100.milli.seconds.sleep()
                }
                0
            }).start()

        daemon {
            3.seconds.sleep()
            process.enter("shutdown")
        }

        val status =
            process.process(ProcessingMode { async(NonInteractive(null)) }).waitFor()

        expectThat(status) {
            isA<Succeeded>().exitCode.isEqualTo(0)
        }
    }
}
