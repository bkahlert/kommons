package com.bkahlert.kommons.exec

import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forSingle
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.pathString
import kotlin.io.path.readLines

class SyncExecutorTest {

    @Test fun instantiation() = testAll {
        SyncExecutor(CommandLine.Fixture) shouldBe CommandLine.Fixture.exec
    }

    @Test fun options(@TempDir tempDir: Path) = testAll {
        val out = tempDir / "out.log"
        var configuredWorkingDirectory: Path? = null
        var configuredEnvironment: Map<String, String> = emptyMap()
        SyncExecutor(CommandLine("echo", "test")).invoke(
            workingDirectory = tempDir,
            environment = arrayOf(
                "foo" to "bar",
                "baz" to "",
            ),
            customize = {
                configuredWorkingDirectory = workingDirectory
                configuredEnvironment = environment
                redirectOutput(out.toFile())
            },
        )

        configuredWorkingDirectory?.pathString shouldBe tempDir.pathString
        configuredEnvironment.shouldContainAll(mapOf("foo" to "bar", "baz" to ""))
        out.readLines().shouldContainExactly("test")
    }

    @Test fun invoke() = testAll {
        SyncExecutor(CommandLine("echo", "test")).invoke() should {
            it.shouldBeInstanceOf<Process.ExitState>()
            it.process.inputStream.bufferedReader().readLines().shouldContainExactly("test")
            it.process.errorStream.bufferedReader().readLines().shouldBeEmpty()
            it.exitCode shouldBe 0
        }
    }

    @Test fun logging() = testAll {
        val logger = RecordingLogger()
        SyncExecutor(CommandLine("echo", "test")).logging(logger = { logger }) should {
            it.shouldBeInstanceOf<Process.ExitState>()
            shouldThrow<IOException> { it.process.inputStream.read() }
            shouldThrow<IOException> { it.process.errorStream.read() }
            it.exitCode shouldBe 0
        }
        logger should {
            it.events.forSingle { event ->
                event.message shouldBe "test"
            }
        }
    }

    @Test fun to_string() = testAll {
        SyncExecutor(CommandLine.Fixture).toString().lines() should {
            it.first() shouldBe "SyncExecutor(\"\"\""
            it.drop(1).dropLast(1).joinToString("\n") shouldBe CommandLine.Fixture.toString(pretty = true)
            it.last() shouldBe "\"\"\")"
        }
    }
}
