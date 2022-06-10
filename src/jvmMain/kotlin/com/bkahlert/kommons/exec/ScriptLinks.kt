package com.bkahlert.kommons.exec

import com.bkahlert.kommons.Kommons
import com.bkahlert.kommons.createTempFile
import com.bkahlert.kommons.shell.ShellScript
import java.net.URI
import java.nio.file.Path

private val shellScriptDir: Path = Kommons.ExecTemp
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
    toFile(
        shellScriptDir.createTempFile(
            prefix = "",
            suffix = shellScriptExtension
        )
    ).toUri()
