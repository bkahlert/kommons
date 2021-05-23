package koodies.exec

import koodies.io.Locations
import koodies.io.path.randomPath
import koodies.shell.ShellScript
import java.net.URI
import java.nio.file.Path

private val shellScriptDir: Path by lazy { Locations.ExecTemp.path }
private const val shellScriptPrefix: String = "koodies.exec."
private const val shellScriptExtension: String = ".sh"

public fun ShellScript.toLink(): URI =
    shellScriptDir.randomPath(shellScriptPrefix, shellScriptExtension)
        .also { toFile(it) }
        .toUri()
