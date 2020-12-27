package koodies.docker

import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Process
import koodies.time.poll
import koodies.text.quoted
import java.util.concurrent.TimeoutException
import kotlin.time.milliseconds
import kotlin.time.seconds

/**
 * A [Process] responsible to run a [Docker] container.
 */
open class DockerProcess private constructor(
    val name: String,
    private val managedProcess: ManagedProcess,
) : ManagedProcess by managedProcess {

    companion object {
        fun from(commandLine: DockerRunCommandLine, expectedExitValue: Int): DockerProcess {
            val name = commandLine.options.name?.sanitized ?: error("Docker container name missing.")
            val managedProcess = ManagedProcess.from(commandLine,
                expectedExitValue = expectedExitValue,
                processTerminationCallback = {
                    Docker.stop(name)
                    Docker.remove(name, forcibly = true)
                })
            return DockerProcess(name, managedProcess)
        }
    }

    init {
        metaLog("🐳 docker attach ${name.quoted}") // TODO consume by Processors
    }

    override val alive: Boolean get() = Docker.isContainerRunning(name)

    override fun stop(): Process = also { Docker.stop(name) }.also { pollTermination() }.also { managedProcess.stop() }
    override fun kill(): Process = also { stop() }.also { managedProcess.kill() }

    private fun pollTermination(): DockerProcess {
        poll {
            !alive
        }.every(100.milliseconds).forAtMost(10.seconds) {
            throw TimeoutException("Could not clean up $this within $it.")
        }
        return this
    }

    override fun toString(): String = super.toString().replaceFirst("Process[", "DockerProcess[name=$name, ")
}
