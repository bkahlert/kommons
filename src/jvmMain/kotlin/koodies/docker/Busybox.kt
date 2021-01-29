package koodies.docker

import koodies.concurrent.process.CommandLineBuilder
import koodies.concurrent.process.Processor
import koodies.concurrent.process.Processors
import koodies.shell.HereDocBuilder.hereDoc
import koodies.text.withRandomSuffix

/**
 * Prepares a [DockerCommandLine] that runs a dockerized [busybox](https://hub.docker.com/_/busybox)
 * with [name] as the name of the container.
 */
fun Docker.busybox(
    name: String = "busybox".withRandomSuffix(),
    processor: Processor<DockerProcess> = Processors.consoleLoggingProcessor(),
    init: CommandLineBuilder.() -> Unit,
): DockerProcess =
    docker({ "busybox" }, processor) {
        options { name { name } }
        commandLine(init)
    }

/**
 * Prepares a [DockerCommandLine] that runs a dockerized [busybox](https://hub.docker.com/_/busybox)
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
                +hereDoc { +lines }
            }
        }
    }
