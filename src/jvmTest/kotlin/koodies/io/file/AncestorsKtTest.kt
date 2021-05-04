package koodies.io.file

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import java.nio.file.Path

class AncestorsKtTest {

    private val path = Path.of("a/b/c")

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
