package koodies.docker

import koodies.builder.BooleanBuilder.OnOff.Context.on
import koodies.concurrent.daemon
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Process
import koodies.concurrent.process.ProcessTerminationCallback
import koodies.concurrent.thread
import koodies.time.poll
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.milliseconds
import kotlin.time.seconds

/**
 * A [Process] responsible to run a [Docker] container.
 */
public open class DockerProcess private constructor(
    public val container: DockerContainer,
    private val managedProcess: ManagedProcess,
) : ManagedProcess by managedProcess {

    public companion object {
        public fun from(
            dockerRunCommandLine: DockerRunCommandLine,
            processTerminationCallback: ProcessTerminationCallback? = null,
        ): DockerProcess {
            val container = dockerRunCommandLine.options.name ?: error("Docker container name missing.")
            val managedProcess = ManagedProcess.from(dockerRunCommandLine,
                processTerminationCallback = { ex ->
                    //TODO       container.remove { force { on } }.apply { onExit.orTimeout(8, SECONDS).get() }
                    processTerminationCallback?.also { it(ex) }
                })
            return DockerProcess(container, managedProcess)
        }
    }

    override val alive: Boolean get() = container.isRunning

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
    public fun stop(async: Boolean = false, escalationTimeout: Duration = 10.seconds): Process = also {
        val block: () -> Unit = {
            daemon { container.stop { time { 5 } } }.also {
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
    public fun kill(async: Boolean = false, gracefulStopTimeout: Duration = 2.seconds): Process = also {
        val block: () -> Unit = {
            daemon { container.stop { time { 5 } } }.also {
                runCatching { pollTermination(gracefulStopTimeout) }
                container.remove { force { on } }
                onExit.orTimeout(8, SECONDS).get()
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

    override fun toString(): String = managedProcess.toString().replaceBefore("Process(", "DockerProcess(name=${container.name}, ")
}
