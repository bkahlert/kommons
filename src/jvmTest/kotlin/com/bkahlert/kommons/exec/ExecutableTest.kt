package com.bkahlert.kommons.exec

import com.bkahlert.kommons.collections.synchronizedListOf
import com.bkahlert.kommons.exec.Process.State.Exited.Failed
import com.bkahlert.kommons.shell.ShellScript
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.testsOld
import com.bkahlert.kommons.test.withTempDir
import org.junit.jupiter.api.DynamicNode
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
        testsOld {
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
        process.process { _, processIO -> processIO { io -> processed.add(io) } }.waitFor()

        expectThat(processed).contains(
            IO.Output typed "test output env",
            IO.Error typed "test error 1",
            IO.Output typed "test output 2",
            IO.Error typed "test error 2",
        )
    }

    @TestFactory
    fun `should return failed state on unexpected exit value`(uniqueId: UniqueId) = testProcesses(uniqueId, "exit 42") { process ->
        expectCatching { process.waitFor() }.isSuccess().isA<Failed>()
    }
}
