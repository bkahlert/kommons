package koodies.docker

import koodies.builder.mapBuild
import koodies.callable
import koodies.shell.ShellScript

/**
 * Extends [ShellScript] with an entry point to build docker commands.
 */
val ShellScript.docker
    get(): ShellScriptAttachingBuilder =
        ShellScriptAttachingBuilder(this)

/**
 * Builder that adds the built commands directly to the [shellScript].
 */
class ShellScriptAttachingBuilder(private val shellScript: ShellScript) {
    val run by callable(DockerRunCommandLine.mapBuild { also { shellScript.command(it) } })
    val stop by callable(DockerStopCommandLine.mapBuild { also { shellScript.command(it) } })
}
