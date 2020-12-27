package koodies.concurrent

import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Processor
import koodies.concurrent.process.Processors
import koodies.concurrent.process.processSynchronously
import koodies.io.path.Locations
import koodies.io.path.randomPath
import koodies.shell.ShellScript
import koodies.shell.ShellScript.Companion.build
import java.nio.file.Path
import kotlin.io.path.name

private const val shellScriptPrefix: String = "koodies.process."
private const val shellScriptExtension: String = ".sh"

internal fun Path.scriptPath(): Path = randomPath(base = shellScriptPrefix, extension = shellScriptExtension)
internal fun Path.isScriptFile(): Boolean = name.startsWith(shellScriptPrefix) && name.endsWith(shellScriptExtension)


fun Path.script(
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int = 0,
    processTerminationCallback: (() -> Unit)? = null,
    shellScript: ShellScript,
    processor: Processor<ManagedProcess> = Processors.noopProcessor(),
): ManagedProcess {
    val scriptFile = shellScript.sanitize(this).buildTo(scriptPath())
    val commandLine = CommandLine(environment, this, scriptFile)
    return process(commandLine, expectedExitValue, processTerminationCallback).processSynchronously(processor)
}

fun script(
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int = 0,
    processTerminationCallback: (() -> Unit)? = null,
    shellScript: ShellScript,
    processor: Processor<ManagedProcess> = Processors.noopProcessor(),
): ManagedProcess = Locations.Temp.script(environment, expectedExitValue, processTerminationCallback, shellScript, processor)


fun Path.script(
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int = 0,
    processTerminationCallback: (() -> Unit)? = null,
    processor: Processor<ManagedProcess> = Processors.noopProcessor(),
    shellScript: ShellScript.() -> Unit,
): ManagedProcess = script(environment, expectedExitValue, processTerminationCallback, shellScript.build(), processor)

fun script(
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int = 0,
    processTerminationCallback: (() -> Unit)? = null,
    processor: Processor<ManagedProcess> = Processors.noopProcessor(),
    shellScript: ShellScript.() -> Unit,
): ManagedProcess = script(environment, expectedExitValue, processTerminationCallback, shellScript.build(), processor)


fun scriptOutputContains(command: String, substring: String, caseSensitive: Boolean = false): Boolean = runCatching {
    val flags = if (caseSensitive) "" else "i"
    val shellScript = ShellScript { line("$command | grep -q$flags '$substring'") }
    check(Locations.Temp.script(shellScript = shellScript).waitFor() == 0)
}.isSuccess
