package koodies.docker

import koodies.builder.BooleanBuilder.BooleanValue
import koodies.builder.BooleanBuilder.OnOff
import koodies.builder.BooleanBuilder.OnOff.Context
import koodies.builder.BuilderTemplate
import koodies.builder.ListBuilder
import koodies.builder.ListBuilder.Companion.buildList
import koodies.builder.StatelessBuilder
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.ListBuildingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.CommandLine.Companion.CommandLineContext
import koodies.docker.DockerImage.ImageContext
import koodies.docker.DockerRunCommandLine.Companion.DockerRunCommandContext
import koodies.docker.DockerRunCommandLineOptions.Companion.OptionsContext
import koodies.docker.MountOptions.Companion.CollectingMountOptionsContext
import koodies.io.file.resolveBetweenFileSystems
import koodies.io.path.asPath
import koodies.io.path.asString
import koodies.io.path.isSubPathOf
import koodies.text.splitAndMap
import java.nio.file.Path
import kotlin.io.path.relativeTo

/**
 * [DockerCommandLine] that runs a command specified by its [redirects], [environment], [workingDirectory],
 * [command] and [arguments] using the specified [image] using the specified [options]
 */
public open class DockerRunCommandLine private constructor(
    /**
     * The image used to run this command line.
     */
    public val image: DockerImage,
    /**
     * Options that specify how this command line is run.
     */
    public val options: DockerRunCommandLineOptions,
    redirects: List<String>,
    environment: Map<String, String>,
    workingDirectory: Path,
    command: String,
    arguments: List<String>,
) : DockerCommandLine(
    redirects = redirects,
    environment = environment,
    workingDirectory = workingDirectory,
    dockerCommand = "run",
    arguments = buildList {
        environment.addAll { listOf("--env", "$key=$value") }
        addAll(options)
        add(image.toString())
        if (command.isNotBlank() && options.entryPoint == null) add(command)
        addAll(options.remapPathsInArguments(workingDirectory, arguments))
    },
) {
    public constructor(image: DockerImage, options: DockerRunCommandLineOptions = DockerRunCommandLineOptions(), commandLine: CommandLine) : this(
        image = image,
        options = options.withFallbackWorkingDirectory(commandLine.workingDirectory),
        redirects = commandLine.redirects,
        environment = commandLine.environment,
        workingDirectory = commandLine.workingDirectory,
        command = commandLine.command,
        arguments = commandLine.arguments,
    )

    public companion object : BuilderTemplate<DockerRunCommandContext, DockerRunCommandLine>() {
        @DockerCommandLineDsl
        public class DockerRunCommandContext(override val captures: CapturesMap) : CapturingContext() {
            public val image: SkippableCapturingBuilderInterface<ImageContext.() -> DockerImage, DockerImage?> by DockerImage
            public val options: SkippableCapturingBuilderInterface<OptionsContext.() -> Unit, DockerRunCommandLineOptions?> by DockerRunCommandLineOptions
            public val commandLine: SkippableCapturingBuilderInterface<CommandLineContext.() -> Unit, CommandLine?> by CommandLine
        }

        override fun BuildContext.build(): DockerRunCommandLine = ::DockerRunCommandContext {
            DockerRunCommandLine(
                ::image.eval(),
                ::options.evalOrDefault { DockerRunCommandLineOptions() },
                ::commandLine.evalOrDefault { CommandLine("") },
            )
        }
    }
}

