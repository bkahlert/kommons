package koodies.docker

import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Process
import koodies.concurrent.thread
import koodies.time.poll
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
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

    /**
     * Gracefully attempts to stop the execution of the backing Docker container
     * for at most the given [escalationTimeout]. If the Docker container does not
     * stop in time the backing OS process is requested to [ManagedProcess.stop].
     *
     * If [async] is set, this call returns immediately, stopping the Docker
     * container in a separate thread.
     */
    fun stop(async: Boolean = false, escalationTimeout: Duration = 10.seconds): Process = also {
        val block = {
            Docker.stop(name).also {
                runCatching { pollTermination(escalationTimeout) }
                managedProcess.stop()
            }
        }
        if (async) thread(block = block)
        else block()
    }

    /**
     * Forcefully stops the execution of the backing Docker container by
     * attempting to [stop] it gracefully for at most the given [gracefulStopTimeout].
     *
     * If the Docker container does not stop in time a container is forcefully removed.
     *
     * If [async] is set, this call returns immediately, killing the Docker
     * container in a separate thread.
     */
    fun kill(async: Boolean = false, gracefulStopTimeout: Duration = 2.seconds): Process = also {
        val block = {
            Docker.stop(name).also {
                runCatching { pollTermination(gracefulStopTimeout) }
                Docker.remove(name, forcibly = true)
                managedProcess.kill()
            }
        }
        if (async) thread(block = block)
        else block()
    }

    private fun pollTermination(timeout: Duration): DockerProcess = also {
        poll { !alive }
            .every(100.milliseconds)
            .forAtMost(timeout) { throw TimeoutException("Could not clean up $this within $it.") }
    }

    override fun toString(): String = managedProcess.toString().replaceBefore("Process[", "DockerProcess[name=$name, ")
}
