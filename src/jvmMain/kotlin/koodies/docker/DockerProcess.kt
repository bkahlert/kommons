package koodies.docker

import koodies.asString
import koodies.concurrent.daemon
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.ManagedProcess.Companion.createDump
import koodies.concurrent.process.Process
import koodies.concurrent.process.Process.ExitState
import koodies.concurrent.process.Process.ExitState.Fatal
import koodies.concurrent.process.Process.ProcessState
import koodies.concurrent.process.Process.ProcessState.Prepared
import koodies.concurrent.process.ProcessTerminationCallback
import koodies.concurrent.process.io
import koodies.concurrent.thread
import koodies.docker.DockerContainer.State.Error
import koodies.docker.DockerContainer.State.Existent.Created
import koodies.docker.DockerContainer.State.Existent.Dead
import koodies.docker.DockerContainer.State.Existent.Exited
import koodies.docker.DockerContainer.State.Existent.Paused
import koodies.docker.DockerContainer.State.Existent.Removing
import koodies.docker.DockerContainer.State.Existent.Restarting
import koodies.docker.DockerContainer.State.Existent.Running
import koodies.docker.DockerContainer.State.NotExistent
import koodies.text.ANSI.ansiRemoved
import koodies.time.poll
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.milliseconds
import kotlin.time.seconds

/**
 * A [Process] representing a running a [Docker] container.
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
//                    TODO       container.remove { force { on } }.apply { onExit.orTimeout(8, SECONDS).get() }
                    processTerminationCallback?.also { it(ex) }
                })
            return DockerProcess(container, managedProcess)
        }
    }

    private var x: ExitState? = null


    override var exitState: ExitState? = null
    override val state: ProcessState
        get() {
            val state1 = container.state
            return when (state1) {
                is NotExistent -> exitState ?: Prepared()
                is Created -> Prepared(format())
                is Restarting, is Running, is Removing, is Paused -> ProcessState.Running(pid, format())
                is Exited, is Dead, is Error -> exitState ?: run {
                    val message = "Backed Docker process no more running but no exit state is known."
                    val dump = createDump(message)
                    Fatal(IllegalStateException(dump.ansiRemoved), exitValue, pid, dump, io, message).also { exitState = it }
                }
            }
        }

    override fun start(): DockerProcess = also { managedProcess.start() }
    override fun stop(): DockerProcess = stop(async = false)
    override fun kill(): DockerProcess = kill(async = false)

    /**
     * Gracefully attempts to stop the execution of the backing Docker container
     * for at most the given [timeout]. If the Docker container does not
     * stop in time the backing OS process is requested to [ManagedProcess.stop].
     *
     * If [async] is set, this call returns immediately, stopping the Docker
     * container in a separate thread.
     */
    public fun stop(async: Boolean = false, timeout: Duration = 10.seconds): DockerProcess = also {
        val block: () -> Unit = {
            daemon { container.stop(5.seconds) }.also {
                runCatching { pollTermination(timeout + .5.milliseconds) }
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
    public fun kill(async: Boolean = false, gracefulStopTimeout: Duration = 2.seconds): DockerProcess = also {
        val block: () -> Unit = {
            daemon { container.stop(gracefulStopTimeout) }.also {
                runCatching { pollTermination(gracefulStopTimeout + .5.milliseconds) }
                container.remove(force = true)
            }
        }

        if (async) thread(block = block)
        else block()
    }

    private fun pollTermination(timeout: Duration): DockerProcess = also {
        poll { !container.isRunning }
            .every(500.milliseconds)
            .forAtMost(timeout) { throw TimeoutException("Could not clean up $this within $it.") }
    }

    override fun toString(): String = asString(::container, ::managedProcess)

    init {
        onExit.thenAccept { run { exitState = it } }
    }
}
