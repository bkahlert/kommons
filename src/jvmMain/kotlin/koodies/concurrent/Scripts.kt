package koodies.concurrent

import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Processor
import koodies.concurrent.process.Processors
import koodies.concurrent.process.processSynchronously
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
fun Path.script(
    shellScript: ShellScript,
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
    processor: Processor<ManagedProcess> = Processors.consoleLoggingProcessor(),
): ManagedProcess {
    val scriptFile = shellScript.sanitize(this).buildTo(scriptPath())
    val commandLine = CommandLine(environment, this, scriptFile)
    return process(commandLine, expectedExitValue, processTerminationCallback).processSynchronously(processor)
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
fun script(
    shellScript: ShellScript,
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
    processor: Processor<ManagedProcess> = Processors.consoleLoggingProcessor(),
): ManagedProcess = Locations.Temp.script(shellScript, environment, expectedExitValue, processTerminationCallback, processor)


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
fun Path.script(
    processor: Processor<ManagedProcess> = Processors.consoleLoggingProcessor(),
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
    shellScript: ShellScript.() -> Unit,
): ManagedProcess = script(shellScript.build(), environment, expectedExitValue, processTerminationCallback, processor)

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
fun script(
    processor: Processor<ManagedProcess> = Processors.consoleLoggingProcessor(),
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
    shellScript: ShellScript.() -> Unit,
): ManagedProcess = script(shellScript.build(), environment, expectedExitValue, processTerminationCallback, processor)


/**
 * Runs the specified [shellScript] with the specified [environment]
 * in `this` [Path] optionally checking the specified [expectedExitValue] (default: `0`).
 *
 * The output of this script will be logged by the specified [logger]
 * which prints all [IO] to the console if `null`
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
fun Path.script(
    logger: RenderingLogger?,
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
    shellScript: ShellScript.() -> Unit,
): ManagedProcess = script(
    processor = logger.toProcessor(),
    environment = environment,
    expectedExitValue = expectedExitValue,
    processTerminationCallback = processTerminationCallback,
    shellScript = shellScript
)

/**
 * Runs the specified [shellScript] with the specified [environment]
 * in [Locations.Temp] optionally checking the specified [expectedExitValue] (default: `0`).
 *
 * The output of this script will be logged by the specified [logger]
 * which prints all [IO] to the console if `null`
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
fun script(
    logger: RenderingLogger?,
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
    shellScript: ShellScript.() -> Unit,
): ManagedProcess = script(
    processor = logger.toProcessor(),
    environment = environment,
    expectedExitValue = expectedExitValue,
    processTerminationCallback = processTerminationCallback,
    shellScript = shellScript
)

/**
 * Creates a [Processor] from `this` [RenderingLogger].
 *
 * Returns a [Processors.consoleLoggingProcessor] if `this` is `null`.
 */
private fun RenderingLogger?.toProcessor() =
    this?.let { Processors.loggingProcessor(it) } ?: Processors.consoleLoggingProcessor()

/**
 * Convenience function to tests if the output of the specified [command]
 * contains the specified [substring] (case-**in**sensitive by default,
 * that is ignoring the case).
 */
fun scriptOutputContains(command: String, substring: String, caseSensitive: Boolean = false): Boolean = runCatching {
    val flags = if (caseSensitive) "" else "i"
    val shellScript = ShellScript { line("$command | grep -q$flags '$substring'") }
    check(Locations.Temp.script(shellScript, processor = Processors.noopProcessor()).waitFor() == 0)
}.isSuccess
