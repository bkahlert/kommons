package koodies.docker

import koodies.builder.ListBuilder.Companion.buildList
import koodies.builder.ListBuilderInit
import koodies.builder.build
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.CommandLineBuilder
import koodies.io.path.asPath
import koodies.io.path.asString
import koodies.io.path.isSubPathOf
import java.nio.file.Path
import kotlin.io.path.relativeTo

class DockerCommandLine(
    val image: DockerImage,
    val options: DockerCommandLineOptions = DockerCommandLineOptions(),
    commandLine: CommandLine,
) : CommandLine(
    redirects = commandLine.redirects,
    environment = commandLine.environment,
    workingDirectory = commandLine.workingDirectory,
    command = "docker",
    arguments = buildList {
        +"run"
        commandLine.environment.forEach {
            +"--env"
            +"${it.key}=${it.value}"
        }
        +options
        +image.formatted
        if (commandLine.command.isNotBlank() && options.entryPoint == null) +commandLine.command
        +commandLine.arguments.map { arg ->
            arg.split("=").map { it.mapToContainerPathOrNull(options.mounts)?.asString() ?: it }.joinToString("=")
        }
    },
) {
    companion object {
        private fun String.mapToContainerPathOrNull(mounts: MountOptions): ContainerPath? =
            kotlin.runCatching { mounts.mapToContainerPath(asHostPath()) }.getOrNull()

        fun build(image: DockerImage, init: DockerCommandLineBuilder.() -> Unit = {}): DockerCommandLine =
            DockerCommandLineBuilder.build(image, init = init)

        fun build(imageInit: DockerImageBuilder.() -> Any, init: DockerCommandLineBuilder.() -> Unit): DockerCommandLine =
            DockerCommandLineBuilder.build(DockerImageBuilder.build(imageInit), init)
    }

    override fun toManagedProcess(expectedExitValue: Int?, processTerminationCallback: (() -> Unit)?): DockerProcess =
        DockerProcess.from(this, expectedExitValue, processTerminationCallback)

    override fun prepare(expectedExitValue: Int): DockerProcess =
        super.prepare(expectedExitValue) as DockerProcess

    override fun execute(expectedExitValue: Int): DockerProcess =
        super.execute(expectedExitValue) as DockerProcess
}


fun DockerImage.buildCommandLine(init: DockerCommandLineBuilder.() -> Unit) =
    DockerCommandLineBuilder.build(image = this, init = init)

class DockerCommandLineBuilder(
    private var options: DockerCommandLineOptions = DockerCommandLineOptions(),
    private var commandLine: CommandLine = CommandLine.build(""),
) {
    companion object {
        fun build(image: DockerImage, init: DockerCommandLineBuilder.() -> Unit): DockerCommandLine =
            DockerCommandLineBuilder().apply(init).run {
                DockerCommandLine(image, options, commandLine)
            }
    }

    fun options(options: DockerCommandLineOptions) = options.also { this.options = it }
    fun options(init: DockerCommandLineOptionsBuilder.() -> Unit) = options(DockerCommandLineOptionsBuilder.build(init))
    fun commandLine(commandLine: CommandLine) = commandLine.also { this.commandLine = it }
    fun commandLine(init: CommandLineBuilder.() -> Unit) = commandLine(CommandLine.build("", init))
    fun commandLine(command: String, init: CommandLineBuilder.() -> Unit) = commandLine(CommandLine.build(command, init))
}

data class DockerCommandLineOptions(
    val entryPoint: String? = null,
    val name: DockerContainerName? = null,
    val privileged: Boolean = false,
    val autoCleanup: Boolean = true,
    val interactive: Boolean = true,
    val pseudoTerminal: Boolean = false,
    val mounts: MountOptions = MountOptions(),
) : List<String> by (buildList {
    entryPoint?.also { +"--entrypoint" + entryPoint }
    name?.also { +"--name" + name.sanitized }
    privileged.takeIf { it }?.also { +"--privileged" }
    autoCleanup.takeIf { it }?.also { +"--rm" }
    interactive.takeIf { it }?.also { +"-i" }
    pseudoTerminal.takeIf { it }?.also { +"-t" }
    mounts.forEach { +it }
})

@DockerCommandLineDsl
abstract class DockerCommandLineOptionsBuilder {

    companion object {
        inline fun build(init: DockerCommandLineOptionsBuilder.() -> Unit): DockerCommandLineOptions {
            var options = DockerCommandLineOptions()
            object : DockerCommandLineOptionsBuilder() {
                override var options: DockerCommandLineOptions
                    get() = options
                    set(value) = value.run { options = this }
            }.apply(init)
            return options
        }
    }

