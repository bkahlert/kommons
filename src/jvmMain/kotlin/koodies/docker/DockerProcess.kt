package koodies.docker

import koodies.asString
import koodies.exec.Exec
import koodies.exec.Exec.Companion.createDump
import koodies.exec.Process
import koodies.exec.Process.ExitState
import koodies.exec.Process.ExitState.Fatal
import koodies.exec.Process.ProcessState
import koodies.exec.Process.ProcessState.Prepared
import koodies.exec.ExecTerminationCallback
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
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import kotlin.time.seconds

/**
 * A [Process] representing a running a [Docker] container.
 */
public open class DockerProcess private constructor(
    public val container: DockerContainer,
    private val exec: Exec,
) : Exec by exec {

    public companion object {
        public fun from(
            dockerRunCommandLine: DockerRunCommandLine,
            execTerminationCallback: ExecTerminationCallback? = null,
        ): DockerProcess = DockerProcess(
            container = dockerRunCommandLine.options.name ?: error("Docker container name missing."),
            exec = Exec.from(dockerRunCommandLine, null, execTerminationCallback))
    }

    override var exitState: ExitState? = null
    override val state: ProcessState
        get() = when (container.state) {
            is NotExistent -> exitState ?: Prepared()
            is Created -> Prepared(format())
            is Restarting, is Running, is Removing, is Paused -> ProcessState.Running(pid, format())
                .also { onExit } // hack to trigger lazy on exit register an exit state storing future
            is Exited, is Dead, is Error -> exitState ?: run {
                val message = "Backed Docker process no more running but no exit state is known."
                val dump = createDump(message)
                Fatal(IllegalStateException(dump.ansiRemoved), exitValue, pid, dump, io.toList(), message).also { exitState = it }
            }
        }

    override val onExit: CompletableFuture<out ExitState> by lazy {
        exec.onExit.apply { thenAccept { run { exitState = it } } }
    }

    override fun start(): DockerProcess = also { exec.start() }

    /**
     * Stops this process by stopping its container.
     */
    override fun stop(): DockerProcess = stop(null)

    /**
     * Kills this process by killing its container.
     */
    override fun kill(): DockerProcess = kill(null)

    /**
     * Stops this process by stopping its container with the optionally specified [timeout] (default: 5 seconds).
     */
    public fun stop(timeout: Duration? = 5.seconds): DockerProcess =
        also { container.stop(timeout = timeout) }.also { exec.stop() }

    /**
     * Kills this process by killing its container with the optionally specified [signal] (default: KILL).
     */
    public fun kill(signal: String?): DockerProcess =
        also { container.kill(signal = signal) }.also { exec.kill() }

    override fun toString(): String = asString(::container, ::exec)
}
