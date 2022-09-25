package com.bkahlert.kommons_deprecated.tracing.rendering

import com.bkahlert.kommons_deprecated.test.expectThrows
import com.bkahlert.kommons_deprecated.test.hasElements
import com.bkahlert.kommons_deprecated.tracing.Key
import com.bkahlert.kommons_deprecated.tracing.rendering.ColumnsLayout.Companion.columns
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

class ColumnsLayoutTest {

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
            expectThat(ColumnsLayout()).isEqualTo(ColumnsLayout(RenderingAttributes.DESCRIPTION columns 80, gap = 5, maxColumns = 80))
        }

        @Test
        fun `should use sum of columns and gaps as max columns default`() {
            expectThat(ColumnsLayout(Key.stringKey("a") columns 44, Key.stringKey("b") columns 33, gap = 5).totalWidth).isEqualTo(82)
        }
    }

    @Nested
    inner class SingleColumn {

        @Test
        fun `should return single column on match`() {
            expectThat(ColumnsLayout(Key.stringKey("col1") columns 80, maxColumns = 80).scaled.toList()).containsExactly(
                Key.stringKey("col1") to 80
            )
        }

        @Test
        fun `should scale up single column if too narrow`() {
            expectThat(ColumnsLayout(Key.stringKey("col1") columns 60, maxColumns = 80).scaled.toList()).containsExactly(
                Key.stringKey("col1") to 80
            )
        }

        @Test
        fun `should scale down single column if too wide`() {
            expectThat(ColumnsLayout(Key.stringKey("col1") columns 100, maxColumns = 80).scaled.toList()).containsExactly(
                Key.stringKey("col1") to 80
            )
        }

        @Test
        fun `should extract matching description`() {
            expectThat(
                ColumnsLayout(RenderingAttributes.DESCRIPTION columns 60).extract(
                    RenderableAttributes.of(RenderingAttributes.DESCRIPTION to "custom description", Key.stringKey("col1") to "column 1")
                )
            )
                .hasElements({ get { first.toString() to second }.isEqualTo("custom description" to 60) })
        }

        @Test
        fun `should extract matching attribute`() {
            expectThat(
                ColumnsLayout(Key.stringKey("col1") columns 60)
                    .extract(RenderableAttributes.of(RenderingAttributes.DESCRIPTION to "custom description", Key.stringKey("col1") to "column 1"))
            )
                .hasElements({ get { first.toString() to second }.isEqualTo("column 1" to 60) })
        }

        @Test
        fun `should extract match null of not present`() {
            expectThat(
                ColumnsLayout(Key.stringKey("col1") columns 60)
                    .extract(RenderableAttributes.of(RenderingAttributes.DESCRIPTION to "custom description", Key.stringKey("col2") to "column 2"))
            )
                .containsExactly(null to 60)
        }
    }

    @Nested
    inner class MultipleColumns {

        @Test
        fun `should return columns on match`() {
            expectThat(ColumnsLayout(Key.stringKey("col1") columns 55, Key.stringKey("col2") columns 20, maxColumns = 80).scaled.toList()).containsExactly(
                Key.stringKey("col1") to 55,
                Key.stringKey("col2") to 20,
            )
        }

        @Test
        fun `should apply maxColumns`() {
            expectThat(ColumnsLayout(Key.stringKey("col1") columns 55, Key.stringKey("col2") columns 20, maxColumns = 20).scaled.toList()).containsExactly(
                Key.stringKey("col1") to 11,
                Key.stringKey("col2") to 4,
            )
        }

        @Test
        fun `should apply gap`() {
            expectThat(
                ColumnsLayout(
                    Key.stringKey("col1") columns 55,
                    Key.stringKey("col2") columns 20,
                    gap = 20,
                    maxColumns = 80
                ).scaled.toList()
            ).containsExactly(
                Key.stringKey("col1") to 44,
                Key.stringKey("col2") to 16,
            )
        }

        @Test
        fun `should scale up columns if too narrow`() {
            expectThat(ColumnsLayout(Key.stringKey("col1") columns 45, Key.stringKey("col2") columns 10, maxColumns = 80).scaled.toList()).containsExactly(
                Key.stringKey("col1") to 62,
                Key.stringKey("col2") to 13,
            )
        }

        @Test
        fun `should scale down columns column if too wide`() {
            expectThat(ColumnsLayout(Key.stringKey("col1") columns 65, Key.stringKey("col2") columns 30, maxColumns = 80).scaled.toList()).containsExactly(
                Key.stringKey("col1") to 52,
                Key.stringKey("col2") to 23,
            )
        }

        @Test
        fun `should extract matching columns`() {
            val attributes = RenderableAttributes.of(RenderingAttributes.DESCRIPTION to "custom description", Key.stringKey("col1") to "column 1")
            expectThat(ColumnsLayout(Key.stringKey("col1") columns 45, RenderingAttributes.DESCRIPTION columns 10).extract(attributes))
                .hasElements(
                    { get { first.toString() to second }.isEqualTo("column 1" to 45) },
                    { get { first.toString() to second }.isEqualTo("custom description" to 10) },
                )
        }

        @Test
        fun `should extract match null of not present`() {
            val attributes = RenderableAttributes.of(RenderingAttributes.DESCRIPTION to "custom description")
            expectThat(ColumnsLayout(Key.stringKey("col1") columns 45, Key.stringKey("col2") columns 10).extract(attributes)).containsExactly(
                null to 45,
                null to 10,
            )
        }
    }

    @Nested
    inner class Shrinking {

        @Test
        fun `should shrink`() {
            val config = ColumnsLayout(Key.stringKey("left") columns 30, Key.stringKey("right") columns 35).shrinkBy(10)
            expectThat(config).isEqualTo(ColumnsLayout(Key.stringKey("left") columns 20, Key.stringKey("right") columns 35))
        }

        @Test
        fun `should throw if to narrow`() {
            expectThrows<IllegalArgumentException> { ColumnsLayout(Key.stringKey("left") columns 30, Key.stringKey("right") columns 35).shrinkBy(30) }
        }
    }
}
