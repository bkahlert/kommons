package koodies.exec

import koodies.io.path.Locations
import koodies.io.path.randomPath
import koodies.jvm.deleteOldTempFilesOnExit
import koodies.shell.ShellScript
import java.net.URI
import java.nio.file.Path
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

private val shellScriptDir: Path by lazy { Locations.Temp.resolve("com.bkahlert.koodies") }
private const val shellScriptPrefix: String = "koodies.exec."
private const val shellScriptExtension: String = ".sh"

private var cleanupHandlerRegistered = false
private fun registerCleanupHandler(): Unit {
    if (!cleanupHandlerRegistered) {
        cleanupHandlerRegistered = true
        deleteOldTempFilesOnExit(shellScriptPrefix, shellScriptExtension, days(3), keepAtMost = 100, shellScriptDir)
        deleteOldTempFilesOnExit(shellScriptPrefix, shellScriptExtension, minutes(10), keepAtMost = 5)
    }
}

public fun ShellScript.toLink(): URI =
    shellScriptDir.randomPath(shellScriptPrefix, shellScriptExtension)
        .also { toFile(it) }
        .toUri()
        .also { registerCleanupHandler() }
