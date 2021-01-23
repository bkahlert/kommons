package koodies.docker

import koodies.builder.ListBuilder.Companion.buildList
import koodies.builder.ListBuilderInit
import koodies.builder.build
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.CommandLineBuilder
import koodies.io.file.resolveBetweenFileSystems
import koodies.io.path.asPath
import koodies.io.path.asString
import koodies.io.path.isSubPathOf
import koodies.text.splitAndMap
import java.nio.file.Path
import kotlin.io.path.relativeTo

open class DockerCommandLine private constructor(
    val image: DockerImage,
    val options: DockerCommandLineOptions,
    commandLine: CommandLine,
    dummy: Any?,
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
        if (commandLine.command.isNotBlank() && options.entryPoint == null) {
            +commandLine.command
        }
        +options.remapPathsInArguments(commandLine)
    },
) {
    constructor(image: DockerImage, options: DockerCommandLineOptions = DockerCommandLineOptions(), commandLine: CommandLine) : this(
        image = image,
        options = options.withFallbackWorkingDirectory(commandLine.workingDirectory),
        commandLine = commandLine,
        dummy = null,
    )

    companion object {

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

open class DockerCommandLineBuilder(
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
    val workingDirectory: ContainerPath? = null,
    val autoCleanup: Boolean = true,
    val interactive: Boolean = true,
    val pseudoTerminal: Boolean = false,
    val mounts: MountOptions = MountOptions(),
) : List<String> by (buildList {
    entryPoint?.also { +"--entrypoint" + entryPoint }
    name?.also { +"--name" + name.sanitized }
    privileged.takeIf { it }?.also { +"--privileged" }
    workingDirectory?.also { +"-w" + it.asString() }
    autoCleanup.takeIf { it }?.also { +"--rm" }
    interactive.takeIf { it }?.also { +"-i" }
    pseudoTerminal.takeIf { it }?.also { +"-t" }
    mounts.forEach { +it }
}) {
    /**
     * Checks if this strings represents a path accessible by one of the [MountOptions]
     * of the specified [DockerCommandLineOptions] and if so, returns the mapped [ContainerPath].
     */
    fun mapToContainerPathOrNull(hostPath: HostPath): ContainerPath? =
        kotlin.runCatching { mounts.mapToContainerPath(hostPath) }.getOrNull()

    /**
     * Tries to find all paths inside the [commandLine] [CommandLine.arguments] and remaps all those
     * that are still accessible through the specified [mounts].
     *
     * Relative paths are resolved using the [CommandLine.workingDirectory] and if specified
     * mapped backed to a relative [ContainerPath] using [DockerCommandLineOptions.workingDirectory].
     *
     * Arguments not containing paths are left unchanged.
     *
     * Arguments of the form `a=b` get mapped with key and value treated separately.
     */
    fun remapPathsInArguments(commandLine: CommandLine): List<String> = commandLine.arguments.map { arg ->
        if (arg.count { it == '=' } > 1) return@map arg
        arg.splitAndMap("=") {
            val originalPath = asHostPath() // e.g. /a/b resp. b
            val absoluteOriginalPath = commandLine.workingDirectory.resolveBetweenFileSystems(originalPath) // e.g. /a/b resp. /a/b (if pwd=/a)
            mapToContainerPathOrNull(absoluteOriginalPath)?.let { mappedPath ->   // e.g. /c/d
                workingDirectory
                    ?.takeIf { !originalPath.isAbsolute }
                    ?.let(mappedPath::relativeTo) // e.g. b (if container pwd=/c)
                    ?: mappedPath.asString() // e.g. /c/d
            } ?: this
        }
    }

    fun withFallbackWorkingDirectory(fallbackWorkingDirectory: HostPath): DockerCommandLineOptions {
        if (workingDirectory == null) {
            val mappedFallbackWorkingDirectory = mapToContainerPathOrNull(fallbackWorkingDirectory)
            if (mappedFallbackWorkingDirectory != null) return copy(workingDirectory = mappedFallbackWorkingDirectory)
        }
        return this
    }
}

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
    fun workingDirectory(workingDirectory: () -> ContainerPath?) = options.copy(workingDirectory = workingDirectory()).run { options = this }
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
        val relativePath = containerPath.relativeTo(target)
        return source.resolve(relativePath)
    }

    fun mapToContainerPath(hostPath: HostPath): ContainerPath {
        require(hostPath.isSubPathOf(source)) { "$hostPath is not mapped by $source" }
        val relativePath = hostPath.relativeTo(source).asString()
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
    private val absolutePath: Path
        get() {
            require(containerPath.isAbsolute) { "$containerPath must be absolute." }
            return containerPath.toAbsolutePath()
        }

    fun relativeTo(baseContainerPath: ContainerPath): String =
        absolutePath.relativeTo(baseContainerPath.absolutePath).asString()

    fun isSubPathOf(baseContainerPath: ContainerPath): Boolean =
        absolutePath.isSubPathOf(baseContainerPath.absolutePath)

    fun resolve(other: ContainerPath): ContainerPath =
        absolutePath.resolve(other.absolutePath).asContainerPath()

    fun resolve(other: String): ContainerPath =
        absolutePath.resolve(other).asContainerPath()

    fun mapToHostPath(mountOptions: MountOptions) =
        mountOptions.mapToHostPath(this)

    fun asString() = absolutePath.asString()

    override fun toString(): String = asString()
}

fun String.asContainerPath() = ContainerPath(asPath())
fun Path.asContainerPath() = ContainerPath(this)

typealias HostPath = Path

fun HostPath.mapToContainerPath(mountOptions: MountOptions) =
    mountOptions.mapToContainerPath(this)

fun String.asHostPath(): HostPath = asPath()
