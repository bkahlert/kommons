package com.bkahlert.kommons.docker

import com.bkahlert.kommons.isSubPathOf
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.text.styling.Borders.Rounded
import com.bkahlert.kommons.text.styling.wrapWithBorder
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

public data class MountOption(val source: HostPath, val target: ContainerPath, val type: String = "bind") :
    AbstractList<String>() {
    private val list = listOf("--mount", "type=$type,source=${source.pathString},target=${target.pathString}")

    override val size: Int = list.size
    override fun get(index: Int): String = list[index]

    public fun mapToHostPath(containerPath: ContainerPath): HostPath {
        require(containerPath.isSubPathOf(target)) { "$containerPath is not mapped by $target" }
        val relativePath = containerPath.relativeTo(target)
        return source.resolve(relativePath)
    }

    public fun mapToContainerPath(hostPath: HostPath): ContainerPath {
        require(hostPath.isSubPathOf(source)) { "$hostPath is not mapped by $source" }
        val relativePath = hostPath.relativeTo(source).pathString
        return target.resolve(relativePath)
    }
}


public abstract class MountOptionContext<T>(
    protected val buildErrors: MutableList<String>,
) {

    public enum class Type { bind, volume, tmpfs }
    public inner class Mount<T>(
        public val type: String,
        public val source: HostPath,
        private val completeCallback: Mount<T>.(ContainerPath) -> T,
    ) {
        private val buildError: String = listOf(
            "Mount ${source.formattedAs.input} of type ${type.formattedAs.input} is missing its ${"target".formattedAs.input}.",
            "Use the ${"at".formattedAs.warning} method to complete the configuration.",
        ).wrapWithBorder(Rounded, formatter = { it.formattedAs.warning })

        init {
            buildErrors.add(buildError)
        }

        public infix fun at(target: String): T = at(target.asContainerPath())
        public infix fun at(target: ContainerPath): T = run {
            buildErrors.remove(buildError)
            completeCallback(target)
        }
    }

    protected abstract fun mount(source: HostPath, target: ContainerPath, type: String = "bind"): T

    public infix fun String.mountAt(target: String): T = mount(asHostPath(), target.asContainerPath())
    public infix fun HostPath.mountAt(target: ContainerPath): T = mount(this, target)
    public infix fun HostPath.mountAt(target: String): T = mount(this, target.asContainerPath())

    public infix fun String.mountAs(type: String): Mount<T> = Mount(type, asHostPath()) { mount(source, it, type) }
    public infix fun HostPath.mountAs(type: String): Mount<T> = Mount(type, this) { mount(source, it, type) }
    public infix fun String.mountAs(type: Type): Mount<T> = Mount(type.name, asHostPath()) { mount(source, it, type.name) }
    public infix fun HostPath.mountAs(type: Type): Mount<T> = Mount(type.name, this) { mount(source, it, type.name) }
}

@JvmInline
public value class ContainerPath(private val containerPath: Path) {
    public constructor(containerPath: String) : this(Paths.get(containerPath))

    init {
        require(containerPath.isAbsolute) { "$containerPath must be absolute." }
    }

    public fun relativeTo(baseContainerPath: ContainerPath): String =
        containerPath.relativeTo(baseContainerPath.containerPath).pathString

    public fun isSubPathOf(baseContainerPath: ContainerPath): Boolean =
        containerPath.isSubPathOf(baseContainerPath.containerPath)

    public fun resolve(other: ContainerPath): ContainerPath =
        containerPath.resolve(other.containerPath).asContainerPath()

    public fun resolve(other: String): ContainerPath =
        containerPath.resolve(other).asContainerPath()

    public fun mapToHostPath(mountOptions: MountOptions): HostPath /* = java.nio.file.Path */ =
        mountOptions.mapToHostPath(this)

    public val pathString: String get() = containerPath.pathString

    override fun toString(): String = pathString
}

public fun String.asContainerPath(): ContainerPath = ContainerPath(this)
public fun Path.asContainerPath(): ContainerPath = ContainerPath(this)

public typealias HostPath = Path

public fun HostPath.mapToContainerPath(mountOptions: MountOptions): ContainerPath =
    mountOptions.mapToContainerPath(this)

public fun String.asHostPath(): HostPath = Paths.get(this)
