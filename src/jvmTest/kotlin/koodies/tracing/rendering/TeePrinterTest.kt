package koodies.tracing.rendering

import koodies.test.toStringIsEqualTo
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.all

class TeePrinterTest {

    @Test
    fun `should delegate calls to printers`() {
        val printers = listOf(InMemoryPrinter(), InMemoryPrinter())
        TeePrinter(*printers.toTypedArray()).apply {
            invoke("foo")
            invoke("bar")
        }
        expectThat(printers).all {
            toStringIsEqualTo("""
            foo
            bar
        """.trimIndent())
        }
    }

    @Test
    fun `should accept last printer as lambda`() {
        val printers = listOf(InMemoryPrinter(), InMemoryPrinter())
        TeePrinter(printers.first()) { printers.last().invoke(it) }.apply {
            invoke("foo")
            invoke("bar")
        }
        expectThat(printers).all {
            toStringIsEqualTo("""
            foo
            bar
        """.trimIndent())
        }
    }
}
