package com.bkahlert.kommons.exec.mock

import com.bkahlert.kommons.LineSeparators
import com.bkahlert.kommons.LineSeparators.LF
import com.bkahlert.kommons.exec.Exec
import com.bkahlert.kommons.exec.Process.State.Exited.Failed
import com.bkahlert.kommons.exec.Process.State.Exited.Succeeded
import com.bkahlert.kommons.exec.Process.State.Running
import com.bkahlert.kommons.exec.ProcessingMode
import com.bkahlert.kommons.exec.enter
import com.bkahlert.kommons.exec.exitCode
import com.bkahlert.kommons.exec.fails
import com.bkahlert.kommons.exec.hasState
import com.bkahlert.kommons.exec.mock.ExecMock.Companion.FAILED_EXEC
import com.bkahlert.kommons.exec.mock.ExecMock.Companion.RUNNING_EXEC
import com.bkahlert.kommons.exec.mock.ExecMock.Companion.SUCCEEDED_EXEC
import com.bkahlert.kommons.exec.mock.JavaProcessMock.Companion.FAILING_PROCESS
import com.bkahlert.kommons.exec.mock.JavaProcessMock.Companion.RUNNING_PROCESS
import com.bkahlert.kommons.exec.mock.JavaProcessMock.Companion.SUCCEEDING_PROCESS
import com.bkahlert.kommons.exec.mock.JavaProcessMock.Companion.processMock
import com.bkahlert.kommons.exec.mock.JavaProcessMock.Companion.withIndividuallySlowInput
import com.bkahlert.kommons.exec.mock.JavaProcessMock.Companion.withSlowInput
import com.bkahlert.kommons.exec.mock.SlowInputStream.Companion.prompt
import com.bkahlert.kommons.exec.mock.SlowInputStream.Companion.slowInputStream
import com.bkahlert.kommons.exec.process
import com.bkahlert.kommons.exec.starts
import com.bkahlert.kommons.exec.status
import com.bkahlert.kommons.exec.succeeds
import com.bkahlert.kommons.io.ByteArrayOutputStream
import com.bkahlert.kommons.nio.InputStreamReader
import com.bkahlert.kommons.runtime.daemon
import com.bkahlert.kommons.test.Slow
import com.bkahlert.kommons.test.expectThrows
import com.bkahlert.kommons.test.expecting
import com.bkahlert.kommons.test.isEqualToByteWise
import com.bkahlert.kommons.test.testEachOld
import com.bkahlert.kommons.test.testOld
import com.bkahlert.kommons.time.poll
import com.bkahlert.kommons.time.sleep
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class JavaExecMockTest {

    @Nested
    inner class ProcessMockFixtures {

        @Nested
        inner class ProcessMocks {

            @TestFactory
            fun `should run`() = testOld({ RUNNING_PROCESS }) {
                expecting { it().isAlive } that { isTrue() }
                expecting("stays running for 5s") {
                    val p = it(); poll { !p.isAlive }.every(0.5.seconds).forAtMost(5.seconds)
                } that { isFalse() }
            }

            @TestFactory
            fun `should have completed successfully`() = testOld({ SUCCEEDING_PROCESS }) {
                expecting { it().isAlive } that { isFalse() }
                expecting { it().exitValue() } that { isEqualTo(0) }
            }

            @TestFactory
            fun `should have failed`() = testOld({ FAILING_PROCESS }) {
                expecting { it().isAlive } that { isFalse() }
                expecting { it().exitValue() } that { isEqualTo(42) }
            }
        }

        @Nested
        inner class ExecMocks {

            @Slow @TestFactory
            fun `should run`() = testOld({ RUNNING_EXEC }) {
                expecting { it() } that { starts() }
                expecting { it() } that { hasState<Running> { status.contains("running") } }
                expecting("stays running for 5s") {
                    val p = it(); poll { p.state !is Running }.every(0.5.seconds).forAtMost(5.seconds)
                } that { isFalse() }
            }

            @TestFactory
            fun `should succeed`() = testOld({ SUCCEEDED_EXEC }) {
                expecting { it() } that { succeeds() }
                expecting { it() } that { hasState<Succeeded> { status.contains("terminated successfully") } }
            }

            @TestFactory
            fun `should fail`() = testOld({ FAILED_EXEC }) {
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
        ).testEachOld("*") { (_, echoOption) ->
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
                if ((System.currentTimeMillis() - start).milliseconds > 0.8.seconds) {
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
            ).testEachOld("*") { inputStream ->
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
            expecting { measureTime { processMock(exitDelay = 50.milliseconds).waitFor() } } that {
                isGreaterThan(40.milliseconds)
                isLessThan(80.milliseconds)
            }
        }

        @Test
        fun `should return true if process exits in time`() {
            expecting { processMock(exitDelay = 50.milliseconds).waitFor(100, TimeUnit.MILLISECONDS) } that { isTrue() }
        }

        @Test
        fun `should return false if process not exits in time`() {
            expecting { processMock(exitDelay = 50.milliseconds).waitFor(25, TimeUnit.MILLISECONDS) } that { isFalse() }
        }

        @Nested
        inner class Liveliness {

            @Nested
            inner class WithDefaultInputStream {

                @Test
                fun `should not be alive if enough time has passed`() {
                    expectThat(processMock(exitDelay = Duration.ZERO).isAlive).isFalse()
                }

                @Test
                fun `should be alive if too little time has passed`() {
                    expectThat(processMock(exitDelay = 100.milliseconds).isAlive).isTrue()
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

        val reader = InputStreamReader(p.inputStream)

        daemon {
            Thread.sleep(5000)
            p.outputStream.enter("password1234")
        }

        expectThat(reader.readLines().joinToString(LineSeparators.Default)).isEqualToByteWise(
            """
            Welcome!
            Password? password1234

            Correct!

            """.trimIndent()
        )
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

    @Slow @Test
    fun `should read zero bytes without exception and delay onexit`() {
        @Suppress("SpellCheckingInspection")
        val exec: Exec = withIndividuallySlowInput(
            Duration.ZERO to "[  OK  ] Started Update UTMP about System Runlevel Changes.$LF",
            prompt(),
            100.milliseconds to "Shutting down",
            baseDelayPerInput = 100.milliseconds,
            echoInput = true,
            exitCode = {
                while (!outputStream.toString().contains("shutdown")) {
                    100.milliseconds.sleep()
                }

                0
            }).start()

        daemon {
            2.seconds.sleep()
            exec.enter("shutdown")
        }

        val status = exec.process(ProcessingMode(async = true)).waitFor()

        expectThat(status) {
            isA<Succeeded>().exitCode.isEqualTo(0)
        }
    }
}
