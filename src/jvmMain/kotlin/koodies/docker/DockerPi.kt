package koodies.docker

import koodies.builder.Init
import koodies.exec.CommandLine.Companion.CommandLineContext
import koodies.text.withRandomSuffix
import java.nio.file.Path

/**
 * Prepares a [DockerRunCommandLine] that runs a dockerized [busybox](https://hub.docker.com/_/busybox)
 * with [name] as the name of the container and [disk] as the file containing the filesystem.
 */
public fun Docker.pi(name: String = "busybox".withRandomSuffix(), disk: Path, init: Init<CommandLineContext> = {}): DockerRunCommandLine {
    @Suppress("SpellCheckingInspection")
    return DockerRunCommandLine {

        image { "lukechilds" / "dockerpi" tag "vm" }
        options {
            name { name }
            mounts { disk mountAt "/sdcard/filesystem.img" }
        }
        commandLine(init)
    }
}
