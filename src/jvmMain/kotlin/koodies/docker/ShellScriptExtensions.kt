package koodies.docker

import koodies.builder.Init
import koodies.builder.mapBuild
import koodies.callable
import koodies.docker.DockerRunCommandLine.Companion.DockerRunCommandContext
import koodies.docker.DockerStopCommandLine.Companion.StopContext
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
    public val run: (Init<DockerRunCommandContext> /* = koodies.docker.DockerRunCommandLine.Companion.DockerRunCommandContext.() -> kotlin.Unit */) -> Unit by callable(
        DockerRunCommandLine.mapBuild { shellScript.command(it) })
    public val stop: (Init<StopContext> /* = koodies.docker.DockerStopCommandLine.Companion.StopContext.() -> kotlin.Unit */) -> Unit by callable(
        DockerStopCommandLine.mapBuild { shellScript.command(it) })
}
