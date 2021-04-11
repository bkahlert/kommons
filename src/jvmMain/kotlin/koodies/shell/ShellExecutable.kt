package koodies.shell

import koodies.concurrent.Executable
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Process.ExitState.ExitStateHandler
import koodies.concurrent.process.ProcessTerminationCallback

/**
 * An [Executable] that is executed by creating a [CommandLine]
 * that is run by means of a shell.
 */
public interface ShellExecutable : Executable {

    /**
     * Creates a [CommandLine] that is able to run [Executable].
     */
    public fun toCommandLine(): CommandLine

    /**
     * Creates a [ManagedProcess] to run this executable.
     */
    public fun toProcess(): ManagedProcess = toProcess(null, null)

    /**
     * Creates a [ManagedProcess] by retrieving a [CommandLine]
     * using [toCommandLine] and running it by means of a shell.
     *
     * @param exitStateHandler if specified, the process's exit state is delegated to it
     * @param processTerminationCallback if specified, will be called with the process's final exit state
     */
    override fun toProcess(
        exitStateHandler: ExitStateHandler?,
        processTerminationCallback: ProcessTerminationCallback?,
    ): ManagedProcess = ManagedProcess.from(
        toCommandLine(),
        exitStateHandler,
        processTerminationCallback
    )
}
