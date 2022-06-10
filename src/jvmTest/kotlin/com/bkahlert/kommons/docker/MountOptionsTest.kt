package com.bkahlert.kommons.docker

import com.bkahlert.kommons.builder.Init
import com.bkahlert.kommons.docker.MountOptionContext.Type.tmpfs
import com.bkahlert.kommons.docker.MountOptionContext.Type.volume
import com.bkahlert.kommons.test.SystemIOExclusive
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.testEachOld
import com.bkahlert.kommons.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import strikt.assertions.isNotSameInstanceAs
import strikt.assertions.message
import java.nio.file.Path

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
    inner class Plus {

        private val mountOptions = MountOptions(
            MountOption(
                source = "host-source".asHostPath(),
                target = "/container-target".asContainerPath(),
            )
        )

        private val mountOption = MountOption(
            source = "new-host-source".asHostPath(),
            target = "/new-container-target".asContainerPath(),
        )

        @Test
        fun `should return new instance`() {
            expectThat(mountOptions + mountOption).isNotSameInstanceAs(mountOptions)
        }

        @Test
        fun `should contain existing elements`() {
            expectThat(mountOptions + mountOption).contains(mountOptions)
        }

        @Test
        fun `should contain new element`() {
            expectThat(mountOptions + mountOption).contains(listOf(mountOption))
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
        fun `should throw on relative target`() = testEachOld<Init<MountOptionContext<Unit>>>(
            { "host-source".asHostPath() mountAt "string-target" },
            { "host-source".asHostPath() mountAt "container-target".asContainerPath() },
            { "string-source" mountAs "string-type" at "string-target" },
            { "string-source" mountAs "string-type" at "container-target".asContainerPath() },
        ) { mountOperation ->
            expectThrows<IllegalArgumentException> { MountOptions { mountOperation() } }
        }

        @Isolated
        @Nested
        inner class IncompleteMounts {

            @SystemIOExclusive
            @TestFactory
            fun `should throw on incomplete mounts`() = testEachOld<Init<MountOptionContext<Unit>>>(
                { "string-source" mountAs "string-type" },
                { "host-source".asHostPath() mountAs "string-type" },
                { "string-source" mountAs volume },
                { "host-source".asHostPath() mountAs tmpfs },
            ) { mountOperation ->
                expectThrows<IllegalArgumentException> { MountOptions { mountOperation() } } that {
                    message.isNotNull().contains("complete the configuration")
                }
            }
        }
    }
}
