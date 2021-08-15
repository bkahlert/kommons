package com.bkahlert.kommons.docker

import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.message
import java.nio.file.Path

class MountOptionTest {

    private val Path.mountOption get() = MountOption(source = resolve("host/container"), target = "/mount/host".asContainerPath())

    @Nested
    inner class MapToHostPath {

        @Test
        fun `should map root`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(mountOption).get { mapToHostPath("/mount/host".asContainerPath()) }
                .isEqualTo(resolve("host/container"))
        }

        @Test
        fun `should map sub path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(mountOption).get { mapToHostPath("/mount/host/dir/file".asContainerPath()) }
                .isEqualTo(resolve("host/container/dir/file"))
        }

        @Test
        fun `should throw on different root`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching { mountOption.mapToHostPath("/different/root".asContainerPath()) }
                .isFailure().isA<IllegalArgumentException>()
                .message.isEqualTo("/different/root is not mapped by /mount/host")
        }
    }


    @Nested
    inner class MapToContainerPath {

        @Test
        fun `should map root`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(mountOption).get { mapToContainerPath(resolve("host/container")) }
                .isEqualTo("/mount/host".asContainerPath())
        }

        @Test
        fun `should map sub path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(mountOption).get { mapToContainerPath(resolve("host/container/dir/file")) }
                .isEqualTo("/mount/host/dir/file".asContainerPath())
        }

        @Test
        fun `should throw on different root`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching { mountOption.mapToContainerPath("/different/root".asHostPath()) }
                .isFailure().isA<IllegalArgumentException>()
                .message.isEqualTo("/different/root is not mapped by $this/host/container")
        }
    }
}
