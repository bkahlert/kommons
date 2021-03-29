package koodies.docker

import koodies.builder.Init
import koodies.builder.mapBuild
import koodies.callable
import koodies.shell.ShellScript

/**
 * Extends [ShellScript] with an entry point to build docker commands.
 */
public val ShellScript.docker: ShellScriptAttachingBuilder
    get(): ShellScriptAttachingBuilder =
        ShellScriptAttachingBuilder(this)

/**
 * Builder that adds the built commands directly to the [shellScript].
 */
public class ShellScriptAttachingBuilder(private val shellScript: ShellScript) {
    public val run: (Init<DockerRunCommandLine.Companion.CommandContext> /* = koodies.docker.DockerRunCommandLine.Companion.DockerRunCommandContext.() -> kotlin.Unit */) -> Unit by callable(
        DockerRunCommandLine.mapBuild { shellScript.command(it) })
    public val stop: (Init<DockerStopCommandLine.Companion.CommandContext> /* = koodies.docker.DockerStopCommandLine.Companion.StopContext.() -> kotlin.Unit */) -> Unit by callable(
        DockerStopCommandLine.mapBuild { shellScript.command(it) })
}
