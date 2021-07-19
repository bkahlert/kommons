package koodies.exec

import koodies.Koodies
import koodies.io.randomPath
import koodies.shell.ShellScript
import java.net.URI
import java.nio.file.Path

private val shellScriptDir: Path = Koodies.ExecTemp
private const val shellScriptExtension: String = ".sh"

/**
 * Creates an [URI] pointing to a valid script
 * that would execute this [Executable].
 */
public fun Executable<*>.toLink(): URI =
    when (this) {
        is CommandLine -> toLink()
        is ShellScript -> toLink()
        else -> toCommandLine().toLink()
    }

/**
 * Creates an [URI] pointing to a valid script
 * that would execute this [CommandLine].
 */
public fun CommandLine.toLink(): URI =
    ShellScript(name) {
        shebang
        !this@toLink.shellCommand
    }.toLink()

/**
 * Creates an [URI] pointing to a valid script
 * that would execute this [ShellScript].
 */
public fun ShellScript.toLink(): URI =
    toFile(shellScriptDir.randomPath(
        base = "",
        extension = shellScriptExtension
    )).toUri()
