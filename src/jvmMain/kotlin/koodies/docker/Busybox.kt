package koodies.docker

import koodies.concurrent.process.CommandLineBuilder
import koodies.shell.HereDocBuilder.hereDoc
import koodies.text.withRandomSuffix

/**
 * Prepares a [DockerCommandLine] that runs a dockerized [busybox](https://hub.docker.com/_/busybox)
 * with [name] as the name of the container.
 */
fun Docker.busybox(name: String = "busybox".withRandomSuffix(), init: CommandLineBuilder.() -> Unit): DockerCommandLine =
    DockerCommandLine.build({ "busybox" }) {
        options { name { name } }
        commandLine(init)
    }

/**
 * Prepares a [DockerCommandLine] that runs a dockerized [busybox](https://hub.docker.com/_/busybox)
 * with [name] as the name of the container and [lines] as the arguments passed to busybox.
 */
fun Docker.busybox(name: String = "busybox".withRandomSuffix(), vararg lines: String): DockerCommandLine =
    busybox(name) {
        if (lines.isNotEmpty()) {
            arguments {
                +hereDoc { +lines }
            }
        }
    }