public data class DockerRunCommandLineOptions(
    /**
     * [Detached (-d)][https://docs.docker.com/engine/reference/run/#detached--d]
     */
    val detached: Boolean = false,
    val entryPoint: String? = null,
    val name: DockerContainerName? = null,
    /**
     * Publish a container's port or a range of ports to the host
     * format: `ip:hostPort:containerPort` or `ip::containerPort` or
     * `hostPort:containerPort` or `containerPort`
     *
     * Both hostPort and containerPort can be specified as a
     * range of ports. When specifying ranges for both, the
     * number of container ports in the range must match the
     * number of host ports in the range, for example:
     * `1234-1236:1234-1236/tcp`
     *
     * When specifying a range for hostPort only, the
     * containerPort must not be a range.  In this case the
     * container port is published somewhere within the
     * specified hostPort range. (e.g., `1234-1236:1234/tcp`)
     *
     * @see <a href="https://docs.docker.com/engine/reference/run/#expose-incoming-ports">EXPOSE (incoming ports)</a>
     */
    val publish: List<String> = emptyList(),
    val privileged: Boolean = false,
    val workingDirectory: ContainerPath? = null,
    val autoCleanup: Boolean = true,
    val interactive: Boolean = true,
    val pseudoTerminal: Boolean = false,
    val mounts: MountOptions = MountOptions(emptyList()),
    val custom: List<String> = emptyList(),
) : List<String> by (buildList {
    detached.takeIf { it }?.also { add("-d") }
    entryPoint?.also { add("--entrypoint", entryPoint) }
    name?.also { add("--name", name.sanitized) }
    publish.forEach { p -> add("-p", p) }
    privileged.takeIf { it }?.also { add("--privileged") }
    workingDirectory?.also { add("-w", it.asString()) }
    autoCleanup.takeIf { it }?.also { add("--rm") }
    interactive.takeIf { it }?.also { add("-i") }
    pseudoTerminal.takeIf { it }?.also { add("-t") }
    mounts.addAll { this }
    custom.forEach { add(it) }
}) {
    /**
     * Checks if this strings represents a path accessible by one of the [MountOptions]
     * of the specified [DockerRunCommandLineOptions] and if so, returns the mapped [ContainerPath].
     */
    public fun mapToContainerPathOrNull(hostPath: HostPath): ContainerPath? =
        kotlin.runCatching { mounts.mapToContainerPath(hostPath) }.getOrNull()

    /**
     * Tries to find all paths found inside [arguments] and remaps all those
     * that are still accessible through the specified [mounts].
     *
     * Relative paths are resolved using the [argumentsWorkingDirectory] and if specified
     * mapped backed to a relative [ContainerPath] using [DockerRunCommandLineOptions.workingDirectory].
     *
     * Arguments not containing paths are left unchanged.
     *
     * Arguments of the form `a=b` get mapped with key and value treated separately.
     */
    public fun remapPathsInArguments(argumentsWorkingDirectory: Path, arguments: List<String>): List<String> = arguments.map { arg ->
        if (arg.count { it == '=' } > 1) return@map arg
        arg.splitAndMap("=") {
            val originalPath = asHostPath() // e.g. /a/b resp. b
            val absoluteOriginalPath = argumentsWorkingDirectory.resolveBetweenFileSystems(originalPath) // e.g. /a/b resp. /a/b (if pwd=/a)
            mapToContainerPathOrNull(absoluteOriginalPath)?.let { mappedPath ->   // e.g. /c/d
                workingDirectory
                    ?.takeIf { !originalPath.isAbsolute }
                    ?.let { mappedPath.relativeTo(it) } // e.g. b (if container pwd=/c)
                    ?: mappedPath.asString() // e.g. /c/d
            } ?: this
        }
    }

    public fun withFallbackWorkingDirectory(fallbackWorkingDirectory: HostPath): DockerRunCommandLineOptions {
        if (workingDirectory == null) {
            val mappedFallbackWorkingDirectory = mapToContainerPathOrNull(fallbackWorkingDirectory)
            if (mappedFallbackWorkingDirectory != null) return copy(workingDirectory = mappedFallbackWorkingDirectory)
        }
        return this
    }

    public companion object : BuilderTemplate<OptionsContext, DockerRunCommandLineOptions>() {
        @DockerCommandLineDsl
        public class OptionsContext(override val captures: CapturesMap) : CapturingContext() {
            /**
             * [Detached (-d)][https://docs.docker.com/engine/reference/run/#detached--d]
             */
            public val detached: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean?> by OnOff
            public val entrypoint: SkippableCapturingBuilderInterface<() -> String, String?> by builder<String>()
            public val name: SkippableCapturingBuilderInterface<() -> String, String?> by builder<String>()

            /**
             * Publish a container's port or a range of ports to the host
             * format: `ip:hostPort:containerPort` or `ip::containerPort` or
             * `hostPort:containerPort` or `containerPort`
             *
             * Both hostPort and containerPort can be specified as a
             * range of ports. When specifying ranges for both, the
             * number of container ports in the range must match the
             * number of host ports in the range, for example:
             * `1234-1236:1234-1236/tcp`
             *
             * When specifying a range for hostPort only, the
             * containerPort must not be a range.  In this case the
             * container port is published somewhere within the
             * specified hostPort range. (e.g., `1234-1236:1234/tcp`)
             *
             * @see <a href="https://docs.docker.com/engine/reference/run/#expose-incoming-ports">EXPOSE (incoming ports)</a>
             */
            public val publish: SkippableCapturingBuilderInterface<ListBuildingContext<String>.() -> Unit, List<String>?> by ListBuilder<String>()
            public val containerName: SkippableCapturingBuilderInterface<() -> String, String?> by builder<String>()
            public val privileged: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean?> by OnOff
            public val workingDirectory: SkippableCapturingBuilderInterface<() -> ContainerPath, ContainerPath?> by builder<ContainerPath>()
            public val autoCleanup: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean?> by OnOff
            public val interactive: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean?> by OnOff
            public val pseudoTerminal: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean?> by OnOff
            public val mounts: SkippableCapturingBuilderInterface<CollectingMountOptionsContext.() -> Unit, MountOptions?> by MountOptions
            public val custom: SkippableCapturingBuilderInterface<ListBuildingContext<String>.() -> Unit, List<String>?> by ListBuilder<String>()
        }

        override fun BuildContext.build(): DockerRunCommandLineOptions = ::OptionsContext {
            DockerRunCommandLineOptions(
                ::detached.evalOrDefault(false),
                ::entrypoint.evalOrNull(),
                ::name.evalOrDefault { ::containerName.evalOrNull<String>() }?.let { DockerContainerName(it) },
                ::publish.evalOrDefault(emptyList()),
                ::privileged.evalOrDefault(false),
                ::workingDirectory.evalOrNull(),
                ::autoCleanup.evalOrDefault(true),
                ::interactive.evalOrDefault(true),
                ::pseudoTerminal.evalOrDefault(false),
                ::mounts.evalOrDefault { MountOptions() },
                ::custom.evalOrDefault(emptyList()),
            )
        }
    }
}

