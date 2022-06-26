package com.bkahlert.kommons.tracing.rendering

import com.bkahlert.kommons.test.test
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TeePrinterTest {

    @Test fun `should delegate calls to printers`() = test {
        val printers = listOf(InMemoryPrinter(), InMemoryPrinter())
        TeePrinter(*printers.toTypedArray()).apply {
            invoke("foo")
            invoke("bar")
        }
        printers.forAll {
            it.toString() shouldBe """
                foo
                bar
            """.trimIndent()
        }
    }

    @Test fun `should accept last printer as lambda`() {
        val printers = listOf(InMemoryPrinter(), InMemoryPrinter())
        TeePrinter(printers.first()) { printers.last().invoke(it) }.apply {
            invoke("foo")
            invoke("bar")
        }
        printers.forAll {
            it.toString() shouldBe """
                foo
                bar
            """.trimIndent()
        }
    }
}
