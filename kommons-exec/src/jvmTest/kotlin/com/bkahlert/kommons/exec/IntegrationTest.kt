package com.bkahlert.kommons.exec

import com.bkahlert.kommons.exec.Process.Failed
import com.bkahlert.kommons.exec.Process.Succeeded
import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forSingle
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.io.IOException


class IntegrationTest {

    @Test fun TODO() = testAll {
        val commandLine = CommandLine(shellCommand, *shellArguments, "echo test")

        val exec = commandLine.exec()
        exec.readLinesOrThrow()
            .shouldContainExactly("test")
    }

    @Test fun command_line() = testAll {
        val commandLine = CommandLine(shellCommand, *shellArguments, "echo test")

        commandLine.exec().readLinesOrThrow()
            .shouldContainExactly("test")

        commandLine.exec.logging()
            .shouldBeInstanceOf<Succeeded>()
    }

    @Test fun shell_script() = testAll {
        val shellScript = ShellScript("echo output")

        shellScript.exec().readLinesOrThrow()
            .shouldContainExactly("output")

        shellScript.exec.logging()
            .shouldBeInstanceOf<Succeeded>()
    }

    @Test fun error_handling() = testAll {
        val logger = RecordingLogger()
        when (val exitState = ShellScript("exit 42").exec()) {
            is Succeeded -> logger.info("Process took ${exitState.runtime}")
            is Failed -> logger.error("Process ${exitState.process.pid} failed")
        }
        logger should {
            it.events.forSingle { event ->
                event.message shouldMatchGlob "Process* failed"
            }
        }

        shouldThrow<IOException> {
            ShellScript("exit 42").exec().readLinesOrThrow()
        }.message shouldMatchGlob "Process* terminated after * with exit code 42"
    }
}
