package com.bkahlert.kommons.exec

import com.bkahlert.kommons.test.testAll
import org.junit.jupiter.api.Test

class IntegrationTest {

    @Test fun command_line() = testAll {
        CommandLine("echo", "test").exec.logging()
    }

    @Test fun shell_script() = testAll {
        ShellScript {
            """
                echo "some output"
                echo "some error" 1>&2
            """.trimIndent()
        }.exec.logging()
    }
}
