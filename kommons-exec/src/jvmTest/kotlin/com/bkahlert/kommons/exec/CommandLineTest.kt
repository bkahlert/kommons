package com.bkahlert.kommons.exec

import com.bkahlert.kommons.quoted
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class CommandLineTest {

    @Test fun instantiation() = testAll {
        CommandLine("command") shouldBe CommandLine(command = "command", arguments = emptyList())
        CommandLine("command", "arg") shouldBe CommandLine(command = "command", arguments = listOf("arg"))
        CommandLine("command", "arg1", "arg2") shouldBe CommandLine(command = "command", arguments = listOf("arg1", "arg2"))
    }

    @Test fun list() = testAll {
        CommandLine.Fixture.shouldContainExactly("command", "-a", "--bee", "c", " x'\ty'\n💤".quoted)
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


internal val CommandLine.Companion.Fixture
    get() = CommandLine("command", "-a", "--bee", "c", " x'\ty'\n💤".quoted)
