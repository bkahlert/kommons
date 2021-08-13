package koodies.io.file

import koodies.test.testEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path

class RequireAncestorKtTest {

    private val path = Path.of("a/b/c")

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
