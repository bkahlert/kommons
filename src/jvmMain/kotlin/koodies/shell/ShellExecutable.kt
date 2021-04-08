package koodies.shell

import koodies.concurrent.Executable
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Process.ExitState.ExitStateHandler
import koodies.concurrent.process.ProcessTerminationCallback

/**
 * An [Executable] that can be run using the [CommandLine]
 * return by [toCommandLine].
 */
public interface ShellExecutable : Executable {

    public fun toCommandLine(): CommandLine

    /**
     * Creates a [ManagedProcess] to run this executable.
     */
    public fun toProcess(): ManagedProcess = toProcess(null, null)

    override fun toProcess(
        exitStateHandler: ExitStateHandler?,
        processTerminationCallback: ProcessTerminationCallback?,
    ): ManagedProcess = ManagedProcess.from(
        toCommandLine(),
        exitStateHandler,
        processTerminationCallback
    )
}
