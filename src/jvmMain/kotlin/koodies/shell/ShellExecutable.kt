package koodies.shell

import koodies.concurrent.Executable
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.ProcessTerminationCallback

public interface ShellExecutable : Executable {
    public override val summary: String
    public fun toCommandLine(): CommandLine

    public fun toProcess(): ManagedProcess = toProcess(null)

    override fun toProcess(processTerminationCallback: ProcessTerminationCallback?): ManagedProcess =
        ManagedProcess.from(toCommandLine(), processTerminationCallback)
}
