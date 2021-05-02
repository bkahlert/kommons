package koodies.exec

import koodies.io.path.Locations
import koodies.io.path.randomPath
import koodies.jvm.deleteOldTempFilesOnExit
import koodies.shell.ShellScript
import java.net.URI
import java.nio.file.Path
import kotlin.time.days
import kotlin.time.minutes

private val shellScriptDir: Path by lazy { Locations.Temp.resolve("com.bkahlert.koodies") }
private const val shellScriptPrefix: String = "koodies.exec."
private const val shellScriptExtension: String = ".sh"

private var cleanupHandlerRegistered = false
private fun registerCleanupHandler(): Unit {
    if (!cleanupHandlerRegistered) {
        cleanupHandlerRegistered = true
        deleteOldTempFilesOnExit(shellScriptPrefix, shellScriptExtension, 3.days, keepAtMost = 100, shellScriptDir)
        deleteOldTempFilesOnExit(shellScriptPrefix, shellScriptExtension, 10.minutes, keepAtMost = 5)
    }
}

public fun ShellScript.toLink(): URI =
    shellScriptDir.randomPath(shellScriptPrefix, shellScriptExtension)
        .also { sanitize().buildTo(it) }
        .toUri()
        .also { registerCleanupHandler() }
