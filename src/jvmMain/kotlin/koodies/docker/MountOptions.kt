package koodies.docker

import koodies.builder.BuilderTemplate
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.docker.MountOptions.Companion.CollectingMountOptionsContext

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
            override fun mount(source: HostPath, target: ContainerPath, type: String) {
                addMount using MountOption(source, target, type)
            }
        }

        override fun BuildContext.build(): MountOptions = ::CollectingMountOptionsContext {
            MountOptions(::addMount.evalAll())
        }
    }
}
