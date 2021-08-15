package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.test.elements
import com.bkahlert.kommons.test.expectThrows
import com.bkahlert.kommons.test.hasEqualElements
import com.bkahlert.kommons.test.isEmpty
import com.bkahlert.kommons.test.testEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import java.nio.file.Path

class AncestorKtTest {

    private val path = Path.of("a/b/c")

    @Nested
    inner class Ancestor {

        @Test
        fun `should return self for order 0`() {
            expectThat(path.ancestor(0)).isEqualTo(path)
        }

        @Test
        fun `should return parent for order 1`() {
            expectThat(path.ancestor(1)).isEqualTo(path.parent)
        }

        @TestFactory
        fun `should return ancestor's parent for order n+1`() =
            (0 until path.nameCount).testEach { n ->
                expecting { path.ancestor(n + 1) } that { isEqualTo((path.ancestor(n) ?: fail("missing parent")).parent) }
            }

        @Test
        fun `should return null of non-existent ancestor`() {
            expectThat(path.ancestor(path.nameCount)).isEqualTo(null)
        }
    }

    @Nested
    inner class Ancestors {

        @Test
        fun `should return ancestors starting at parent`() {
            expectThat(path.ancestors()).isEqualTo(path.ancestors(1))
        }

        @Test
        fun `should return ancestors starting at specified order`() {
            expectThat(path.ancestors(1)).containsExactly(Path.of("a/b"), Path.of("a"))
        }

        @Test
        fun `should return empty sequence on missing ancestors`() {
            expectThat(path.ancestors(path.nameCount)).isEmpty()
        }
    }

    @Nested
    inner class AncestorSequence {

        @Test
        fun `should return ancestors starting at parent`() {
            expectThat(path.ancestorSequence()).hasEqualElements(path.ancestorSequence(1))
        }

        @Test
        fun `should return ancestors starting at specified order`() {
            expectThat(path.ancestorSequence(1)).elements.containsExactly(Path.of("a/b"), Path.of("a"))
        }

        @Test
        fun `should return empty sequence on missing ancestors`() {
            expectThat(path.ancestorSequence(path.nameCount)).isEmpty()
        }
    }

    @Nested
    inner class RequireAncestorKtTest {

        @Test
        fun `should return self for order 0`() {
            expectThat(path.requireAncestor(0)).isEqualTo(path)
        }

        @Test
        fun `should return parent for order 1`() {
            expectThat(path.requireAncestor(1)).isEqualTo(path.parent)
        }

        @TestFactory
        fun `should return ancestor's parent for order n+1`() =
            (0 until path.nameCount - 1).testEach { n ->
                expecting { path.requireAncestor(n + 1) } that { isEqualTo((path.ancestor(n) ?: fail("missing parent")).parent) }
            }

        @Test
        fun `should return throw of non-existent ancestor`() {
            expectCatching { path.requireAncestor(path.nameCount) }
        }
    }

    @Nested
    inner class SubPath {

        @Test
        fun `should return sub path`() {
            expectThat(Path.of("/a/b/c").subpath(2)).isEqualTo(Path.of("b/c"))
        }

        @Test
        fun `should throw on invalid order`() {
            expectThrows<IllegalArgumentException> { Path.of("/a/b/c/d").subpath(0) }
        }
    }
}
