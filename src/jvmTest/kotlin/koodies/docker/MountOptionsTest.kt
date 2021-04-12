package koodies.docker

import koodies.debug.CapturedOutput
import koodies.docker.MountOptionContext.Type.tmpfs
import koodies.docker.MountOptionContext.Type.volume
import koodies.docker.MountOptions.Companion.CollectingMountOptionsContext
import koodies.test.SystemIoExclusive
import koodies.test.UniqueId
import koodies.test.testEach
import koodies.test.withTempDir
import koodies.time.poll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isTrue
import strikt.assertions.message
import java.nio.file.Path
import kotlin.time.milliseconds
import kotlin.time.seconds

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

    @Nested
    inner class WithBuilder {

        @Test
        fun `should build`() {
            val mountOption = MountOptions {
                "string-source" mountAt "/string-target"
                "host-source".asHostPath() mountAt "/string-target"
                "host-source".asHostPath() mountAt "/container-target".asContainerPath()

                "string-source" mountAs "string-type" at "/string-target"
                "string-source" mountAs "string-type" at "/container-target".asContainerPath()
                "host-source".asHostPath() mountAs "string-type" at "/string-target"
                "host-source".asHostPath() mountAs "string-type" at "/container-target".asContainerPath()

                "string-source" mountAs volume at "/string-target"
                "string-source" mountAs volume at "/container-target".asContainerPath()
                "host-source".asHostPath() mountAs tmpfs at "/string-target"
                "host-source".asHostPath() mountAs tmpfs at "/container-target".asContainerPath()
            }
            expectThat(mountOption).contains(
                MountOption("string-source".asHostPath(), "/string-target".asContainerPath()),
                MountOption("host-source".asHostPath(), "/string-target".asContainerPath()),
                MountOption("host-source".asHostPath(), "/container-target".asContainerPath()),

                MountOption("string-source".asHostPath(), "/string-target".asContainerPath(), "string-type"),
                MountOption("string-source".asHostPath(), "/container-target".asContainerPath(), "string-type"),
                MountOption("host-source".asHostPath(), "/string-target".asContainerPath(), "string-type"),
                MountOption("host-source".asHostPath(), "/container-target".asContainerPath(), "string-type"),

                MountOption("string-source".asHostPath(), "/string-target".asContainerPath(), volume.name),
                MountOption("string-source".asHostPath(), "/container-target".asContainerPath(), volume.name),
                MountOption("host-source".asHostPath(), "/string-target".asContainerPath(), tmpfs.name),
                MountOption("host-source".asHostPath(), "/container-target".asContainerPath(), tmpfs.name),
            )
        }

        @TestFactory
        fun `should throw on relative target`() = listOf<CollectingMountOptionsContext.() -> Unit>(
            { "host-source".asHostPath() mountAt "string-target" },
            { "host-source".asHostPath() mountAt "container-target".asContainerPath() },
            { "string-source" mountAs "string-type" at "string-target" },
            { "string-source" mountAs "string-type" at "container-target".asContainerPath() },
        ).testEach { mountOperation ->
            test {
                expectThrowing { MountOptions { mountOperation() } }.isFailure().isA<IllegalArgumentException>()
            }
        }

        @SystemIoExclusive
        @TestFactory
        fun `should throw on incomplete mounts`(capturedOutput: CapturedOutput) = listOf<CollectingMountOptionsContext.() -> Unit>(
            { "string-source" mountAs "string-type" },
            { "host-source".asHostPath() mountAs "string-type" },
            { "string-source" mountAs volume },
            { "host-source".asHostPath() mountAs tmpfs },
        ).testEach { mountOperation ->
            test {
                MountOptions { mountOperation() }
                expectThat(poll { capturedOutput.contains("missing") && capturedOutput.contains("complete the configuration") }
                    .every(100.milliseconds).forAtMost(5.seconds) { fail("No warning due to missing at call logged.") }).isTrue()
            }
        }
    }
}
