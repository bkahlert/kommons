package koodies.docker

interface DockerRunAdaptable {
    fun adapt(): DockerRunCommandLine
}
