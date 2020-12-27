package koodies.docker

import koodies.text.withRandomSuffix
import java.nio.file.Path

/**
 * Prepares a [DockerRunCommandLine] that runs a dockerized [busybox](https://hub.docker.com/_/busybox)
 * with [name] as the name of the container and [disk] as the file containing the filesystem.
 */
fun Docker.pi(name: String = "busybox".withRandomSuffix(), disk: Path, init: DockerRunCommandLineBuilder.() -> Unit = {}): DockerRunCommandLine {
    @Suppress("SpellCheckingInspection")
    return image { "lukechilds" / "dockerpi" tag "vm" }.run {
        options {
            name { name }
            mounts { disk mountAt "/sdcard/filesystem.img" }
        }
        apply(init)
    }
}
