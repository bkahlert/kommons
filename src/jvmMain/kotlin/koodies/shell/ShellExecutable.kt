package koodies.shell

import koodies.concurrent.Executable
import koodies.concurrent.process.CommandLine
import koodies.exec.Exec
import koodies.exec.Process.ExitState.ExitStateHandler
import koodies.exec.ExecTerminationCallback

/**
 * An [Executable] that is executed by creating a [CommandLine]
 * that is run by means of a shell.
 */
public interface ShellExecutable : Executable {

    /**
     * Creates a [Exec] to run this executable.
     */
    public fun toProcess(): Exec = toProcess(null, null)

    /**
     * Creates a [Exec] by retrieving a [CommandLine]
     * using [toCommandLine] and running it by means of a shell.
     *
     * @param exitStateHandler if specified, the process's exit state is delegated to it
     * @param execTerminationCallback if specified, will be called with the process's final exit state
     */
    override fun toProcess(
        exitStateHandler: ExitStateHandler?,
        execTerminationCallback: ExecTerminationCallback?,
    ): Exec = Exec.from(
        toCommandLine(),
        exitStateHandler,
        execTerminationCallback
    )
}
