package koodies.docker

import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Process
import koodies.concurrent.thread
import koodies.time.poll
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
        fun from(
            dockerRunCommandLine: DockerRunCommandLine,
            expectedExitValue: Int?,
            processTerminationCallback: (() -> Unit)? = null,
        ): DockerProcess {
            val name = dockerRunCommandLine.options.name?.sanitized ?: error("Docker container name missing.")
            val managedProcess = ManagedProcess.from(dockerRunCommandLine,
                expectedExitValue = expectedExitValue,
                processTerminationCallback = {
                    Docker.remove(name, forcibly = true)
                    processTerminationCallback?.also { it() }
                })
            return DockerProcess(name, managedProcess)
        }
    }

    override val alive: Boolean get() = Docker.isContainerRunning(name)

    override fun stop(): Process = stop(false)
    override fun kill(): Process = kill(false)

    fun stop(async: Boolean = false): Process = also {
        val block = { Docker.stop(name).also { pollTermination() }.also { managedProcess.stop() } }
        if (async) thread(block = block)
        else block()
    }

    fun kill(async: Boolean = false): Process = also {
        val block = {
            Docker.stop(name).also { pollTermination() }.also {
                Docker.remove(name, forcibly = true)
                managedProcess.kill()
            }
        }
        if (async) thread(block = block)
        else block()
    }

    private fun pollTermination(): DockerProcess = also {
        poll { !alive }
            .every(100.milliseconds)
            .forAtMost(10.seconds) { throw TimeoutException("Could not clean up $this within $it.") }
    }

    override fun toString(): String = managedProcess.toString().replaceBefore("Process[", "DockerProcess[name=$name, ")
}
