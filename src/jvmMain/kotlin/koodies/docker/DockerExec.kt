package koodies.docker

import koodies.asString
import koodies.docker.DockerContainer.State.Error
import koodies.docker.DockerContainer.State.Existent.Created
import koodies.docker.DockerContainer.State.Existent.Dead
import koodies.docker.DockerContainer.State.Existent.Exited
import koodies.docker.DockerContainer.State.Existent.Paused
import koodies.docker.DockerContainer.State.Existent.Removing
import koodies.docker.DockerContainer.State.Existent.Restarting
import koodies.docker.DockerContainer.State.Existent.Running
import koodies.docker.DockerContainer.State.NotExistent
import koodies.docker.DockerExitStateHandler.Failure
import koodies.exec.Exec
import koodies.exec.Exec.Companion.createDump
import koodies.exec.Process.ExitState
import koodies.exec.Process.ExitState.Fatal
import koodies.exec.Process.ProcessState
import koodies.text.ANSI.ansiRemoved
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration

/**
 * An [Exec] representing a running a [Docker] container.
 */
public open class DockerExec(
    public val container: DockerContainer,
    private val exec: Exec,
) : Exec by exec {

    override var exitState: ExitState? = null
    override val state: ProcessState
        get() = exitState ?: when (container.state) {
            is NotExistent, is Created, is Restarting, is Running, is Removing, is Paused -> ProcessState.Running(pid, format())
            is Exited, is Dead, is Error -> run {
                val message = "Backed Docker exec no more running but no exit state is known."
                val dump = createDump(message)
                Fatal(IllegalStateException(dump.ansiRemoved), -1, pid, dump, io, message).also { exitState = it }
            }
        }.also { onExit } // hack to trigger lazy on exit register an exit state storing future

    override val onExit: CompletableFuture<out ExitState> by lazy {
        exec.onExit.apply {
            thenAccept {
                exitState = kotlin.runCatching { DockerExitStateHandler.handle(it) }.getOrNull()
                    ?.takeIf { it is Failure.ConnectivityProblem }
                    ?: it
            }
        }
    }

    /**
     * Stops this [Exec] by stopping its container.
     */
    override fun stop(): DockerExec = stop(null)

    /**
     * Kills this [Exec] by killing its container.
     */
    override fun kill(): DockerExec = kill(null)

    /**
     * Stops this process by stopping its container with the optionally specified [timeout] (default: 5 seconds).
     */
    public fun stop(timeout: Duration? = Duration.seconds(5)): DockerExec =
        also { container.stop(timeout = timeout) }.also { exec.stop() }

    /**
     * Kills this process by killing its container with the optionally specified [signal] (default: KILL).
     */
    public fun kill(signal: String?): DockerExec =
        also { container.kill(signal = signal) }.also { exec.kill() }

    override fun toString(): String = asString(::container, ::exec)
}
