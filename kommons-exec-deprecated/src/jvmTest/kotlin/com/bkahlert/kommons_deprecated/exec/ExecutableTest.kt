package com.bkahlert.kommons_deprecated.exec

import com.bkahlert.kommons.test.junit.SimpleId
import com.bkahlert.kommons_deprecated.collections.synchronizedListOf
import com.bkahlert.kommons_deprecated.exec.Process.State.Exited.Failed
import com.bkahlert.kommons_deprecated.shell.ShellScript
import com.bkahlert.kommons_deprecated.test.testsOld
import com.bkahlert.kommons_deprecated.test.withTempDir
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

    private fun testProcesses(simpleId: SimpleId, command: String = echoingCommands, block: (Exec) -> Unit): List<DynamicNode> =
        testsOld {
            test {
                withTempDir(simpleId) {
                    val commandLine = CommandLine("/bin/sh", "-c", command)
                    commandLine.toExec(false, mapOf("TEST" to "env"), this, null).let(block)
                }
            }
            test {
                withTempDir(simpleId) {
                    val shellScript = ShellScript { !command }
                    shellScript.toExec(false, mapOf("TEST" to "env"), this, null).let(block)
                }
            }
        }

    @TestFactory
    fun `should start`(simpleId: SimpleId) = testProcesses(simpleId) { process ->
        expectThat(process.pid).isGreaterThan(0L)
    }

    @TestFactory
    fun `should process`(simpleId: SimpleId) = testProcesses(simpleId) { process ->
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
    fun `should return failed state on unexpected exit value`(simpleId: SimpleId) = testProcesses(simpleId, "exit 42") { process ->
        expectCatching { process.waitFor() }.isSuccess().isA<Failed>()
    }
}