package koodies.concurrent

import koodies.exec.Exec
import koodies.exec.ExecTerminationCallback
import koodies.exec.JavaExec
import koodies.exec.Process.ExitState.ExitStateHandler
import koodies.io.path.Locations
import koodies.shell.ShellScript
import java.nio.file.Path

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
    return JavaExec(commandLine, exitStateHandler, execTerminationCallback)
}
