package koodies.docker

import koodies.builder.Init
import koodies.concurrent.process.Processor
import koodies.docker.DockerImage.ImageContext
import koodies.exec.CommandLine
import koodies.exec.CommandLine.Companion.CommandLineContext
import koodies.shell.HereDocBuilder.hereDoc
import koodies.text.withRandomSuffix
import koodies.toBaseName

/**
 * Prepares a [DockerRunCommandLine] that runs a dockerized [busybox](https://hub.docker.com/_/busybox)
 * with [name] as the name of the container.
 */
public fun Docker.busybox(
    name: String = "busybox".withRandomSuffix(),
    processor: Processor<DockerExec>?,
    init: Init<CommandLineContext>,
): DockerExec =
    docker({
        image { "busybox" }
        options { name { name } }
        commandLine(init)
    }, processor = processor)

/**
 * Prepares a [DockerRunCommandLine] that runs a dockerized [busybox](https://hub.docker.com/_/busybox)
 * with [name] as the name of the container and [lines] as the arguments passed to busybox.
 */
public fun Docker.busybox(
    name: String = "busybox".withRandomSuffix(),
    vararg lines: String,
    processor: Processor<DockerExec>?,
): DockerExec =
    busybox(name, processor) {
        if (lines.isNotEmpty()) {
            arguments {
                +hereDoc { addAll(lines) }
            }
        }
    }

/**
 * Prepares a [DockerRunCommandLine] that runs a dockerized [busybox](https://hub.docker.com/_/busybox)
 * with [name] as the name of the container.
 */
public fun Docker.busybox(
    image: DockerImageInit = { "busybox" },
    processor: Processor<DockerExec>?,
    init: Init<CommandLineContext>,
): DockerExec =
    docker({
        images(image)
        options { name { CommandLine(init).summary.toBaseName() } }
        commandLine(init)
    }, processor = processor)

/**
 * Prepares a [DockerRunCommandLine] that runs a dockerized [busybox](https://hub.docker.com/_/busybox)
 * with [name] as the name of the container and [lines] as the arguments passed to busybox.
 */
public fun Docker.busybox(
    image: ImageContext.() -> DockerImage,
    vararg lines: String,
    processor: Processor<DockerExec>? = null,
): DockerExec =
    busybox(image, processor) {
        if (lines.isNotEmpty()) {
            arguments {
                +hereDoc { addAll(lines) }
            }
        }
    }
