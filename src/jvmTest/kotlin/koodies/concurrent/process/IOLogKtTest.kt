package koodies.concurrent.process

import koodies.concurrent.process
import koodies.concurrent.script
import koodies.exec.alive
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
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.first
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.second

@Execution(CONCURRENT)
class IOLogKtTest {

    @Nested
    inner class OutputFn {

        private val echoingCommands =
            "echo \"test output 1\"; sleep 1; >&2 echo \"test error 1\"; sleep 1; echo \"test output 2\"; sleep 1; >&2 echo \"test error 2\""

        private val notStartedProcess
            get() = process(ShellScript { !echoingCommands }).also { require(!it.started) { "Process started" } }
        private val startedProcess
            get() = notStartedProcess.processSynchronously(processor = {}).also { require(it.started) { "Process not started" } }
        private val terminatedProcess
            get() = startedProcess.processSynchronously(processor = {}).also { require(it.successful != null) { "Process not terminated" } }

        @Slow
        @TestFactory
        fun `should get OUT`() = testEach(
            { notStartedProcess },
            { startedProcess },
            { terminatedProcess },
        ) {
            expect { it().let { p -> p to p.output() } }.that {
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
