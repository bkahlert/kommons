package koodies.tracing.rendering

import koodies.test.expectThrows
import koodies.tracing.Span
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

class ColumnsFormatTest {

    @Nested
    inner class NoColumns {

        @Test
        fun `should throw on missing columns`() {
            expectThrows<IllegalArgumentException> { ColumnsLayout(columns = emptyArray()) }
        }
    }

    @Nested
    inner class DefaultColumns {

        @Test
        fun `should have one description labeled columns by default`() {
            expectThat(ColumnsLayout()).isEqualTo(ColumnsLayout("description" to 80, gap = 5, maxColumns = 80))
        }

        @Test
        fun `should use sum of columns and gaps as max columns default`() {
            expectThat(ColumnsLayout("a" to 44, "b" to 33, gap = 5).totalWidth).isEqualTo(82)
        }
    }

    @Nested
    inner class SingleColumn {

        @Test
        fun `should return single column on match`() {
            expectThat(ColumnsLayout("col1" to 80, maxColumns = 80).scaled.toList()).containsExactly(
                "col1" to 80
            )
        }

        @Test
        fun `should scale up single column if too narrow`() {
            expectThat(ColumnsLayout("col1" to 60, maxColumns = 80).scaled.toList()).containsExactly(
                "col1" to 80
            )
        }

        @Test
        fun `should scale down single column if too wide`() {
            expectThat(ColumnsLayout("col1" to 100, maxColumns = 80).scaled.toList()).containsExactly(
                "col1" to 80
            )
        }

        @Test
        fun `should extract matching description`() {
            expectThat(ColumnsLayout(Span.Description to 60).extract(
                mapOf(Span.Description to "custom description", "col-1" to "column 1"))).containsExactly(
                "custom description" to 60
            )
        }

        @Test
        fun `should extract matching attribute`() {
            expectThat(ColumnsLayout("col-1" to 60).extract(mapOf(Span.Description to "custom description", "col-1" to "column 1"))).containsExactly(
                "column 1" to 60
            )
        }

        @Test
        fun `should extract match null of not present`() {
            expectThat(ColumnsLayout("col-1" to 60).extract(mapOf(Span.Description to "custom description", "col-2" to "column 2"))).containsExactly(
                null to 60
            )
        }
    }

    @Nested
    inner class MultipleColumns {

        @Test
        fun `should return columns on match`() {
            expectThat(ColumnsLayout("col1" to 55, "col2" to 20, maxColumns = 80).scaled.toList()).containsExactly(
                "col1" to 55,
                "col2" to 20,
            )
        }

        @Test
        fun `should apply maxColumns`() {
            expectThat(ColumnsLayout("col1" to 55, "col2" to 20, maxColumns = 20).scaled.toList()).containsExactly(
                "col1" to 11,
                "col2" to 4,
            )
        }

        @Test
        fun `should apply gap`() {
            expectThat(ColumnsLayout("col1" to 55, "col2" to 20, gap = 20, maxColumns = 80).scaled.toList()).containsExactly(
                "col1" to 44,
                "col2" to 16,
            )
        }

        @Test
        fun `should scale up columns if too narrow`() {
            expectThat(ColumnsLayout("col1" to 45, "col2" to 10, maxColumns = 80).scaled.toList()).containsExactly(
                "col1" to 62,
                "col2" to 13,
            )
        }

        @Test
        fun `should scale down columns column if too wide`() {
            expectThat(ColumnsLayout("col1" to 65, "col2" to 30, maxColumns = 80).scaled.toList()).containsExactly(
                "col1" to 52,
                "col2" to 23,
            )
        }

        @Test
        fun `should extract matching columns`() {
            expectThat(ColumnsLayout("col-1" to 45, Span.Description to 10).extract(
                mapOf(Span.Description to "custom description", "col-1" to "column 1"))).containsExactly(
                "column 1" to 45,
                "custom description" to 10,
            )
        }

        @Test
        fun `should extract match null of not present`() {
            expectThat(ColumnsLayout("col-1" to 45, "col2" to 10).extract(mapOf(Span.Description to "custom description"))).containsExactly(
                null to 45,
                null to 10,
            )
        }
    }

    @Nested
    inner class Shrinking {

        @Test
        fun `should shrink`() {
            val config = ColumnsLayout("left" to 30, "right" to 35).shrinkBy(10)
            expectThat(config).isEqualTo(ColumnsLayout("left" to 20, "right" to 35))
        }

        @Test
        fun `should throw if to narrow`() {
            expectThrows<IllegalArgumentException> { ColumnsLayout("left" to 30, "right" to 35).shrinkBy(30) }
        }
    }
}
