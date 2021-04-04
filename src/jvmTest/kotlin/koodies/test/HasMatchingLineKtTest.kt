package koodies.test

import koodies.io.path.randomFile
import koodies.io.path.writeText
import koodies.text.LineSeparators.LF
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
class HasMatchingLineKtTest {

    @Test
    fun `matches path with one matching line`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val path = randomFile().writeText("abc\nadc\naop$LF")
        expectThat(path).hasMatchingLine("a{}c")
    }

    @Test
    fun `matches path with multiple matching line`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val path = randomFile().writeText("abc\nadc\naop$LF")
        expectThat(path).hasMatchingLine("a{}")
    }

    @Test
    fun `matches path with no matching line`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val path = randomFile().writeText("abc\nadc\naop$LF")
        expectThat(path).not { hasMatchingLine("xyz") }
    }
}
