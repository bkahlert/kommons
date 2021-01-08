package koodies.concurrent

import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.processSynchronously
import koodies.text.LineSeparators
import java.nio.file.Path

fun process(
    commandLine: CommandLine,
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
): ManagedProcess = commandLine.toManagedProcess(expectedExitValue, processTerminationCallback)

fun Path.process(
    command: String,
    vararg arguments: String,
    redirects: List<String> = emptyList(),
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
): ManagedProcess = process(CommandLine(redirects, environment, this, command, arguments.toList()), expectedExitValue, processTerminationCallback)

fun ManagedProcess.output(
): String = run {
    processSynchronously()
    ioLog.logged.filter { it.type == IO.Type.OUT }.joinToString(LineSeparators.LF) { it.unformatted }
}
