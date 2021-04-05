package koodies.concurrent.process

import koodies.concurrent.daemon
import koodies.concurrent.process.JavaProcessMock.Companion.FAILED_PROCESS
import koodies.concurrent.process.JavaProcessMock.Companion.RUNNING_PROCESS
import koodies.concurrent.process.JavaProcessMock.Companion.SUCCEEDED_PROCESS
import koodies.concurrent.process.JavaProcessMock.Companion.processMock
import koodies.concurrent.process.JavaProcessMock.Companion.withIndividuallySlowInput
import koodies.concurrent.process.JavaProcessMock.Companion.withSlowInput
import koodies.concurrent.process.ManagedProcessMock.Companion.FAILED_MANAGED_PROCESS
import koodies.concurrent.process.ManagedProcessMock.Companion.PREPARED_MANAGED_PROCESS
import koodies.concurrent.process.ManagedProcessMock.Companion.RUNNING_MANAGED_PROCESS
import koodies.concurrent.process.ManagedProcessMock.Companion.SUCCEEDED_MANAGED_PROCESS
import koodies.concurrent.process.Process.ExitState
import koodies.concurrent.process.Process.ExitState.Success
import koodies.concurrent.process.Process.ProcessState
import koodies.concurrent.process.ProcessExitMock.Companion.immediateExit
import koodies.concurrent.process.ProcessExitMock.Companion.immediateSuccess
import koodies.concurrent.process.ProcessingMode.Interactivity.NonInteractive
import koodies.concurrent.process.SlowInputStream.Companion.prompt
import koodies.concurrent.process.SlowInputStream.Companion.slowInputStream
import koodies.concurrent.process.UserInput.enter
import koodies.io.ByteArrayOutputStream
import koodies.io.path.isEqualToByteWise
import koodies.logging.InMemoryLogger
import koodies.nio.NonBlockingReader
import koodies.test.Slow
import koodies.test.assertTimeoutPreemptively
import koodies.test.test
import koodies.test.testEach
import koodies.text.LineSeparators.LF
import koodies.text.joinLinesToString
import koodies.time.poll
import koodies.time.sleep
import koodies.tracing.subTrace
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isLessThanOrEqualTo
import strikt.assertions.isTrue
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.measureTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class JavaProcessMockTest {

    @Execution(CONCURRENT)
    @Nested
    inner class ProcessMockFixtures {

        @Execution(CONCURRENT)
        @Nested
        inner class ProcessMocks {

            @Execution(CONCURRENT)
            @TestFactory
            fun `should run`() = test({ RUNNING_PROCESS }) {
                expect { it().isAlive }.that { isTrue() }
                expect("stays running for 5s") { val p = it(); poll { !p.isAlive }.every(500.milliseconds).forAtMost(5.seconds) }.that { isFalse() }
            }

            @Execution(CONCURRENT)
            @TestFactory
            fun `should have completed successfully`() = test({ SUCCEEDED_PROCESS }) {
                expect { it().isAlive }.that { isFalse() }
                expect { it().exitValue() }.that { isEqualTo(0) }
            }

            @Execution(CONCURRENT)
            @TestFactory
            fun `should have failed`() = test({ FAILED_PROCESS }) {
                expect { it().isAlive }.that { isFalse() }
                expect { it().exitValue() }.that { isEqualTo(-1) }
            }
        }

        @Execution(CONCURRENT)
        @Nested
        inner class ManagedProcessMocks {

            @Execution(CONCURRENT)
            @TestFactory
            fun `should not run`() = test({ PREPARED_MANAGED_PROCESS }) {
                expect { it() }.that { notStarted() }
                expect { it() }.that { hasState<ProcessState.Prepared> { status.contains("not yet started") } }
                expect("stays prepared for 5s") { val p = it(); poll { p.started }.every(500.milliseconds).forAtMost(5.seconds) }.that { isFalse() }
            }

            @Execution(CONCURRENT)
            @TestFactory
            fun `should run`() = test({ RUNNING_MANAGED_PROCESS }) {
                expect { it() }.that { started() }
                expect { it() }.that { hasState<ProcessState.Running> { status.contains("running") } }
                expect("stays running for 5s") {
                    val p = it(); poll { p.state !is ProcessState.Running }.every(500.milliseconds).forAtMost(5.seconds)
                }.that { isFalse() }
            }

            @Execution(CONCURRENT)
            @TestFactory
            fun `should succeed`() = test({ SUCCEEDED_MANAGED_PROCESS }) {
                expect { it() }.that { completesSuccessfully() }
                expect { it() }.that { hasState<Success> { status.contains("terminated successfully") } }
            }

            @Execution(CONCURRENT)
            @TestFactory
            fun `should fail`() = test({ FAILED_MANAGED_PROCESS }) {
                expect { it() }.that { fails() }
                expect { it() }.that { hasState<ExitState.Failure> { status.contains("terminated with exit code") } }
            }
        }
    }

    @Nested
    inner class WithSlowInputStream {

        @Test
        fun InMemoryLogger.`should provide input correctly`() {
            val slowInputStream = slowInputStream(1.seconds, "Hello$LF", "World!$LF")

            assertTimeoutPreemptively(10.seconds) {
                val read = String(slowInputStream.readBytes())

                expectThat(read).isEqualTo("Hello\nWorld!$LF")
            }
        }

        @Test
        fun InMemoryLogger.`should provide input slowly`() {
            val delay = 1.seconds
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
                0.seconds,
                0.seconds to "Password? ",
                prompt(),
                0.seconds to "\r",
                0.seconds to "Correct!$LF",
                byteArrayOutputStream = byteArrayOutputStream,
                echoInput = echoOption,
            )

            val input = "password1234"
            val output = StringBuilder()
            val start = System.currentTimeMillis()
            while (!slowInputStream.terminated) {
                if ((System.currentTimeMillis() - start).milliseconds > .8.seconds) {
                    byteArrayOutputStream.write("password1234\r".toByteArray())
                    byteArrayOutputStream.flush()
                }
                val available = slowInputStream.available()
                if (available > 0) {
                    val byteArray = ByteArray(available)
                    val read = slowInputStream.read(byteArray, 0, available)
                    expect { read }.that { isGreaterThan(0) }
                    output.append(String(byteArray))
                }
                Thread.sleep(10)
            }
            if (echoOption) expect { output }.that { isEqualToByteWise("Password? $input\r\rCorrect!$LF") }
            else expect { output }.that { isEqualToByteWise("Password? \rCorrect!$LF") }
        }


        @Test
        fun InMemoryLogger.`should produce same byte sequence as ByteArrayInputStream`() {
            val input = "A𝌪𝌫𝌬𝌭𝌮Z"
            val inputStream = slowInputStream(2.seconds, input)
            expectThat(input.byteInputStream().readAllBytes()).isEqualTo(inputStream.readAllBytes())
        }

        @TestFactory
        fun InMemoryLogger.`should never apply delay at at end stream`() = "𝌪".let { input ->
            listOf(
                slowInputStream(5.seconds, input, echoInput = true),
                slowInputStream(5.seconds, 0.seconds to input, echoInput = true),
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
                expect { duration }.that { isLessThanOrEqualTo(2.seconds) }
            }
        }
    }

    @Nested
    inner class ReadingExitValue {

        @Nested
        inner class UsingExitState {
            @Test
            fun InMemoryLogger.`should return mocked exit`() {
                val p = processMock(processExit = { immediateExit(0) })
                expectThat(p.exitValue()).isEqualTo(0)
            }

            @Test
            fun InMemoryLogger.`should throw on exception`() {
                val p = processMock(processExit = { throw IllegalStateException() })

                expectCatching {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    p.exitValue()
                }.isFailure().isA<IllegalStateException>()
            }
        }

        @Slow
        @Isolated // benchmark
        @Nested
        inner class UsingWaitFor {
            @Test
            fun InMemoryLogger.`should return mocked exit code`() {
                val p = processMock(processExit = { immediateExit(0) })

                val exitValue = p.waitFor()

                expectThat(exitValue).isEqualTo(0)
            }

            @Test
            fun InMemoryLogger.`should throw on exception`() {
                val p = processMock(processExit = { throw IllegalStateException() })

                expectCatching {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    p.waitFor()
                }.isFailure().isA<IllegalStateException>()
            }

            @Test
            fun InMemoryLogger.`should delay exit`() {
                val p = processMock(processExit = { ProcessExitMock.delayedExit(0, 50.milliseconds) })
                val start = System.currentTimeMillis()

                val exitValue = p.waitFor()

                expectThat(exitValue).isEqualTo(0)
                expectThat(System.currentTimeMillis() - start).isGreaterThan(40).isLessThan(80)
            }

            @Test
            fun InMemoryLogger.`should return true if process exits in time`() {
                val p = processMock(processExit = { ProcessExitMock.delayedExit(0, 50.milliseconds) })

                val returnValue = p.waitFor(100, TimeUnit.MILLISECONDS)

                expectThat(returnValue).isTrue()
            }

            @Test
            fun InMemoryLogger.`should return false if process not exits in time`() {
                val p = processMock(processExit = { ProcessExitMock.delayedExit(0, 50.milliseconds) })

                val returnValue = p.waitFor(25, TimeUnit.MILLISECONDS)

                expectThat(returnValue).isTrue()
            }
        }

        @Nested
        inner class Liveliness {

            @Nested
            inner class WithDefaultInputStream {
                @Test
                fun InMemoryLogger.`should be finished if exit is immediate`() {
                    val p = processMock(processExit = { immediateExit(0) })
                    expectThat(p.isAlive).isFalse()
                }

                @Test
                fun InMemoryLogger.`should be alive if exit is delayed`() {
                    val p = processMock(processExit = { ProcessExitMock.delayedExit(0, 50.milliseconds) })
                    expectThat(p.isAlive).isTrue()
                }
            }

            @Nested
            inner class WithSlowInputStream {
                @Test
                fun InMemoryLogger.`should be finished if all read`() {
                    val p = withSlowInput(echoInput = true, processExit = { immediateExit(0) })
                    expectThat(p.isAlive).isFalse()
                }

                @Test
                fun InMemoryLogger.`should be alive if not all read`() {
                    val p = withSlowInput(
                        "unread",
                        echoInput = true,
                        processExit = { immediateExit(0) },
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
            val p = withIndividuallySlowInput(prompt(), echoInput = true, processExit = { immediateSuccess() })
            with(p.outputStream.writer()) {
                expectThat(p.received).isEmpty()
                expectThat(p.inputStream.available()).isEqualTo(0)

                write("user input")
                flush() // !

                expectThat(p.received).isEqualTo("user input")
                subTrace<Any?>("???") {
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
            500.milliseconds to "Welcome!$LF",
            500.milliseconds to "Password? ",
            prompt(),
            500.milliseconds to "\r",
            500.milliseconds to "Correct!$LF",
            baseDelayPerInput = 1.seconds,
            echoInput = true,
            processExit = { immediateSuccess() },
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
            500.milliseconds to "Welcome!$LF",
            500.milliseconds to "Password? ",
            prompt(),
            500.milliseconds to "\r",
            500.milliseconds to "Correct!$LF",
            baseDelayPerInput = 1.seconds,
            echoInput = true,
            processExit = { immediateSuccess() },
        )

        val reader = NonBlockingReader(p.inputStream, logger = this)

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
    fun InMemoryLogger.`should provide access to unfiltered output stream`() {
        val p = withIndividuallySlowInput(
            baseDelayPerInput = 1.seconds,
            echoInput = true,
            processExit = { immediateSuccess() },
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
            0.milliseconds to "[  OK  ] Started Update UTMP about System Runlevel Changes.$LF",
            prompt(),
            100.milliseconds to "Shutting down",
            baseDelayPerInput = 100.milliseconds,
            echoInput = true,
            processExit = {
                object : ProcessExitMock(0, Duration.ZERO) {
                    override fun invoke(): Int {
                        while (!outputStream.toString().contains("shutdown")) {
                            100.milliseconds.sleep()
                        }
                        return 0
                    }

                    override fun invoke(timeout: Duration): Boolean {
                        while (!outputStream.toString().contains("shutdown")) {
                            100.milliseconds.sleep()
                        }
                        return true
                    }
                }
            }).start()

        daemon {
            3.seconds.sleep()
            process.enter("shutdown")
        }

        val status = process.process({ async(NonInteractive(null)) }, process.terminationLoggingProcessor(this)).waitForTermination()

        expectThat(status) {
            isA<Success>().exitCode.isEqualTo(0)
        }
    }
}
