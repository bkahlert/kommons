package koodies.concurrent

import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.ProcessTerminationCallback
import koodies.concurrent.process.Processors
import koodies.io.path.Locations
import koodies.io.path.randomPath
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
 * Runs the specified [shellScript] with the specified [environment]
 * in `this` [Path] optionally checking the specified [expectedExitValue] (default: `0`).
 *
 * The output of this script will be processed by the specified [processor]
 * which defaults to [Processors.consoleLoggingProcessor] which prints all [IO] to the console.
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
public fun Path.script(
    shellScript: ShellScript,
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: ProcessTerminationCallback? = null,
): ManagedProcess {
    val commandLine = shellScript.toCommandLine(this, environment)
    return process(commandLine, expectedExitValue, processTerminationCallback)
}

/**
 * Runs the specified [shellScript] with the specified [environment]
 * in [Locations.Temp] optionally checking the specified [expectedExitValue] (default: `0`).
 *
 * The output of this script will be processed by the specified [processor]
 * which defaults to [Processors.consoleLoggingProcessor] which prints all [IO] to the console.
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
public fun script(
    shellScript: ShellScript,
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: ProcessTerminationCallback? = null,
//    processor: Processor<ManagedProcess> = Processors.consoleLoggingProcessor(),
): ManagedProcess = Locations.Temp.script(shellScript, environment, expectedExitValue, processTerminationCallback)//, processor)


/**
 * Runs the specified [shellScript] with the specified [environment]
 * in `this` [Path] optionally checking the specified [expectedExitValue] (default: `0`).
 *
 * The output of this script will be processed by the specified [processor]
 * which defaults to [Processors.consoleLoggingProcessor] which prints all [IO] to the console.
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
public fun Path.script(
//    processor: Processor<ManagedProcess> = Processors.consoleLoggingProcessor(),
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: ProcessTerminationCallback? = null,
    shellScript: ShellScript.() -> Unit,
): ManagedProcess = script(shellScript.build(), environment, expectedExitValue, processTerminationCallback)//, processor)

/**
 * Runs the specified [shellScript] with the specified [environment]
 * in [Locations.Temp] optionally checking the specified [expectedExitValue] (default: `0`).
 *
 * The output of this script will be processed by the specified [processor]
 * which defaults to [Processors.consoleLoggingProcessor] which prints all [IO] to the console.
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
public fun script(
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: ProcessTerminationCallback? = null,
    shellScript: ShellScript.() -> Unit,
): ManagedProcess = script(shellScript.build(), environment, expectedExitValue, processTerminationCallback)

//
///**
// * Runs the specified [shellScript] with the specified [environment]
// * in `this` [Path] optionally checking the specified [expectedExitValue] (default: `0`).
// *
// * The output of this script will be logged by the specified [logger]
// * which prints all [IO] to the console if `null`.
// *
// * If provided, the [processTerminationCallback] will be called on process
// * termination and before other [ManagedProcess.onExit] registered listeners
// * get called.
// */
//public fun Path.script(
//    logger: RenderingLogger?,
//    environment: Map<String, String> = emptyMap(),
//    expectedExitValue: Int? = 0,
//    processTerminationCallback: ProcessTerminationCallback? = null,
//    shellScript: ShellScript.() -> Unit,
//): ManagedProcess = script(
//    processor = TODO(),//logger.toProcessor(),
//    environment = environment,
//    expectedExitValue = expectedExitValue,
//    processTerminationCallback = processTerminationCallback,
//    shellScript = shellScript
//)
//
/**
 * Runs the specified [shellScript] with the specified [environment]
 * in [Locations.Temp] optionally checking the specified [expectedExitValue] (default: `0`).
 *
 * The output of this script will be logged by the specified [logger]
 * which prints all [IO] to the console if `null`.
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
public fun script(
    logger: RenderingLogger?,
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: ProcessTerminationCallback? = null,
    shellScript: ShellScript.() -> Unit,
): ManagedProcess = with(logger) { shellScript.build().execute { null } }


/**
 * Convenience function to tests if the output of the specified [command]
 * contains the specified [substring] (case-**in**sensitive by default,
 * that is ignoring the case).
 */
public fun scriptOutputContains(command: String, substring: String, caseSensitive: Boolean = false): Boolean =
    script { !command }.output().contains(substring, ignoreCase = !caseSensitive)
