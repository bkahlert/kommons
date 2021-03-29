package koodies.docker

import koodies.builder.StatelessBuilder.Returning
import koodies.io.path.asPath
import koodies.io.path.asString
import koodies.io.path.isSubPathOf
import java.nio.file.Path
import kotlin.io.path.relativeTo

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

    public companion object : Returning<MountOptionContext<MountOption>, MountOption>(object : MountOptionContext<MountOption> {
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
