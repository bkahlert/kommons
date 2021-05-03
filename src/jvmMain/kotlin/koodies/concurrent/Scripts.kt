package koodies.concurrent

import koodies.concurrent.process.IO
import koodies.concurrent.process.output
import koodies.exec.Exec
import koodies.exec.ExecTerminationCallback
import koodies.io.path.Locations
import koodies.logging.MutedRenderingLogger
import koodies.logging.RenderingLogger
import koodies.shell.ShellScript
import koodies.shell.ShellScript.ScriptContext
import java.nio.file.Path

/**
 * Creates a [Exec] from the specified [shellScript]
 * with the specified [workingDirectory] and the specified [environment].
 *
 * If provided, the [execTerminationCallback] will be called on process
 * termination and before other [Exec.onExit] registered listeners
 * get called.
 */
@Deprecated("delete")
public fun process(
    shellScript: ShellScript,
    environment: Map<String, String> = emptyMap(),
    workingDirectory: Path = Locations.Temp,
    execTerminationCallback: ExecTerminationCallback? = null,
): Exec {
    return shellScript.toExec(false, environment, workingDirectory, execTerminationCallback)
}

/**
 * Runs the specified [shellScript] in `this` [Path].
 *
 * The output of this script will be logged by the specified [logger]
 * which prints all [IO] to the console if `null`.
 *
 * Though [execute] is recommended, for simple IO processing, [output] can be used.
 */
@Deprecated("use exec")
public fun Path.script(
    logger: RenderingLogger = MutedRenderingLogger(),
    shellScript: ScriptContext.() -> Unit,
): Exec = ShellScript { shellScript(); "" }.exec.logging(logger, this)
