package koodies.docker

import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Process
import koodies.text.quoted
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
            dockerCommandLine: DockerCommandLine,
            expectedExitValue: Int?,
            processTerminationCallback: (() -> Unit)? = null,
        ): DockerProcess {
            val name = dockerCommandLine.options.name?.sanitized ?: error("Docker container name missing.")
            val managedProcess = ManagedProcess.from(dockerCommandLine,
                expectedExitValue = expectedExitValue,
                processTerminationCallback = {
                    Docker.remove(name, forcibly = true)
                    processTerminationCallback?.also { it() }
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
