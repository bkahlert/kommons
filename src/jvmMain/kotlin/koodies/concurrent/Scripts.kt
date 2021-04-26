package koodies.concurrent

import koodies.concurrent.process.IO
import koodies.concurrent.process.output
import koodies.exec.Exec
import koodies.exec.execute
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
 * Though [execute] is recommended, for simple IO processing, [output] can be used.
 */
public fun script(
    logger: RenderingLogger? = MutedRenderingLogger(),
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
public fun Path.script(
    logger: RenderingLogger? = MutedRenderingLogger(),
    shellScript: ShellScript.() -> Unit,
): Exec = with(logger) { shellScript.build().toCommandLine(this@script).execute { null } }

/**
 * Convenience function to tests if the output of the specified [command]
 * contains the specified [substring] (case-**in**sensitive by default,
 * that is ignoring the case).
 */
public fun scriptOutputContains(command: String, substring: String, caseSensitive: Boolean = false): Boolean =
    script { !command }.output().contains(substring, ignoreCase = !caseSensitive)
