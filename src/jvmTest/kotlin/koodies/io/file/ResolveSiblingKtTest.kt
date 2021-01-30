package koodies.io.file

import koodies.functional.alsoIf
import koodies.io.path.asString
import koodies.test.testEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFailure
import java.nio.file.Path

@Execution(CONCURRENT)
class ResolveSiblingKtTest {

    @Test
    fun `should resolve sibling path`() {
        expectThat(Path.of("/a/b/c").resolveSibling { resolveSibling(fileName.asString() + "-x") }).serializedIsEqualTo("/a/b-x/c")
    }

    @Test
    fun `should resolve with returned multi-segment path`() {
        expectThat(Path.of("/a/b/c.d").resolveSibling { resolveSibling("1/e") }).serializedIsEqualTo("/a/1/e/c.d")
    }

    @TestFactory
    fun `should apply order`() = listOf(
        0 to "/a/b/c-x",
        1 to "/a/b-x/c",
        2 to "/a-x/b/c",
    ).testEach { (order, expected) ->
        expect { Path.of("/a/b/c").resolveSibling(order) { resolveSibling(fileName.asString() + "-x") } }.that { serializedIsEqualTo(expected) }
    }

    @Test
    fun `should throw on more levels requested than present`() {
        expectCatching { Path.of("/a/b/c").resolveSibling(order = 3) { this } }.isFailure().isA<IllegalArgumentException>()
    }

    @Test
    fun `should throw on negative order`() {
        expectCatching { Path.of("/a/b/c").resolveSibling(order = -1) { this } }.isFailure().isA<IllegalArgumentException>()
    }
}

fun <T : Path> Assertion.Builder<T>.isSiblingOf(expected: Path, order: Int = 1) =
    assert("is sibling of order $order") { actual ->
        val actualNames = actual.map { name -> name.asString() }.toList()
        val otherNames = expected.map { name -> name.asString() }.toList()
        val actualIndex = actualNames.size - order - 1
        val otherIndex = otherNames.size - order - 1
        val missing = (actualIndex - otherNames.size + 1).alsoIf({ it > 0 }) {
            fail("$expected is too short. At least $it segments are missing to be able to be sibling.")
        }
        if (missing <= 0) {
            val evaluation = actualNames.zip(otherNames).mapIndexed { index, namePair ->
                val match = if (index == actualIndex || index == otherIndex) true
                else namePair.first == namePair.second
                namePair to match
            }
            val matches = evaluation.takeWhile { (_, match) -> match }.map { (namePair, _) -> namePair.first }
            val misMatch = evaluation.getOrNull(matches.size)?.let { (namePair, _) -> namePair }
            if (misMatch != null) fail("Paths match up to $matches, then mismatch $misMatch")
            else pass()
        }
    }
