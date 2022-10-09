package com.bkahlert.kommons.exec

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class ShellScriptTest {

    private val shellScript get() = ShellScript("echo test")

    @Test fun instantiation() = testAll {
        ShellScript("echo foo", "echo bar") shouldBe ShellScript(
            """
                echo foo
                echo bar
            """.trimIndent()
        )
    }

    @Test fun content() = testAll {
        ShellScript("echo foo", "echo bar").content shouldBe """
                echo foo
                echo bar
            """.trimIndent()
    }

    @Test fun to_command_line() = testAll {
        listOf(
            CommandLine("/bin/sh", "-c", "echo test"),
            CommandLine("cmd.exe", "/X", "/C", "echo test"),
        ).shouldContain(shellScript.toCommandLine())
    }

    @Test fun exec() = testAll {
        shellScript.exec should {
            it shouldBe shellScript.toCommandLine().exec
        }
    }

    @Test fun equality() = testAll {
        ShellScript("name", "echo test") shouldBe ShellScript("name", "echo test")
        ShellScript("name", "echo test") shouldBe ShellScript("name", "echo test")
        ShellScript("name", "echo test") shouldNotBe ShellScript("name2", "echo test")
        ShellScript("name", "echo test") shouldNotBe ShellScript("name", "echo test2")
    }

    @Test fun to_string() = testAll {
        shellScript.toString() shouldBe "echo test"
    }
}
