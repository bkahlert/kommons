package com.bkahlert.kommons.exec

import com.bkahlert.kommons.Now
import com.bkahlert.kommons.minus
import com.bkahlert.kommons.quoted
import com.bkahlert.kommons.randomString
import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.date.shouldNotBeAfter
import io.kotest.matchers.date.shouldNotBeBefore
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ProcessTest {

    @Test fun instantiate() = testAll {
        Process(ProcessBuilder("echo", "test")) should {
            it.waitFor() shouldBe 0
        }

        shouldThrow<IOException> {
            Process(ProcessBuilder("invalid-command"))
        }
    }

    @Test fun start() = testAll {
        val start = Now
        Process(ProcessBuilder("echo", "test")).start should {
            it.shouldNotBeBefore(start)
            it.shouldNotBeAfter(Now)
        }
    }

    @Test fun output_stream() = testAll {
        val input = randomString()
        Process(ProcessBuilder("cat")) should {
            it.outputStream.write(input.encodeToByteArray())
            it.outputStream.close()
            it.waitFor() shouldBe 0
            it.inputStream.readBytes().decodeToString() shouldBe input
        }
    }

    @Test fun input_stream() = testAll {
        val input = randomString()
        val process = Process(ProcessBuilder(ShellScript("echo ${input.quoted}").toCommandLine()))
            .also { it.waitFor() }
        process.inputStream.readBytes().decodeToString() shouldContain input
    }

    @Test fun error_stream() = testAll {
        val input = randomString()
        val process = Process(ProcessBuilder(ShellScript("echo ${input.quoted} 1>&2").toCommandLine()))
            .also { it.waitFor() }
        process.errorStream.readBytes().decodeToString() shouldContain input
    }

    @Test fun wait_for() = testAll {
        Process.succeeding().waitFor() shouldBe 0
        Process.failing().waitFor() shouldBe 42
    }

    @Test fun exit_value() = testAll {
        Process.succeeded().exitValue() shouldBe 0
        Process.failed().exitValue() shouldBe 42
    }

    @Test fun destroy() = testAll {
        Process.running().apply {
            destroy()
            waitFor()
        } should {
            it.exitValue() shouldNotBe 0
            Now.minus(it.start) shouldBeLessThan 5.seconds
        }
    }

    @Test fun pid() = testAll {
        val runningProcess = Process.running()
        shouldNotThrowAny { runningProcess.pid }
        runningProcess.destroy()

        val succeededProcess = Process.succeeded()
        shouldNotThrowAny { succeededProcess.pid }

        val failedProcess = Process.failed()
        shouldNotThrowAny { failedProcess.pid }
    }

    @Test fun to_string() = testAll {
        Process.succeeded().toString() shouldMatchGlob """
            Process {**
                state: "succeeded",
                commandLine: *
            }
        """.trimIndent()
    }

    @Test fun state() = testAll {
        val runningProcess = Process.running()
        runningProcess.state.shouldBeInstanceOf<Process.State.Running>() should {
            it.process shouldBeSameInstanceAs runningProcess
            it.start.minus(it.process.start) shouldBeLessThan 10.milliseconds
            it.status shouldMatchGlob "Process* is running"
            it.toString() shouldBe it.status
        }
        runningProcess.destroy()

        val succeededProcess = Process.succeeded()
        succeededProcess.state.shouldBeInstanceOf<Process.State.Terminated.Succeeded>() should {
            it.process shouldBeSameInstanceAs succeededProcess
            it.start.minus(it.process.start) shouldBeLessThan 10.milliseconds
            it.end shouldNotBeAfter Now
            it.runtime shouldBe it.end.minus(it.start)
            it.exitCode shouldBe 0
            it.status shouldMatchGlob "Process* terminated successfully within *"
            it.toString() shouldBe it.status
        }

        val failedProcess = Process.failed()
        failedProcess.state.shouldBeInstanceOf<Process.State.Terminated.Failed>() should {
            it.process shouldBeSameInstanceAs failedProcess
            it.start.minus(it.process.start) shouldBeLessThan 10.milliseconds
            it.end shouldNotBeAfter Now
            it.runtime shouldBe it.end.minus(it.start)
            it.exitCode shouldBe 42
            it.commandLine shouldBe ShellScript("exit 42").toCommandLine()
            it.status shouldMatchGlob "Process* terminated after * with exit code 42; command line: *exit 42*"
            it.toString() shouldBe it.status
        }
    }
}

internal fun Process.Companion.running(): Process =
    Process(ProcessBuilder(ShellScript("sleep 5").toCommandLine()))

internal fun Process.Companion.succeeding(): Process =
    Process(ProcessBuilder(ShellScript("exit 0").toCommandLine()))

internal fun Process.Companion.succeeded(): Process =
    succeeding().apply { waitFor() }

internal fun Process.Companion.failing(): Process =
    Process(ProcessBuilder(ShellScript("exit 42").toCommandLine()))

internal fun Process.Companion.failed(): Process =
    failing().apply { waitFor() }
