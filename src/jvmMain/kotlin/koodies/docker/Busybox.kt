package koodies.docker

import koodies.builder.Init
import koodies.concurrent.process.CommandLine.Companion.CommandLineContext
import koodies.concurrent.process.Processor
import koodies.concurrent.process.Processors
import koodies.shell.HereDocBuilder.hereDoc
import koodies.text.withRandomSuffix

/**
 * Prepares a [DockerRunCommandLine] that runs a dockerized [busybox](https://hub.docker.com/_/busybox)
 * with [name] as the name of the container.
 */
fun Docker.busybox(
    name: String = "busybox".withRandomSuffix(),
    processor: Processor<DockerProcess> = Processors.consoleLoggingProcessor(),
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
fun Docker.busybox(
    name: String = "busybox".withRandomSuffix(),
    vararg lines: String,
    processor: Processor<DockerProcess> = Processors.consoleLoggingProcessor(),
): DockerProcess =
    busybox(name, processor) {
        if (lines.isNotEmpty()) {
            arguments {
                +hereDoc { addAll(lines) }
            }
        }
    }
