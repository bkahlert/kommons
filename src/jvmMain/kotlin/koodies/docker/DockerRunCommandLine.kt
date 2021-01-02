package koodies.docker

import koodies.builder.ListBuilder.Companion.buildList
import koodies.builder.ListBuilderInit
import koodies.builder.MapBuilderInit
import koodies.builder.build
import koodies.builder.buildListTo
import koodies.builder.buildMap
import koodies.concurrent.process.CommandLine
import koodies.docker.DockerRunCommandLine.Options
import koodies.io.path.Locations
import koodies.io.path.asPath
import koodies.io.path.asString
import koodies.io.path.isSubPathOf
import java.nio.file.Path
import kotlin.io.path.relativeTo
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

data class DockerRunCommandLine(
    override val workingDirectory: Path,
    val dockerRedirects: List<String> = emptyList(),
    val options: Options = Options(),
    val dockerImage: DockerImage,
    val dockerCommand: String? = null,
    val dockerArguments: List<String> = emptyList(),
) : CommandLine(dockerRedirects, options.env, workingDirectory, "docker", mutableListOf("run").apply {
    addAll(options)
    add(dockerImage.formatted)
    dockerCommand?.also { add(it) }
    addAll(dockerArguments)
}) {
    data class Options(
        val env: Map<String, String> = emptyMap(),
        val entryPoint: String? = null,
        val name: DockerContainerName? = null,
        val privileged: Boolean = false,
        val autoCleanup: Boolean = true,
        val interactive: Boolean = true,
        val pseudoTerminal: Boolean = false,
        val mounts: MountOptions = MountOptions(),
    ) : List<String> by (buildList {
        env.forEach {
            +"--env"
            +"${it.key}=${it.value}"
        }
        entryPoint?.also { +"--entrypoint" + entryPoint }
        name?.also { +"--name" + name.sanitized }
        privileged.takeIf { it }?.also { +"--privileged" }
        autoCleanup.takeIf { it }?.also { +"--rm" }
        interactive.takeIf { it }?.also { +"-i" }
        pseudoTerminal.takeIf { it }?.also { +"-t" }
        mounts.forEach { +it }
    })

    override fun prepare(expectedExitValue: Int): DockerProcess =
        DockerProcess.from(this, expectedExitValue)

    override fun execute(expectedExitValue: Int): DockerProcess =
        prepare(expectedExitValue).also { it.start() }

    override fun toString(): String = super.toString()
}


@DockerCommandDsl
class DockerRunCommandLineBuilder(
    private var workingDirectory: Path = Locations.Temp,
    private val redirects: MutableList<String> = mutableListOf(),
    private var dockerOptions: Options = Options(),
    private var dockerCommand: String? = null,
    private val dockerArguments: MutableList<String> = mutableListOf(),
) {

    class ImageProvidedBuilder(private val image: DockerImage) {
        infix fun run(init: DockerRunCommandLineBuilder.() -> Unit): DockerRunCommandLine = build(image, init)
    }

    companion object {
        fun build(init: DockerImageBuilder.() -> Any): ImageProvidedBuilder =
            ImageProvidedBuilder(DockerImageBuilder.build(init))

        fun DockerImage.buildRunCommand(init: DockerRunCommandLineBuilder.() -> Unit): DockerRunCommandLine = build(this, init)

        fun build(dockerImage: DockerImage, init: DockerRunCommandLineBuilder.() -> Unit): DockerRunCommandLine =
            DockerRunCommandLineBuilder().apply(init).run {
                DockerRunCommandLine(
                    workingDirectory = workingDirectory,
                    dockerRedirects = redirects,
                    options = dockerOptions,
                    dockerImage = dockerImage,
                    dockerCommand = dockerCommand,
                    dockerArguments = dockerArguments,
                )
            }
    }

    fun workingDirectory(workingDirectory: Path) = workingDirectory.also { this.workingDirectory = it }
    fun redirects(init: ListBuilderInit<String>) = init.buildListTo(redirects)
    fun options(init: OptionsBuilder.() -> Unit) = OptionsBuilder.build(init).run { dockerOptions = this }
    fun command(init: () -> String?) = init.build()?.run { dockerCommand = this }
    fun arguments(init: ListBuilderInit<String>) = init.buildListTo(dockerArguments)
}

class OptionBuilder : ReadOnlyProperty<OptionsBuilder, OptionBuilder> {
    override fun getValue(thisRef: OptionsBuilder, property: KProperty<*>): OptionBuilder {
        return this
    }

    operator fun invoke(init: () -> String) {

    }
}

@DockerCommandDsl
abstract class OptionsBuilder {
    companion object {
        inline fun build(init: OptionsBuilder.() -> Unit): Options {
            var options = Options()
            object : OptionsBuilder() {
                override var options: Options
                    get() = options
                    set(value) = value.run { options = this }
            }.apply(init)
            return options
        }
    }

    protected abstract var options: Options

    val sample by OptionBuilder()

    fun env(init: MapBuilderInit<String, String>) = init.buildMap().also { options.copy(env = options.env + it).run { options = this } }
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
    List<String> by listOf("--mount", "type=$type,source=${source.asString()},target=${target.asString()}") {
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
}

open class ContainerPath(private val containerPath: Path) {
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
