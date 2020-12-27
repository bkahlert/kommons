package koodies.test

import koodies.io.path.randomFile
import koodies.io.path.writeText
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
class HasMatchingLineKtTest {

    @Test
    fun `matches path with one matching line`() = withTempDir {
        val path = randomFile().apply { writeText("abc\nadc\naop\n") }
        expectThat(path).hasMatchingLine("a{}c")
    }

    @Test
    fun `matches path with multiple matching line`() = withTempDir {
        val path = randomFile().apply { writeText("abc\nadc\naop\n") }
        expectThat(path).hasMatchingLine("a{}")
    }

    @Test
    fun `matches path with no matching line`() = withTempDir {
        val path = randomFile().apply { writeText("abc\nadc\naop\n") }
        expectThat(path).not { hasMatchingLine("xyz") }
    }
}
