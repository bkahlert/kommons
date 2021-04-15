package koodies.concurrent

import koodies.concurrent.process.CommandLine
import koodies.exec.Exec
import koodies.exec.Process.ExitState.ExitStateHandler
import koodies.exec.ExecTerminationCallback
import koodies.docker.DockerProcess
import koodies.docker.DockerRunCommandLine
import koodies.io.path.Locations
import koodies.shell.ShellScript
import java.nio.file.Path

/**
 * Creates a [DockerProcess] that executes this command line.
 */
public fun DockerRunCommandLine.toExec(execTerminationCallback: ExecTerminationCallback?): DockerProcess =
    // TODO implement in DockerRunCommandLine
    DockerProcess.from(this, execTerminationCallback)

/**
 * Creates a [Exec] that executes this command line.
 */
public fun CommandLine.toExec(
    exitStateHandle: ExitStateHandler? = null,
    execTerminationCallback: ExecTerminationCallback? = null,
): Exec =
    toProcess(exitStateHandle, execTerminationCallback)

/**
 * Creates a [Exec] from the specified [commandLine].
 *
 * If provided, the [execTerminationCallback] will be called on process
 * termination and before other [Exec.onExit] registered listeners
 * get called.
 */
public fun process(
    commandLine: CommandLine,
    exitStateHandler: ExitStateHandler? = null,
    execTerminationCallback: ExecTerminationCallback? = null,
): Exec = commandLine.toExec(exitStateHandler, execTerminationCallback)

/**
 * Creates a [Exec] from the specified [shellScript]
 * with the specified [workingDirectory] and the specified [environment].
 *
 * If provided, the [execTerminationCallback] will be called on process
 * termination and before other [Exec.onExit] registered listeners
 * get called.
 */
public fun process(
    shellScript: ShellScript,
    environment: Map<String, String> = emptyMap(),
    workingDirectory: Path = Locations.Temp,
    exitStateHandler: ExitStateHandler? = null,
    execTerminationCallback: ExecTerminationCallback? = null,
): Exec {
    val commandLine = shellScript.toCommandLine(workingDirectory, environment)
    return process(commandLine, exitStateHandler, execTerminationCallback)
}
