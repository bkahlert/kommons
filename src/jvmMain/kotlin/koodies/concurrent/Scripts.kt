package koodies.concurrent

import koodies.concurrent.process.IO
import koodies.concurrent.process.output
import koodies.exec.Exec
import koodies.io.path.Locations
import koodies.logging.MutedRenderingLogger
import koodies.logging.RenderingLogger
import koodies.shell.ShellScript
import koodies.shell.ShellScript.Companion.build
import java.nio.file.Path

/**
 * Runs the specified [shellScript] in [Locations.Temp].
 *
 * The output of this script will be logged by the specified [logger]
 * which prints all [IO] to the console if `null`.
 *
 * Though [exec] is recommended, for simple IO processing, [output] can be used.
 */
@Deprecated("use exec")
public fun script(
    logger: RenderingLogger = MutedRenderingLogger(),
    shellScript: ShellScript.() -> Unit,
): Exec = Locations.Temp.script(logger, shellScript)

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
    shellScript: ShellScript.() -> Unit,
): Exec = shellScript.build().toCommandLine(this@script).exec.logging(logger)
