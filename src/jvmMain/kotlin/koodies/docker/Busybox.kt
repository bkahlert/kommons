package koodies.docker

import koodies.builder.Init
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.CommandLine.Companion.CommandLineContext
import koodies.concurrent.process.Processor
import koodies.docker.DockerImage.ImageContext
import koodies.shell.HereDocBuilder.hereDoc
import koodies.text.withRandomSuffix
import koodies.toBaseName

/**
 * Prepares a [DockerRunCommandLine] that runs a dockerized [busybox](https://hub.docker.com/_/busybox)
 * with [name] as the name of the container.
 */
public fun Docker.busybox(
    name: String = "busybox".withRandomSuffix(),
    processor: Processor<DockerProcess>?,
    init: Init<CommandLineContext>,
): DockerProcess =
    docker({
        image { official("busybox") }
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
    processor: Processor<DockerProcess>?,
): DockerProcess =
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
    image: ImageContext.() -> DockerImage = { official("busybox") },
    processor: Processor<DockerProcess>?,
    init: Init<CommandLineContext>,
): DockerProcess =
    docker({
        image(image)
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
    processor: Processor<DockerProcess>? = null,
): DockerProcess =
    busybox(image, processor) {
        if (lines.isNotEmpty()) {
            arguments {
                +hereDoc { addAll(lines) }
            }
        }
    }

