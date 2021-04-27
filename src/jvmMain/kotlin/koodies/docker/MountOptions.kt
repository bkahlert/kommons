package koodies.docker

import koodies.Exceptions
import koodies.builder.Builder
import koodies.builder.Init

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

    /**
     * Returns [MountOptions] containing all [mountOptions] of the original
     * [MountOptions] and then the given [mountOption].
     */
    public operator fun plus(mountOption: MountOption): MountOptions =
        MountOptions(mountOptions.plusElement(mountOption))

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

    public companion object : Builder<Init<MountOptionContext<Unit>>, MountOptions> {

        override fun invoke(init: Init<MountOptionContext<Unit>>): MountOptions {
            val mounts: MutableList<MountOption> = mutableListOf()
            val buildErrors: MutableList<String> = mutableListOf()
            object : MountOptionContext<Unit>(buildErrors) {
                override fun mount(source: HostPath, target: ContainerPath, type: String): Unit {
                    mounts.add(MountOption(source, target, type))
                }
            }.init()
            if (buildErrors.isNotEmpty()) throw Exceptions.IAE(buildErrors)
            return MountOptions(mounts)
        }
    }
}
