package koodies.io.file

import koodies.test.test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path

@Execution(CONCURRENT)
class QuotedKtTest {

    @TestFactory
    fun `should quote path`() = listOf(
        "filename" to "\"filename\"",
        "filename.test" to "\"filename.test\"",
        "my/path/filename" to "\"my/path/filename\"",
        "my/path/filename.test" to "\"my/path/filename.test\"",
        "/my/path/filename" to "\"/my/path/filename\"",
        "/my/path/filename.test" to "\"/my/path/filename.test\"",
    ).test("{} -> {}") { (path, expected) ->
        expectThat(Path.of(path)).quoted.isEqualTo(expected)
    }
}

val <T : Path> Assertion.Builder<T>.quoted
    get() = get("wrapped in quotes %s") { quoted }
