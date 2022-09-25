package com.bkahlert.kommons.exec

import com.bkahlert.kommons.debug.trace
import com.bkahlert.kommons.logging.SLF4J
import com.bkahlert.kommons.test.testAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit.SECONDS

private val Process.ExitState.x: Any
    get() {
        return this.process.io.trace
    }

class IntegrationTest {

    @Test fun xxx() = testAll {
        val commandLine = CommandLine("echo", "test")

        commandLine.exec().x
    }

    @Test fun command_line() = testAll {
        val commandLine = CommandLine("echo", "test")

        commandLine.exec().x
        commandLine.exec.logging()

        sync { commandLine.exec.async() }
        sync { commandLine.exec.async.logging() }
        sync { commandLine.exec.async { logger.info(it.toString()) } }
        sync { commandLine.exec.async.logging { logger.info(it.toString()) } }
    }

    @Test fun shell_script() = testAll {
        val shellScript = ShellScript {
            """
                echo "some output"
                echo "some error" 1>&2
            """.trimIndent()
        }

        shellScript.exec()
        shellScript.exec.logging()

        sync { shellScript.exec.async() }
        sync { shellScript.exec.async.logging() }
        sync { shellScript.exec.async { logger.info(it.toString()) } }
        sync { shellScript.exec.async.logging { logger.info(it.toString()) } }
    }

    companion object {
        private val logger by SLF4J

        private fun sync(block: () -> Process) {
            block().waitFor(2, SECONDS)
        }
    }
}
