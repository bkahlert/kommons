package koodies.tracing.rendering

import koodies.test.expectThrows
import koodies.tracing.Span.AttributeKeys
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
            expectThrows<IllegalArgumentException> { ColumnsFormat(columnSpecs = emptyArray()) }
        }
    }

    @Nested
    inner class DefaultColumns {

        @Test
        fun `should have one description labeled columns by default`() {
            expectThat(ColumnsFormat()).isEqualTo(ColumnsFormat(80 to "description", gap = 5, maxColumns = 80))
        }

        @Test
        fun `should use sum of columns and gaps as max columns default`() {
            expectThat(ColumnsFormat(44 to "a", 33 to "b", gap = 5).maxColumns).isEqualTo(44 + 5 + 33)
        }
    }

    @Nested
    inner class SingleColumn {

        @Test
        fun `should return single column on match`() {
            expectThat(ColumnsFormat(80 to "col1", maxColumns = 80).scaled.toList()).containsExactly(
                "col1" to 80
            )
        }

        @Test
        fun `should scale up single column if too narrow`() {
            expectThat(ColumnsFormat(60 to "col1", maxColumns = 80).scaled.toList()).containsExactly(
                "col1" to 80
            )
        }

        @Test
        fun `should scale down single column if too wide`() {
            expectThat(ColumnsFormat(100 to "col1", maxColumns = 80).scaled.toList()).containsExactly(
                "col1" to 80
            )
        }

        @Test
        fun `should extract matching description`() {
            expectThat(ColumnsFormat(60 to AttributeKeys.Description).extract("custom description",
                mapOf("col-1" to "column 1"))).containsExactly(
                "custom description" to 60
            )
        }

        @Test
        fun `should extract matching attribute`() {
            expectThat(ColumnsFormat(60 to "col-1").extract("custom description", mapOf("col-1" to "column 1"))).containsExactly(
                "column 1" to 60
            )
        }

        @Test
        fun `should extract match null of not present`() {
            expectThat(ColumnsFormat(60 to "col-1").extract("custom description", mapOf("col-2" to "column 2"))).containsExactly(
                null to 60
            )
        }
    }

    @Nested
    inner class MultipleColumns {

        @Test
        fun `should return columns on match`() {
            expectThat(ColumnsFormat(55 to "col1", 20 to "col2", maxColumns = 80).scaled.toList()).containsExactly(
                "col1" to 55,
                "col2" to 20,
            )
        }

        @Test
        fun `should apply maxColumns`() {
            expectThat(ColumnsFormat(55 to "col1", 20 to "col2", maxColumns = 20).scaled.toList()).containsExactly(
                "col1" to 11,
                "col2" to 4,
            )
        }

        @Test
        fun `should apply gap`() {
            expectThat(ColumnsFormat(55 to "col1", 20 to "col2", gap = 20, maxColumns = 80).scaled.toList()).containsExactly(
                "col1" to 44,
                "col2" to 16,
            )
        }

        @Test
        fun `should scale up columns if too narrow`() {
            expectThat(ColumnsFormat(45 to "col1", 10 to "col2", maxColumns = 80).scaled.toList()).containsExactly(
                "col1" to 62,
                "col2" to 13,
            )
        }

        @Test
        fun `should scale down columns column if too wide`() {
            expectThat(ColumnsFormat(65 to "col1", 30 to "col2", maxColumns = 80).scaled.toList()).containsExactly(
                "col1" to 52,
                "col2" to 23,
            )
        }

        @Test
        fun `should extract matching columns`() {
            expectThat(ColumnsFormat(45 to "col-1", 10 to AttributeKeys.Description).extract("custom description",
                mapOf("col-1" to "column 1"))).containsExactly(
                "column 1" to 45,
                "custom description" to 10,
            )
        }

        @Test
        fun `should extract match null of not present`() {
            expectThat(ColumnsFormat(45 to "col-1", 10 to "col2").extract("custom description", emptyMap())).containsExactly(
                null to 45,
                null to 10,
            )
        }
    }

    @Nested
    inner class Shrinking {

        @Test
        fun `should shrink`() {
            val config = ColumnsFormat(30 to "left", 35 to "right").shrinkBy(10)
            expectThat(config).isEqualTo(ColumnsFormat(20 to "left", 35 to "right"))
        }

        @Test
        fun `should throw if to narrow`() {
            expectThrows<IllegalArgumentException> { ColumnsFormat(30 to "left", 35 to "right").shrinkBy(30) }
        }
    }
}
