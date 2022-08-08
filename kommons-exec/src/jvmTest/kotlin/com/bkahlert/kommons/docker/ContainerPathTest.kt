package com.bkahlert.kommons.docker

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class ContainerPathTest {

    @Nested
    inner class RelativeTo {
        @Test
        fun `should throw on relative path`() {
            expectCatching { "dir/file".asContainerPath().relativeTo("/some/where".asContainerPath()) }
                .isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should relativize same-root absolute path`() {
            expectThat("/some/where/dir/file".asContainerPath().relativeTo("/some/where".asContainerPath()))
                .isEqualTo("dir/file")
        }

        @Test
        fun `should relativize different-root absolute path`() {
            expectThat("/different/root/dir/file".asContainerPath().relativeTo("/some/where".asContainerPath()))
                .isEqualTo("../../different/root/dir/file")
        }
    }

    @Nested
    inner class IsSubPathOf {
        @Test
        fun `should return true if sub-path`() {
            expectThat("/dir/file".asContainerPath().isSubPathOf("/dir".asContainerPath())).isTrue()
        }

        @Test
        fun `should return false if not sub-path`() {
            expectThat("/dir/file".asContainerPath().isSubPathOf("/other".asContainerPath())).isFalse()
        }
    }

    @Nested
    inner class Resolve {
        @Test
        fun `should resolve relative path`() {
            expectThat("/some/where".asContainerPath().resolve("dir/file"))
                .isEqualTo("/some/where/dir/file".asContainerPath())
        }

        @Test
        fun `should resolve absolute path`() {
            expectThat("/some/where".asContainerPath().resolve("/dir/file".asContainerPath()))
                .isEqualTo("/dir/file".asContainerPath())
        }
    }


    @Nested
    inner class AsString {
        @Test
        fun `should return path as string`() {
            expectThat("/some/where".asContainerPath().pathString).isEqualTo("/some/where")
        }
    }
}
