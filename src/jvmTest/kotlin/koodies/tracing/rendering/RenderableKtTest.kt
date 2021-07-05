package koodies.tracing.rendering

import koodies.text.ANSI.Text.Companion.ansi
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo

class RenderableKtTest {

    @Nested
    inner class RenderingRenderable {

        @Test
        fun `should render using renderable`() {
            expectThat(Renderable { columns, rows -> "$columns x $rows" }.render(20, 10)).isEqualTo("20 x 10")
        }
    }

    @Nested
    inner class RenderingAny {

        @Test
        fun `should render using toString and truncation`() {
            expectThat(Renderable.of("""
                123456789012345678901234567890
                1234567890123456789012345
                12345678901234567890
                123456789012345
                1234567890
                12345
                
                12345
                1234567890
                123456789012345
                12345678901234567890
                1234567890123456789012345
                123456789012345678901234567890
            """.trimIndent()).render(20, 10)).isEqualTo("""
                123456789 … 34567890
                123456789 … 89012345
                12345678901234567890
                123456789012345
                1234567890
                12345
                
                12345
                1234567890
                123456789012345
            """.trimIndent())
        }

        @Test
        fun `should crop by columns`() {
            expectThat(Renderable.of("⮕⮕⮕⮕").render(7, 1)).isEqualTo("⮕ … ⮕")
        }

        @Test
        fun `should support ANSI`() {
            expectThat(Renderable.of("blue-blue".ansi.blue).render(7, 1)).isEqualTo("${"bl".ansi.blue} … ${"ue".ansi.blue}")
        }

        @Test
        fun `should not truncate URIs`() {
            expectThat(Renderable.of("https://domain.tld").render(1, 1)).isEqualTo("https://domain.tld")
        }
    }

    @Nested
    inner class RenderingNull {

        @Test
        fun `should render as empty string`() {
            expectThat(Renderable.of(null).render(20, 10)).isEmpty()
        }
    }
}
