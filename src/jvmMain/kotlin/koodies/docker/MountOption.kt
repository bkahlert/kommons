package koodies.docker

import koodies.builder.StatelessBuilder.Returning
import koodies.jvm.thread
import koodies.io.path.asPath
import koodies.io.path.asString
import koodies.io.path.isSubPathOf
import koodies.text.Semantics.formattedAs
import koodies.text.styling.Borders.Rounded
import koodies.text.styling.wrapWithBorder
import koodies.time.sleep
import java.nio.file.Path
import kotlin.io.path.relativeTo
import kotlin.time.milliseconds

public data class MountOption(val source: HostPath, val target: ContainerPath, val type: String = "bind") :
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

    public companion object : Returning<MountOptionContext<MountOption>, MountOption>(object : MountOptionContext<MountOption> {
        override fun mount(source: HostPath, target: ContainerPath, type: String): MountOption {
            return MountOption(source, target, type)
        }
    })
}

public interface MountOptionContext<T> {

    public enum class Type { bind, volume, tmpfs }
    public data class Mount<T>(
        val type: String,
        val source: HostPath,
        private val completeCallback: Mount<T>.(ContainerPath) -> T,
    ) {
        private var incomplete: Boolean = true

        init {
            thread {
                50.milliseconds.sleep()
                if (incomplete) listOf("Mount ${source.formattedAs.input} of type ${type.formattedAs.input} is missing its ${"target".formattedAs.input}.",
                    "Use the ${"at".formattedAs.warning} method to complete the configuration.").wrapWithBorder(Rounded,
                    formatter = { it.formattedAs.warning }).also { println(it) }
            }
        }

        public infix fun at(target: String): T = at(target.asContainerPath())
        public infix fun at(target: ContainerPath): T = run {
            incomplete = false
            completeCallback(target)
        }
    }

    public fun mount(source: HostPath, target: ContainerPath, type: String = "bind"): T

    public infix fun String.mountAt(target: String): T = mount(asHostPath(), target.asContainerPath())
    public infix fun HostPath.mountAt(target: ContainerPath): T = mount(this, target)
    public infix fun HostPath.mountAt(target: String): T = mount(this, target.asContainerPath())

    public infix fun String.mountAs(type: String): Mount<T> = Mount(type, asHostPath()) { mount(source, it, type) }
    public infix fun HostPath.mountAs(type: String): Mount<T> = Mount(type, this) { mount(source, it, type) }
    public infix fun String.mountAs(type: Type): Mount<T> = Mount(type.name, asHostPath()) { mount(source, it, type.name) }
    public infix fun HostPath.mountAs(type: Type): Mount<T> = Mount(type.name, this) { mount(source, it, type.name) }
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
