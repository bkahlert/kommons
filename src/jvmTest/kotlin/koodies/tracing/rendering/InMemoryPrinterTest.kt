package koodies.tracing.rendering

import koodies.test.toStringIsEqualTo
import org.junit.jupiter.api.Test
import strikt.api.expectThat

class InMemoryPrinterTest {

    @Test
    fun `should be initially empty`() {
        expectThat(InMemoryPrinter()).toStringIsEqualTo("")
    }

    @Test
    fun `should remove trailing line`() {
        expectThat(InMemoryPrinter().apply {
            invoke("foo")
        }).toStringIsEqualTo("""
            foo
        """.trimIndent())
    }

    @Test
    fun `should append lines`() {
        expectThat(InMemoryPrinter().apply {
            invoke("foo")
            invoke("bar")
        }).toStringIsEqualTo("""
            foo
            bar
        """.trimIndent())
    }

    @Test
    fun `should not collect if disabled`() {
        expectThat(InMemoryPrinter(enabled = false).apply {
            invoke("foo")
            enabled = true
            invoke("bar")
        }).toStringIsEqualTo("""
            bar
        """.trimIndent())
    }
}
