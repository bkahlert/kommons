package com.bkahlert.kommons.docker

import com.bkahlert.kommons.Now
import com.bkahlert.kommons.ansiRemoved
import com.bkahlert.kommons.debug.asString
import com.bkahlert.kommons.docker.DockerContainer.State.Error
import com.bkahlert.kommons.docker.DockerContainer.State.Existent.Created
import com.bkahlert.kommons.docker.DockerContainer.State.Existent.Dead
import com.bkahlert.kommons.docker.DockerContainer.State.Existent.Exited
import com.bkahlert.kommons.docker.DockerContainer.State.Existent.Paused
import com.bkahlert.kommons.docker.DockerContainer.State.Existent.Removing
import com.bkahlert.kommons.docker.DockerContainer.State.Existent.Restarting
import com.bkahlert.kommons.docker.DockerContainer.State.Existent.Running
import com.bkahlert.kommons.docker.DockerContainer.State.NotExistent
import com.bkahlert.kommons.exec.Exec
import com.bkahlert.kommons.exec.Exec.Companion.createDump
import com.bkahlert.kommons.exec.Process.ExitState
import com.bkahlert.kommons.exec.Process.State
import com.bkahlert.kommons.exec.Process.State.Excepted
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * An [Exec] representing a running a [Docker] container.
 */
public open class DockerExec(
    public val container: DockerContainer,
    private val exec: Exec,
) : Exec by exec {

    private var finalState: ExitState? = null
    override val state: State
        get() {
            return (exec.state as? ExitState) ?: when (container.containerState) {
                is NotExistent, is Created, is Restarting, is Running, is Removing, is Paused -> State.Running(start, pid, container.containerState.format())
                is Exited, is Dead, is Error -> run {
                    val message = "Backed Docker exec no more running but no exit state is known."
                    val dump = createDump(message)
                    Excepted(start, Now, pid, -1, io, IllegalStateException(dump.ansiRemoved), dump, message).also { finalState = it }
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
    public fun stop(timeout: Duration? = 5.seconds): DockerExec =
        also { container.stop(timeout = timeout) }.also { exec.stop() }

    /**
     * Kills this process by killing its container with the optionally specified [signal] (default: KILL).
     */
    public fun kill(signal: String?): DockerExec =
        also { container.kill(signal = signal) }.also { exec.kill() }

    override fun toString(): String = asString(::container, ::exec)
}
