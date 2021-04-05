package koodies.concurrent

import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.ProcessTerminationCallback
import koodies.docker.DockerProcess
import koodies.docker.DockerRunCommandLine
import koodies.io.path.Locations
import koodies.shell.ShellScript
import java.nio.file.Path

/**
 * Creates a [DockerProcess] that executes this command line.
 */
public fun DockerRunCommandLine.toManagedProcess(processTerminationCallback: ProcessTerminationCallback?): DockerProcess =
    // TODO implement in DockerRunCommandLine
    DockerProcess.from(this, processTerminationCallback)

/**
 * Creates a [ManagedProcess] that executes this command line.
 */
public fun CommandLine.toManagedProcess(processTerminationCallback: ProcessTerminationCallback? = null): ManagedProcess =
    toProcess(processTerminationCallback)

/**
 * Creates a [ManagedProcess] from the specified [commandLine].
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
public fun process(
    commandLine: CommandLine,
    processTerminationCallback: ProcessTerminationCallback? = null,
): ManagedProcess = commandLine.toManagedProcess(processTerminationCallback)

/**
 * Creates a [ManagedProcess] from the specified [shellScript]
 * with the specified [workingDirectory] and the specified [environment].
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
public fun process(
    shellScript: ShellScript,
    environment: Map<String, String> = emptyMap(),
    workingDirectory: Path = Locations.Temp,
    processTerminationCallback: ProcessTerminationCallback? = null,
): ManagedProcess {
    val commandLine = shellScript.toCommandLine(workingDirectory, environment)
    return process(commandLine, processTerminationCallback)
}
