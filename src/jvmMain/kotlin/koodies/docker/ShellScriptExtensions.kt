package koodies.docker

import koodies.shell.ShellScript


/**
 * Extends [ShellScript] with an entry point to build docker commands.
 */
fun ShellScript.docker(init: DockerImage.Builder.() -> DockerImage): ShellScriptAttachingBuilder =
    ShellScriptAttachingBuilder(this, dockerImage(init))

/**
 * Builder that adds the built commands directly to the [shellScript].
 */
class ShellScriptAttachingBuilder(private val shellScript: ShellScript, private val image: DockerImage) {
    infix fun run(init: DockerRunCommandLineBuilder.() -> Unit): Unit =
        DockerRunCommandLineBuilder.build(image, init).run { shellScript.command(this) }
}
