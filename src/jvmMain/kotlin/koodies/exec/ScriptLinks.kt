package koodies.exec

import koodies.io.Koodies
import koodies.io.randomPath
import koodies.shell.ShellScript
import java.net.URI
import java.nio.file.Path

private val shellScriptDir: Path = Koodies.ExecTemp
private const val shellScriptPrefix: String = "koodies.exec."
private const val shellScriptExtension: String = ".sh"

public fun ShellScript.toLink(): URI =
    shellScriptDir.randomPath(shellScriptPrefix, shellScriptExtension)
        .also { toFile(it) }
        .toUri()
