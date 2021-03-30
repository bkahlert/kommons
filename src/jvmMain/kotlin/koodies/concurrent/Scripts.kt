package koodies.concurrent

import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.output
import koodies.io.path.Locations
import koodies.io.path.randomPath
import koodies.logging.MutedRenderingLogger
import koodies.logging.RenderingLogger
import koodies.shell.ShellScript
import koodies.shell.ShellScript.Companion.build
import java.nio.file.Path
import kotlin.io.path.name

private const val shellScriptPrefix: String = "koodies.process."
private const val shellScriptExtension: String = ".sh"

internal fun Path.scriptPath(): Path = randomPath(base = shellScriptPrefix, extension = shellScriptExtension)
internal fun Path.isScriptFile(): Boolean = name.startsWith(shellScriptPrefix) && name.endsWith(shellScriptExtension)

/**
 * Builds a shell script that runs this command line.
 */
public fun CommandLine.toShellScript(): ShellScript =
    ShellScript().apply {
        shebang
        changeDirectoryOrExit(directory = workingDirectory)
        command(commandLine)
    }

/**
 * Saves a shell script to a temporary `.sh` file
 * that runs this command line.
 */
public fun CommandLine.toShellScriptFile(): Path =
    toShellScript().buildTo(workingDirectory.scriptPath())

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
): ManagedProcess = Locations.Temp.script(logger, shellScript)

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
): ManagedProcess = with(logger) { shellScript.build().toCommandLine(this@script).execute { null } }

/**
 * Convenience function to tests if the output of the specified [command]
 * contains the specified [substring] (case-**in**sensitive by default,
 * that is ignoring the case).
 */
public fun scriptOutputContains(command: String, substring: String, caseSensitive: Boolean = false): Boolean =
    script { !command }.output().contains(substring, ignoreCase = !caseSensitive)
