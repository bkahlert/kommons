package koodies.concurrent

import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Processors
import koodies.concurrent.process.processSynchronously
import koodies.text.LineSeparators
import java.nio.file.Path


/**
 * Runs the specified [commandLine] optionally checking the specified [expectedExitValue].
 *
 * **Important:** This function does just that—starting the process. To do something with the returned [ManagedProcess]
 * you can use one of the provided [Processors] or implement one on your own, e.g.
 * - `process(...).process()` defaults to [Processors.noopProcessor] which prints all [IO] to the console
 * - `process(...).process { io -> doSomething(io) }` to process the [IO] the way you like
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
fun process(
    commandLine: CommandLine,
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
): ManagedProcess = commandLine.toManagedProcess(expectedExitValue, processTerminationCallback)

/**
 * Runs the specified [command] using the specified [arguments] in `this` working directory.
 *
 * Optionally [redirects], the [environment] and the [expectedExitValue] can be provided.
 *
 * **Important:** This function does just that—starting the process. To do something with the returned [ManagedProcess]
 * you can use one of the provided [Processors] or implement one on your own, e.g.
 * - `process(...).process()` defaults to [Processors.noopProcessor] which prints all [IO] to the console
 * - `process(...).process { io -> doSomething(io) }` to process the [IO] the way you like
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
fun Path.process(
    command: String,
    vararg arguments: String,
    redirects: List<String> = emptyList(),
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
): ManagedProcess = process(CommandLine(redirects, environment, this, command, arguments.toList()), expectedExitValue, processTerminationCallback)

/**
 * Returns (and possibly blocks until finished) the output of `this` [ManagedProcess].
 */
fun ManagedProcess.output(
): String = run {
    processSynchronously()
    ioLog.logged.filter { it.type == IO.Type.OUT }.joinToString(LineSeparators.LF) { it.unformatted }
}
