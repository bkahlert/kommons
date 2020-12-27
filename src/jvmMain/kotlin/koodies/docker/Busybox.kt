package koodies.docker

import koodies.shell.HereDocBuilder.hereDoc
import koodies.text.withRandomSuffix

/**
 * Prepares a [DockerRunCommandLine] that runs a dockerized [busybox](https://hub.docker.com/_/busybox)
 * with [name] as the name of the container.
 */
fun Docker.busybox(name: String = "busybox".withRandomSuffix(), init: DockerRunCommandLineBuilder.() -> Unit): DockerRunCommandLine {
    return image { "busybox" }.run {
        options { name { name } }
        apply(init)
    }
}

/**
 * Prepares a [DockerRunCommandLine] that runs a dockerized [busybox](https://hub.docker.com/_/busybox)
 * with [name] as the name of the container and [lines] as the arguments passed to busybox.
 */
fun Docker.busybox(name: String = "busybox".withRandomSuffix(), vararg lines: String): DockerRunCommandLine {
    return busybox(name) {
        if (lines.isNotEmpty()) {
            arguments {
                +hereDoc { +lines }
            }
        }
    }
}
