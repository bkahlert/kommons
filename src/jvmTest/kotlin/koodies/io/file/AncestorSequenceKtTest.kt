package koodies.io.file

import koodies.test.elements
import koodies.test.hasEqualElements
import koodies.test.isEmpty
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.containsExactly
import java.nio.file.Path


class AncestorSequenceKtTest {

    private val path = Path.of("a/b/c")

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