    protected abstract var options: DockerCommandLineOptions

    fun entrypoint(entryPoint: () -> String?) = options.copy(entryPoint = entryPoint()).run { options = this }
    fun name(name: () -> String?) = options.copy(name = name()?.let { DockerContainerName(it) }).run { options = this }
    fun containerName(name: () -> DockerContainerName?) = options.copy(name = name()).run { options = this }
    fun privileged(privileged: () -> Boolean) = options.copy(privileged = privileged()).run { options = this }
    fun autoCleanup(autoCleanup: () -> Boolean) = options.copy(autoCleanup = autoCleanup()).run { options = this }
    fun interactive(interactive: () -> Boolean) = options.copy(interactive = interactive()).run { options = this }
    fun pseudoTerminal(pseudoTerminal: () -> Boolean) = options.copy(pseudoTerminal = pseudoTerminal()).run { options = this }
    fun mounts(init: ListBuilderInit<MountOption>) = init.build().also { options.copy(mounts = MountOptions(options.mounts + it)).run { options = this } }

    infix fun HostPath.mountAt(target: String) = mountAt(target.asContainerPath())

    infix fun HostPath.mountAt(target: ContainerPath) {
        mounts {
            +MountOption(source = this@mountAt, target = target)
        }
    }

    override fun toString(): String = options.toString()
}

data class MountOption(val type: String = "bind", val source: HostPath, val target: ContainerPath) :
    AbstractList<String>() {
    private val list = listOf("--mount", "type=$type,source=${source.asString()},target=${target.asString()}")

    override val size: Int = list.size
    override fun get(index: Int): String = list[index]

    fun mapToHostPath(containerPath: ContainerPath): HostPath {
        require(containerPath.isSubPathOf(target)) { "$containerPath is not mapped by $target" }
        val relativePath = containerPath.relativeTo(target).asString()
        return source.resolve(relativePath)
    }

    fun mapToContainerPath(hostPath: HostPath): ContainerPath {
        require(hostPath.isSubPathOf(source)) { "$hostPath is not mapped by $source" }
        val relativePath = hostPath.relativeTo(source).asContainerPath()
        return target.resolve(relativePath)
    }
}

class MountOptions(private val mountOptions: List<MountOption>) : AbstractList<MountOption>() {
    constructor(vararg mountOptions: MountOption) : this(mountOptions.toList())

    override val size: Int = mountOptions.size
    override fun get(index: Int): MountOption = mountOptions[index]

    fun mapToHostPath(containerPath: ContainerPath): HostPath {
        val mappedHostPaths = mapNotNull { mountOption ->
            kotlin.runCatching { mountOption.mapToHostPath(containerPath) }.getOrNull()
        }
        require(mappedHostPaths.isNotEmpty()) { "$containerPath is not mapped by any of ${map { it.target }.joinToString(", ")}" }
        return mappedHostPaths.first()
    }

    fun mapToContainerPath(hostPath: HostPath): ContainerPath {
        val mappedContainerPaths = mapNotNull { mountOption ->
            kotlin.runCatching { mountOption.mapToContainerPath(hostPath) }.getOrNull()
        }
        require(mappedContainerPaths.isNotEmpty()) { "$hostPath is not mapped by any of ${map { it.source }.joinToString(", ")}" }
        return mappedContainerPaths.first()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MountOptions

        if (mountOptions != other.mountOptions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + mountOptions.hashCode()
        return result
    }


}

inline class ContainerPath(private val containerPath: Path) {
    fun relativeTo(baseContainerPath: ContainerPath): ContainerPath =
        containerPath.relativeTo(baseContainerPath.containerPath).asContainerPath()

    fun isSubPathOf(baseContainerPath: ContainerPath): Boolean =
        containerPath.isSubPathOf(baseContainerPath.containerPath)

    fun resolve(other: ContainerPath): ContainerPath =
        containerPath.resolve(other.containerPath).asContainerPath()

    fun mapToHostPath(mountOptions: MountOptions) =
        mountOptions.mapToHostPath(this)

    fun asString() = containerPath.asString()

    override fun toString(): String = asString()
}

fun String.asContainerPath() = ContainerPath(asPath())
fun Path.asContainerPath() = ContainerPath(this)

typealias HostPath = Path

fun HostPath.mapToContainerPath(mountOptions: MountOptions) =
    mountOptions.mapToContainerPath(this)

fun String.asHostPath(): HostPath = asPath()
