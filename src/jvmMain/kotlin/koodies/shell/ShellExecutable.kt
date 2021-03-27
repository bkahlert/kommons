package koodies.shell

import koodies.concurrent.Executable
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.ProcessTerminationCallback

public interface ShellExecutable : Executable {
    public override val summary: String
    public fun toCommandLine(): CommandLine

    public fun toProcess(): ManagedProcess = toProcess(0, null)

    override fun toProcess(expectedExitValue: Int?, processTerminationCallback: ProcessTerminationCallback?): ManagedProcess =
        ManagedProcess.from(toCommandLine(), expectedExitValue, processTerminationCallback)
}
