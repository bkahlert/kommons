package com.bkahlert.kommons.exec

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.pathString
import kotlin.io.path.readLines

class ProcessBuildersKtTest {

    @Test fun command_line() = testAll {
        ProcessBuilder().commandLine shouldBe null
        ProcessBuilder("command").commandLine shouldBe CommandLine("command")
        ProcessBuilder("command", "arg").commandLine shouldBe CommandLine("command", "arg")
        ProcessBuilder("command", "arg1", "arg2").commandLine shouldBe CommandLine("command", "arg1", "arg2")
    }

    @Test fun environment() = testAll {
        ProcessBuilder() should {
            it.environment shouldBeSameInstanceAs it.environment()
        }
    }

    @Test fun working_directory(@TempDir tempDir: Path) = testAll {
        ProcessBuilder().apply {
            workingDirectory = tempDir
        }.directory().path shouldBe tempDir.pathString
    }

    @Test fun start(@TempDir tempDir: Path) = testAll {
        val out = tempDir / "out.log"
        var configuredWorkingDirectory: Path? = null
        var configuredEnvironment: Map<String, String> = emptyMap()
        ProcessBuilder(shellCommand, *shellArguments, "echo test").start(
            tempDir,
            "foo" to "bar",
            "baz" to "",
        ) {
            configuredWorkingDirectory = workingDirectory
            configuredEnvironment = environment
            redirectOutput(out.toFile())
        }.waitFor()

        configuredWorkingDirectory?.pathString shouldBe tempDir.pathString
        configuredEnvironment.shouldContainAll(mapOf("foo" to "bar", "baz" to ""))
        out.readLines().shouldContainExactly("test")
    }
}
