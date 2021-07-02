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
                1234567890…234567890
                1234567890…789012345
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
            expectThat(Renderable.of("⮕⮕⮕").render(5, 1)).isEqualTo("⮕…⮕")
        }

        @Test
        fun `should support ANSI`() {
            expectThat(Renderable.of("blue".ansi.blue).render(3, 1)).isEqualTo("${"b".ansi.blue}…${"e".ansi.blue}")
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
