package koodies.concurrent

import koodies.collections.synchronizedListOf
import koodies.concurrent.process.IO
import koodies.concurrent.process.process
import koodies.exec.CommandLine
import koodies.exec.Exec
import koodies.exec.JavaExec
import koodies.exec.Process.ExitState.Failure
import koodies.exec.started
import koodies.shell.ShellScript
import koodies.test.UniqueId
import koodies.test.tests
import koodies.test.withTempDir
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@Execution(CONCURRENT)
class ProcessesKtTest {

    private val echoingCommands =
        "echo \"test output ${'$'}TEST\"; sleep 1; >&2 echo \"test error 1\"; sleep 1; echo \"test output 2\"; >&2 echo \"test error 2\"; sleep 1"


    private fun testProcesses(uniqueId: UniqueId, command: String = echoingCommands, block: (Exec) -> Unit): List<DynamicNode> =
        tests {
            test {
                withTempDir(uniqueId) {
                    val commandLine = CommandLine(
                        environment = mapOf("TEST" to "env"),
                        workingDirectory = this,
                        "/bin/sh", "-c", command,
                    )
                    JavaExec(commandLine, null, null).let(block)
                }
            }
            test {
                withTempDir(uniqueId) {
                    val shellScript = ShellScript {
                        !command
                    }
                    process(shellScript,
                        environment = mapOf("TEST" to "env"),
                        workingDirectory = this).let(block)
                }
            }
        }

    @TestFactory
    fun `should not start`(uniqueId: UniqueId) = testProcesses(uniqueId) { process ->
        expectThat(process.started).isFalse()
    }

    @TestFactory
    fun `should start`(uniqueId: UniqueId) = testProcesses(uniqueId) { process ->
        process.start()
        expectThat(process.started).isTrue()
    }

    @TestFactory
    fun `should process`(uniqueId: UniqueId) = testProcesses(uniqueId) { process ->
        val processed = synchronizedListOf<IO>()
        process.process { io -> processed.add(io) }.waitFor()

        expectThat(processed).contains(
            IO.OUT typed "test output env",
            IO.ERR typed "test error 1",
            IO.OUT typed "test output 2",
            IO.ERR typed "test error 2",
        )
    }

    @TestFactory
    fun `should not throw on unexpected exit value`(uniqueId: UniqueId) = testProcesses(uniqueId, "exit 42") { process ->
        process.start()
        expectThat(process.started).isTrue()
    }

    @TestFactory
    fun `should return failed state on unexpected exit value`(uniqueId: UniqueId) = testProcesses(uniqueId, "exit 42") { process ->
        process.start()
        expectThat(process.waitFor()).isA<Failure>()
    }
}
