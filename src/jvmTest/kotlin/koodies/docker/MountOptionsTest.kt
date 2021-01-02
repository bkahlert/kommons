package koodies.docker

import koodies.test.UniqueId
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.message
import java.nio.file.Path

@Execution(CONCURRENT)
class MountOptionsTest {

    private val Path.mountOptions
        get() = MountOptions(
            MountOption(source = resolve("host/root1"), target = "/mount/host1".asContainerPath()),
            MountOption(source = resolve("host/root2"), target = "/mount/host2".asContainerPath()),
            MountOption(source = resolve("host/root3"), target = "/mount/host1/root3".asContainerPath()),
            MountOption(source = resolve("host/root1"), target = "/mount/host4".asContainerPath()),
        )

    @Nested
    inner class MapToHostPath {

        @Test
        fun `should map on single match`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(mountOptions).get { mapToHostPath("/mount/host2".asContainerPath()) }
                .isEqualTo(resolve("host/root2"))
        }

        @Test
        fun `should map first on multiple matches`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(mountOptions).get { mapToHostPath("/mount/host1/root3".asContainerPath()) }
                .isEqualTo(resolve("host/root1/root3"))
        }

        @Test
        fun `should throw on no match`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching { mountOptions.mapToHostPath("/different/root".asContainerPath()) }
                .isFailure().isA<IllegalArgumentException>()
                .message.isEqualTo("/different/root is not mapped by any of /mount/host1, /mount/host2, /mount/host1/root3, /mount/host4")
        }
    }


    @Nested
    inner class MapToContainerPath {

        @Test
        fun `should map on single match`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(mountOptions).get { mapToContainerPath(resolve("host/root2/dir/file")) }
                .isEqualTo("/mount/host2/dir/file".asContainerPath())
        }

        @Test
        fun `should map first on multiple matches`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(mountOptions).get { mapToContainerPath(resolve("host/root1/dir/file")) }
                .isEqualTo("/mount/host1/dir/file".asContainerPath())
        }

        @Test
        fun `should throw on no match`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching { mountOptions.mapToContainerPath("/different/root".asHostPath()) }
                .isFailure().isA<IllegalArgumentException>()
                .message.isEqualTo("/different/root is not mapped by any of $this/host/root1, $this/host/root2, $this/host/root3, $this/host/root1")
        }
    }
}
