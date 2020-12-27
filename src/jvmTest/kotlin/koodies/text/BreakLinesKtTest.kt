package koodies.text

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class BreakLinesKtTest {
    @Test
    fun `should break do nothing on single short line`() {
        expectThat("short line".breakLines(15)).isEqualTo("short line")
    }

    @Test
    fun `should break do nothing on multiple short lines`() {
        expectThat("short line\nshort line".breakLines(15)).isEqualTo("short line\nshort line")
    }

    @Test
    fun `should break long line`() {
        expectThat("very very long line".breakLines(15)).isEqualTo("very very long \nline")
    }

    @Test
    fun `should break multiple long line`() {
        expectThat("very very long line\nvery very long line".breakLines(15)).isEqualTo("very very long \nline\nvery very long \nline")
    }

    @Test
    fun `should only break long line if mixed with short line`() {
        expectThat("short line\nvery very long line\nshort line".breakLines(15)).isEqualTo("short line\nvery very long \nline\nshort line")
    }

    @Test
    fun `should break long line in as many lines as needed`() {
        expectThat("very very long line".breakLines(5)).isEqualTo("very \nvery \nlong \nline")
    }
}
