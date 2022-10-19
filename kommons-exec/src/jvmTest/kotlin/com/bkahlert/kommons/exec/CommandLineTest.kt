package com.bkahlert.kommons.exec

import com.bkahlert.kommons.quoted
import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.pathString

class CommandLineTest {

    @Test fun instantiation() = testAll {
        CommandLine("command") shouldBe CommandLine(command = "command", arguments = emptyList())
        CommandLine("command", "arg") shouldBe CommandLine(command = "command", arguments = listOf("arg"))
        CommandLine("command", "arg1", "arg2") shouldBe CommandLine(command = "command", arguments = listOf("arg1", "arg2"))
    }

    @Test fun instantiation(@TempDir tempDir: Path) = testAll {
        CommandLine(TestClassWithMain::class, "foo", "bar", javaBinary = tempDir / "java", classPath = "foo/bar:baz")
            .shouldContainExactly(tempDir.resolve("java").pathString, "-cp", "foo/bar:baz", TestClassWithMain::class.qualifiedName, "foo", "bar")

        CommandLine(TestClassWithMain::class, "foo", "bar").exec().readLinesOrThrow() should {
            it shouldHaveSize 3
            it[0] shouldBe System.getProperty("java.home")
            it[1] shouldBe System.getProperty("java.class.path")
            it[2] shouldBe "foo,bar"
        }

        shouldThrow<IOException> {
            CommandLine(TestClassWithoutMain::class, "foo", "bar").exec().readLinesOrThrow()
        }.message.shouldContain("main method")
    }

    @Test fun list() = testAll {
        CommandLine.Fixture.shouldContainExactly("command", "-a", "--bee", "c", " x'\ty'\n💤".quoted)
    }

    @Test fun plus() = testAll {
        CommandLine("foo", "bar") + "baz" shouldBe CommandLine("foo", "bar", "baz")
    }

    @Test fun exec() = testAll {
        CommandLine.Fixture.exec shouldBe SyncExecutor(CommandLine.Fixture)
    }

    @Test fun equality() = testAll {
        CommandLine("echo", "test") shouldBe CommandLine("echo", "test")
        CommandLine("echo", "test") shouldNotBe CommandLine("echo", "test2")
    }

    @Test fun to_string() = testAll {
        CommandLine.Fixture.toString() shouldBe CommandLine.Fixture.toString(pretty = false)

        CommandLine.Fixture.toString(pretty = false).shouldBeIn(
            """
                'command' '-a' '--bee' 'c' '" x'"'"'\ty'"'"'\n💤"'
            """.trimIndent(),
            """
                command -a --bee c " x'\ty'\n💤"
            """.trimIndent(),
        )

        CommandLine.Fixture.toString(pretty = true).shouldBeIn(
            """
                'command' \
                '-a' \
                '--bee' \
                'c' \
                '" x'"'"'\ty'"'"'\n💤"'
            """.trimIndent(),
            """
                command \
                -a \
                --bee \
                c \
                " x'\ty'\n💤"
            """.trimIndent()
        )
    }

    @Test fun parse() = testAll(
        CommandLine.parse("echo test"),
        CommandLine.parse("'echo' 'test'"),
        CommandLine.parse(
            """
                echo \
                test
            """.trimIndent()
        ),
        CommandLine.parse(
            """
                'echo' \
                'test'
            """.trimIndent()
        ),
        CommandLine.parse("echo test\n"),
        CommandLine.parse(
            """
                echo \
                \
                test
            """.trimIndent()
        ),
    ) {
        it shouldBe CommandLine("echo", "test")
    }
}


class TestClassWithMain {

    companion object {
        @JvmStatic
        fun main(vararg args: String) {
            println(System.getProperty("java.home"))
            println(System.getProperty("java.class.path"))
            println(args.joinToString(","))
        }
    }
}

class TestClassWithoutMain

internal val CommandLine.Companion.Fixture
    get() = CommandLine("command", "-a", "--bee", "c", " x'\ty'\n💤".quoted)
