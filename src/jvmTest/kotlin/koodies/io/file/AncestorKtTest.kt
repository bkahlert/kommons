package koodies.io.file

import koodies.test.testEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path


class AncestorKtTest {

    private val path = Path.of("a/b/c")

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
