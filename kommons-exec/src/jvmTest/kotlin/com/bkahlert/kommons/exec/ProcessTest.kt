package com.bkahlert.kommons.exec

import com.bkahlert.kommons.Now
import com.bkahlert.kommons.Timestamp
import com.bkahlert.kommons.randomString
import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons.text.LineSeparators.removeTrailingLineSeparator
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.codehaus.plexus.util.Os
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ProcessTest {

    @Test fun instantiate() = testAll {
        Process(ProcessBuilder(shellCommand, *shellArguments, "echo test")) should {
            it.waitFor() shouldBe 0
        }

        shouldThrow<IOException> {
            Process(ProcessBuilder("invalid-command"))
        }
    }

    @Test fun start() = testAll {
        val start = Now
        Process(ProcessBuilder(shellCommand, *shellArguments, "echo test")).start should {
            it.toEpochMilliseconds() shouldBeGreaterThanOrEqualTo start.toEpochMilliseconds()
            it.toEpochMilliseconds() shouldBeLessThanOrEqualTo Timestamp
        }
    }

    @Test fun output_stream() = testAll {
        val input = randomString()
        Process(ProcessBuilder(shellCommand, *shellArguments, catCommand)) should {
            it.outputStream.write(input.encodeToByteArray())
            it.outputStream.close()
            it.waitFor() shouldBe 0
            it.inputStream.bufferedReader().readLines().shouldContainExactly(input)
        }
    }

    @Test fun input_stream() = testAll {
        val input = randomString()
        val process = Process(ProcessBuilder(shellCommand, *shellArguments, "echo $input"))
            .also { it.waitFor() }
        process.inputStream.bufferedReader().readLines().shouldContainExactly(input)
    }

    @Test fun error_stream() = testAll {
        val input = randomString()
        val process = Process(ProcessBuilder(shellCommand, *shellArguments, "1>&2 echo $input"))
            .also { it.waitFor() }
        process.errorStream.bufferedReader().readLines().shouldContainExactly(input)
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
        val runningProcess = Process.running()
        runningProcess.toString().replace('\n', ' ').replace(Regex("\\s+"), " ") shouldMatchGlob """
            Process {* state: "running", commandLine: * }
        """.trimIndent()
        runningProcess.destroy()

        val succeededProcess = Process.succeeded()
        succeededProcess.toString().replace('\n', ' ').replace(Regex("\\s+"), " ") shouldMatchGlob """
            Process {* state: "succeeded", commandLine: * }
        """.trimIndent()

        val failedProcess = Process.failed()
        failedProcess.toString().replace('\n', ' ').replace(Regex("\\s+"), " ") shouldMatchGlob """
            Process {* state: "failed", commandLine: * }
        """.trimIndent()
    }

    @Test fun exit_state() = testAll {
        val runningProcess = Process.running()
        runningProcess.exitState shouldBe null
        runningProcess.destroy()

        val succeededProcess = Process.succeeded()
        succeededProcess.exitState.shouldBeInstanceOf<Process.Succeeded>() should {
            it.process shouldBeSameInstanceAs succeededProcess
            it.start.minus(it.process.start) shouldBeLessThan 10.milliseconds
            it.end.toEpochMilliseconds() shouldBeLessThanOrEqualTo Timestamp
            it.runtime shouldBe it.end.minus(it.start)
            it.exitCode shouldBe 0
            it.status shouldMatchGlob "Process* terminated successfully within *"
            it.toString() shouldBe it.status
            it.readBytesOrThrow().decodeToString().removeTrailingLineSeparator() shouldBe "output"
            it.readTextOrThrow().removeTrailingLineSeparator() shouldBe "output"
            it.readLinesOrThrow().shouldContainExactly("output")
        }

        val failedProcess = Process.failed()
        failedProcess.exitState.shouldBeInstanceOf<Process.Failed>() should {
            it.process shouldBeSameInstanceAs failedProcess
            it.start.minus(it.process.start) shouldBeLessThan 10.milliseconds
            it.end.toEpochMilliseconds() shouldBeLessThanOrEqualTo Timestamp
            it.runtime shouldBe it.end.minus(it.start)
            it.exitCode shouldBe 42
            it.status shouldMatchGlob "Process* terminated after * with exit code 42"
            it.toString() shouldBe it.status
            listOf(
                { it.readBytesOrThrow() },
                { it.readTextOrThrow() },
                { it.readLinesOrThrow() },
            ).forAll { read ->
                shouldThrow<IOException> { read() }.message shouldMatchGlob """
                    Process* terminated after * with exit code 42:
                    error
                """.trimIndent()
            }
        }
    }
}

private val isWindows = Os.isFamily(Os.FAMILY_WINDOWS)
private val commandSeparator = if (isWindows) "&" else "\n"
private fun commands(vararg commands: String) = commands.joinToString(commandSeparator)

@Suppress("SpellCheckingInspection")
private val catCommand = if (isWindows) "findstr \"^\"" else "cat"
private fun sleepCommand(seconds: Long) = if (isWindows) "ping 127.0.0.1 -n $seconds > nul" else "sleep $seconds"

internal val shellCommand: String = ShellScript.ShellCommandLine.command
internal val shellArguments: Array<String> = ShellScript.ShellCommandLine.arguments.toTypedArray()


internal fun Process.Companion.running(duration: Duration = 5.seconds): Process =
    Process(ProcessBuilder(ShellScript(sleepCommand(duration.inWholeSeconds)).toCommandLine()).apply {
        redirectErrorStream(true)
    })

internal fun Process.Companion.succeeding(): Process =
    Process(ProcessBuilder(ShellScript(commands("echo output", "exit 0")).toCommandLine()))

internal fun Process.Companion.succeeded(): Process =
    succeeding().apply { waitFor() }

internal fun Process.Companion.failing(): Process =
    Process(ProcessBuilder(ShellScript(commands("echo output", "1>&2 echo error", "exit 42")).toCommandLine()))

internal fun Process.Companion.failed(): Process =
    failing().apply { waitFor() }
