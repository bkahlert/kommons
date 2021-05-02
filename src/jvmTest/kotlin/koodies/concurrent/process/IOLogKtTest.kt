package koodies.concurrent.process

import koodies.concurrent.script
import koodies.exec.alive
import koodies.io.path.Locations.Temp
import koodies.shell.ShellScript
import koodies.test.HtmlFile
import koodies.test.Slow
import koodies.test.UniqueId
import koodies.test.copyToDirectory
import koodies.test.testEach
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.first
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.second

class IOLogKtTest {

    @Nested
    inner class OutputFn {

        private val echoingCommands =
            "echo \"test output 1\"; sleep 1; >&2 echo \"test error 1\"; sleep 1; echo \"test output 2\"; sleep 1; >&2 echo \"test error 2\""

        private val startedProcess
            get() = ShellScript { !echoingCommands }.toExec(false, emptyMap(), Temp, null).processSynchronously(processor = {})
        private val terminatedProcess
            get() = startedProcess.processSynchronously(processor = {}).also { require(it.successful != null) { "Process not terminated" } }

        @Slow
        @TestFactory
        fun `should get output`() = testEach(
            { startedProcess },
            { terminatedProcess },
        ) {
            expecting { it().let { p -> p to p.output() } } that {
                first.not { alive }
                second.isEqualTo("test output 1\ntest output 2")
            }
        }

        @Test
        fun `should transform each line`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val name = HtmlFile.copyToDirectory(this)
            val script = script { !"cat $name" }
            expectThat(script.output { length + 1 }.sum() - 1).isEqualTo(HtmlFile.text.length) // +1 = line ending
        }

        @Test
        fun `should drop null mapped lines`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val name = HtmlFile.copyToDirectory(this)
            val script = script { !"cat $name" }
            expectThat(script.output { length.takeIf { it > 10 } }).hasSize(3)
        }
    }
}