public class MountOptions(private val mountOptions: List<MountOption>) : AbstractList<MountOption>() {
    public constructor(vararg mountOptions: MountOption) : this(mountOptions.toList())

    override val size: Int = mountOptions.size
    override fun get(index: Int): MountOption = mountOptions[index]

    public fun mapToHostPath(containerPath: ContainerPath): HostPath {
        val mappedHostPaths = mapNotNull { mountOption ->
            kotlin.runCatching { mountOption.mapToHostPath(containerPath) }.getOrNull()
        }
        require(mappedHostPaths.isNotEmpty()) { "$containerPath is not mapped by any of ${map { it.target }.joinToString(", ")}" }
        return mappedHostPaths.first()
    }

    public fun mapToContainerPath(hostPath: HostPath): ContainerPath {
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

    public companion object : BuilderTemplate<CollectingMountOptionsContext, MountOptions>() {
        @DockerCommandLineDsl
        public class CollectingMountOptionsContext(override val captures: CapturesMap) : CapturingContext(), MountOptionContext<Unit> {
            public val addMount: SkippableCapturingBuilderInterface<MountOptionContext<MountOption>.() -> MountOption, MountOption?> by MountOption
            override fun mount(type: String, source: HostPath, target: ContainerPath) {
                addMount using MountOption(type, source, target)
            }
        }

        override fun BuildContext.build(): MountOptions = ::CollectingMountOptionsContext {
            MountOptions(::addMount.evalAll())
        }
    }
}

public data class MountOption(val type: String = "bind", val source: HostPath, val target: ContainerPath) :
    AbstractList<String>() {
    private val list = listOf("--mount", "type=$type,source=${source.asString()},target=${target.asString()}")

    override val size: Int = list.size
    override fun get(index: Int): String = list[index]

    public fun mapToHostPath(containerPath: ContainerPath): HostPath {
        require(containerPath.isSubPathOf(target)) { "$containerPath is not mapped by $target" }
        val relativePath = containerPath.relativeTo(target)
        return source.resolve(relativePath)
    }

    public fun mapToContainerPath(hostPath: HostPath): ContainerPath {
        require(hostPath.isSubPathOf(source)) { "$hostPath is not mapped by $source" }
        val relativePath = hostPath.relativeTo(source).asString()
        return target.resolve(relativePath)
    }

    public companion object : StatelessBuilder.Returning<MountOptionContext<MountOption>, MountOption>(object : MountOptionContext<MountOption> {
        override fun mount(type: String, source: HostPath, target: ContainerPath): MountOption {
            return MountOption(type, source, target)
        }
    })
}

public interface MountOptionContext<T> {
    public enum class Type { bind, volume, tmpfs }
    public data class Mount(val type: String, val source: HostPath)

