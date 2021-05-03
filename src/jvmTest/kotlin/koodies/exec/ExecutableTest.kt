package koodies.exec

import koodies.collections.synchronizedListOf
import koodies.concurrent.process.IO
import koodies.concurrent.process.process
import koodies.exec.Process.ExitState.Failure
import koodies.shell.ShellScript
import koodies.test.UniqueId
import koodies.test.tests
import koodies.test.withTempDir
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isGreaterThan
import strikt.assertions.isSuccess

class ExecutableTest {

    private val echoingCommands =
        "echo \"test output ${'$'}TEST\"; sleep 1; >&2 echo \"test error 1\"; sleep 1; echo \"test output 2\"; >&2 echo \"test error 2\"; sleep 1"

    private fun testProcesses(uniqueId: UniqueId, command: String = echoingCommands, block: (Exec) -> Unit): List<DynamicNode> =
        tests {
            test {
                withTempDir(uniqueId) {
                    val commandLine = CommandLine("/bin/sh", "-c", command)
                    commandLine.toExec(false, mapOf("TEST" to "env"), this, null).let(block)
                }
            }
            test {
                withTempDir(uniqueId) {
                    val shellScript = ShellScript { !command }
                    shellScript.toExec(false, mapOf("TEST" to "env"), this, null).let(block)
                }
            }
        }

    @TestFactory
    fun `should start`(uniqueId: UniqueId) = testProcesses(uniqueId) { process ->
        expectThat(process.pid).isGreaterThan(0L)
    }

    @TestFactory
    fun `should process`(uniqueId: UniqueId) = testProcesses(uniqueId) { process ->
        val processed = synchronizedListOf<IO>()
        process.process { io -> processed.add(io) }.waitFor()

        expectThat(processed).contains(
            IO.Output typed "test output env",
            IO.Error typed "test error 1",
            IO.Output typed "test output 2",
            IO.Error typed "test error 2",
        )
    }

    @TestFactory
    fun `should return failed state on unexpected exit value`(uniqueId: UniqueId) = testProcesses(uniqueId, "exit 42") { process ->
        expectCatching { process.waitFor() }.isSuccess().isA<Failure>()
    }

    @Nested
    inner class ShellScriptBased {

        @Test
        fun `should add missing shebang`() {
            val exec = ShellScript().toExec(false, emptyMap(), null, null)
            expectThat(exec).isA<JavaExec>().get { commandLine.shellCommand }.contains("#!/bin/sh")
        }
    }
}