    public fun mount(type: String = "bind", source: HostPath, target: ContainerPath): T

    public infix fun String.mountAt(target: String): T = mount(source = asHostPath(), target = target.asContainerPath())
    public infix fun HostPath.mountAt(target: ContainerPath): T = mount(source = this, target = target)
    public infix fun HostPath.mountAt(target: String): T = mount(source = this, target = target.asContainerPath())

    public infix fun String.mountAs(type: String): Mount = Mount(type, asHostPath())
    public infix fun HostPath.mountAs(type: String): Mount = Mount(type, this)
    public infix fun String.mountAs(type: Type): Mount = Mount(type.name, asHostPath())
    public infix fun HostPath.mountAs(type: Type): Mount = Mount(type.name, this)

    public infix fun Mount.at(target: String): T = mount(type = type, source = source, target = target.asContainerPath())
    public infix fun Mount.at(target: ContainerPath): T = mount(type = type, source = source, target = target)
}

public inline class ContainerPath(private val containerPath: Path) {
    private val absolutePath: Path
        get() {
            require(containerPath.isAbsolute) { "$containerPath must be absolute." }
            return containerPath.toAbsolutePath()
        }

    public fun relativeTo(baseContainerPath: ContainerPath): String =
        absolutePath.relativeTo(baseContainerPath.absolutePath).asString()

    public fun isSubPathOf(baseContainerPath: ContainerPath): Boolean =
        absolutePath.isSubPathOf(baseContainerPath.absolutePath)

    public fun resolve(other: ContainerPath): ContainerPath =
        absolutePath.resolve(other.absolutePath).asContainerPath()

    public fun resolve(other: String): ContainerPath =
        absolutePath.resolve(other).asContainerPath()

    public fun mapToHostPath(mountOptions: MountOptions): HostPath /* = java.nio.file.Path */ =
        mountOptions.mapToHostPath(this)

    public fun asString(): String = absolutePath.asString()

    override fun toString(): String = asString()
}

public fun String.asContainerPath(): ContainerPath = ContainerPath(asPath())
public fun Path.asContainerPath(): ContainerPath = ContainerPath(this)

public typealias HostPath = Path

public fun HostPath.mapToContainerPath(mountOptions: MountOptions): ContainerPath =
    mountOptions.mapToContainerPath(this)

public fun String.asHostPath(): HostPath = asPath()
