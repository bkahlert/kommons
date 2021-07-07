@file:Suppress("SpellCheckingInspection")

package koodies.tracing.rendering

import koodies.exec.ExecAttributes
import koodies.test.Smoke
import koodies.test.expectThrows
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.ansiRemoved
import koodies.text.LineSeparators.prefixLinesWith
import koodies.text.lines
import koodies.text.matchesCurlyPattern
import koodies.text.toUpperCase
import koodies.time.seconds
import koodies.tracing.CurrentSpan
import koodies.tracing.Key
import koodies.tracing.NOOP
import koodies.tracing.SpanId
import koodies.tracing.TestSpan
import koodies.tracing.TraceId
import koodies.tracing.rendering.BlockStyles.Dotted
import koodies.tracing.rendering.BlockStyles.None
import koodies.tracing.rendering.BlockStyles.Solid
import koodies.tracing.rendering.ColumnsLayout.Companion.columns
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.length
import strikt.assertions.startsWith
import kotlin.time.Duration

class BlockRendererTest {

    private val plain80 =
        "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod."
    private val ansi80 =
        "${"Lorem ipsum ${"dolor".ansi.cyan} sit".ansi.italic.underline} amet, ${"consetetur sadipscing".ansi.brightBlue} elitr, sed diam nonumy eirmod."

    private val settings = Settings(blockStyle = None)

    @Smoke @TestFactory
    fun TestSpan.`should render using styles`() = testEach(
        Solid to """
            ╭──╴One Two Three
            │
            │   LOREM IPSUM DOLOR SIT AMET, CONSETETUR S     LOREM IPSUM DOLOR SI
            │   ADIPSCING ELITR, SED DIAM NONUMY EIRMOD.     T AMET, CONSETETUR S
            │                                                ADIPSCING ELITR, SED
            │                                                 DIAM NONUMY EIRMOD.
            │   ╭──╴child-span
            │   │
            │   │   LOREM IPSUM DOLOR SIT AMET, CONSETET     LOREM IPSUM DOLOR SI
            │   │   UR SADIPSCING ELITR, SED DIAM NONUMY     T AMET, CONSETETUR S
            │   │    EIRMOD.                                 ADIPSCING ELITR, SED
            │   │                                             DIAM NONUMY EIRMOD.
            │   │   ╭──╴child-span
            │   │   │
            │   │   │   LOREM IPSUM DOLOR SIT AMET, CONS     LOREM IPSUM DOLOR SI
            │   │   │   ETETUR SADIPSCING ELITR, SED DIA     T AMET, CONSETETUR S
            │   │   │   M NONUMY EIRMOD.                     ADIPSCING ELITR, SED
            │   │   │                                         DIAM NONUMY EIRMOD.
            │   │   ϟ
            │   │   ╰──╴RuntimeException: Now Panic! at.(BlockRendererTest.kt:{})
            │   ϟ
            │   ╰──╴RuntimeException: message at.(BlockRendererTest.kt:{})
            │
            ╰──╴✔︎
        """.trimIndent(),
        ::Dotted to """
            ▶ One Two Three
            · LOREM IPSUM DOLOR SIT AMET, CONSETETUR S     LOREM IPSUM DOLOR SI
            · ADIPSCING ELITR, SED DIAM NONUMY EIRMOD.     T AMET, CONSETETUR S
            ·                                              ADIPSCING ELITR, SED
            ·                                               DIAM NONUMY EIRMOD.
            · ▶ child-span
            · · LOREM IPSUM DOLOR SIT AMET, CONSETETUR     LOREM IPSUM DOLOR SI
            · ·  SADIPSCING ELITR, SED DIAM NONUMY EIR     T AMET, CONSETETUR S
            · · MOD.                                       ADIPSCING ELITR, SED
            · ·                                             DIAM NONUMY EIRMOD.
            · · ▶ child-span
            · · · LOREM IPSUM DOLOR SIT AMET, CONSETET     LOREM IPSUM DOLOR SI
            · · · UR SADIPSCING ELITR, SED DIAM NONUMY     T AMET, CONSETETUR S
            · · ·  EIRMOD.                                 ADIPSCING ELITR, SED
            · · ·                                           DIAM NONUMY EIRMOD.
            · · ϟ RuntimeException: Now Panic! at.(BlockRendererTest.kt:{})
            · ϟ RuntimeException: message at.(BlockRendererTest.kt:{})
            ✔︎
        """.trimIndent(),
        ::None to """
            One Two Three
            LOREM IPSUM DOLOR SIT AMET, CONSETETUR S     LOREM IPSUM DOLOR SI
            ADIPSCING ELITR, SED DIAM NONUMY EIRMOD.     T AMET, CONSETETUR S
                                                         ADIPSCING ELITR, SED
                                                          DIAM NONUMY EIRMOD.
                child-span
                LOREM IPSUM DOLOR SIT AMET, CONSETET     LOREM IPSUM DOLOR SI
                UR SADIPSCING ELITR, SED DIAM NONUMY     T AMET, CONSETETUR S
                 EIRMOD.                                 ADIPSCING ELITR, SED
                                                          DIAM NONUMY EIRMOD.
                    child-span
                    LOREM IPSUM DOLOR SIT AMET, CONS     LOREM IPSUM DOLOR SI
                    ETETUR SADIPSCING ELITR, SED DIA     T AMET, CONSETETUR S
                    M NONUMY EIRMOD.                     ADIPSCING ELITR, SED
                                                          DIAM NONUMY EIRMOD.
                    ϟ RuntimeException: Now Panic! at.(BlockRendererTest.kt:{})
                ϟ RuntimeException: message at.(BlockRendererTest.kt:{})
            ✔︎
        """.trimIndent(),
    ) { (style, expected) ->
        val rendered = capturing { printer ->
            BlockRenderer(Settings(
                blockStyle = style,
                layout = ColumnsLayout(RenderingAttributes.DESCRIPTION columns 40, RenderingAttributes.EXTRA columns 20),
                contentFormatter = { it.toString().toUpperCase().ansi.random },
                decorationFormatter = { it.toString().ansi.brightRed },
                returnValueTransform = { it },
                printer = printer,
            )).apply {

                start("One Two Three")
                log(ansi80, RenderingAttributes.EXTRA to plain80)
                childRenderer().apply {
                    start("child-span")
                    log(ansi80, RenderingAttributes.EXTRA to plain80)
                    childRenderer().apply {
                        start("child-span")
                        log(ansi80, RenderingAttributes.EXTRA to plain80)
                        end(Result.failure<Unit>(RuntimeException("Now Panic!")))
                    }
                    end(Result.failure<Unit>(RuntimeException("message")))
                }

                end(Result.success(true))
            }
        }
        expectThat(rendered).matchesCurlyPattern(expected)
    }

    @Nested
    inner class Header {

        @Test
        fun TestSpan.`should render name`() {
            val rendered = capturing { BlockRenderer(settings.copy(printer = it)).start("name") }
            expectThat(rendered.ansiRemoved).isEqualTo("name")
        }

        @Test
        fun TestSpan.`should render multi-line name`() {
            val rendered = capturing { BlockRenderer(settings.copy(printer = it)).start("line #1\nline #2") }
            expectThat(rendered.ansiRemoved).isEqualTo("""
                line #1
                line #2
            """.trimIndent())
        }
    }

    @Nested
    inner class Footer {

        @Test
        fun TestSpan.`should render success`() {
            val rendered = capturing { BlockRenderer(settings.copy(printer = it)).end(Result.success(true)) }
            expectThat(rendered.ansiRemoved).isEqualTo("✔︎")
        }

        @Test
        fun TestSpan.`should render exception`() {
            val rendered = capturing { BlockRenderer(settings.copy(printer = it)).end(Result.failure<Unit>(RuntimeException("failed"))) }
            expectThat(rendered.ansiRemoved).matchesCurlyPattern("ϟ RuntimeException: failed at.(BlockRendererTest.kt:{})")
        }
    }

    @Nested
    inner class SingleColumn {

        @Test
        fun TestSpan.`should render one plain event`() {
            val rendered = capturing { BlockRenderer(settings.copy(printer = it)).log(plain80) }
            expectThat(rendered).isEqualTo(plain80)
        }

        @Test
        fun TestSpan.`should render one ansi event`() {
            val rendered = capturing { BlockRenderer(settings.copy(printer = it)).log(ansi80) }
            expectThat(rendered).isEqualTo(ansi80)
        }

        @Test
        fun TestSpan.`should not render event without matching column`() {
            val rendered = capturing { BlockRenderer(settings.copy(printer = it)).event("unknown", RenderableAttributes.EMPTY) }
            expectThat(rendered).isEmpty()
        }

        @Test
        fun TestSpan.`should wrap too long plain event`() {
            val rendered = capturing { BlockRenderer(settings.copy(layout = ColumnsLayout(totalWidth = 40), printer = it)).log(plain80) }
            expectThat(rendered).isEqualTo("""
                Lorem ipsum dolor sit amet, consetetur s
                adipscing elitr, sed diam nonumy eirmod.
            """.trimIndent())
        }

        @Test
        fun TestSpan.`should wrap too long ansi event`() {
            val rendered = capturing { BlockRenderer(settings.copy(layout = ColumnsLayout(totalWidth = 40), printer = it)).log(ansi80) }
            expectThat(rendered).isEqualTo("""
                [4m[3mLorem ipsum [36mdolor[39m sit[23m[24m amet, [94mconsetetur s[39m
                [94madipscing[39m elitr, sed diam nonumy eirmod.
            """.trimIndent())
        }

        @Test
        fun TestSpan.`should not wrap links`() {
            val rendered =
                capturing {
                    BlockRenderer(settings.copy(layout = ColumnsLayout(totalWidth = 40), printer = it))
                        .log("https://1234567890.1234567890.1234567890.1234567890")
                }
            expectThat(rendered).isEqualTo("https://1234567890.1234567890.1234567890.1234567890")
        }
    }

    @Nested
    inner class MultipleColumns {

        private val twoColsLayout = ColumnsLayout(RenderingAttributes.EXTRA columns 10, RenderingAttributes.DESCRIPTION columns 25, maxColumns = 40)

        @Test
        fun TestSpan.`should render one plain event`() {
            val rendered = capturing { BlockRenderer(settings.copy(layout = twoColsLayout, printer = it)).log(plain80, RenderingAttributes.EXTRA to plain80) }
            expectThat(rendered).isEqualTo("""
                Lorem ipsu     Lorem ipsum dolor sit ame
                m dolor si     t, consetetur sadipscing 
                t amet, co     elitr, sed diam nonumy ei
                nsetetur s     rmod.                    
                adipscing      
                elitr, sed     
                 diam nonu     
                my eirmod.     
            """.trimIndent())
        }

        @Smoke @Test
        fun TestSpan.`should render one ansi event`() {
            val rendered = capturing { BlockRenderer(settings.copy(layout = twoColsLayout, printer = it)).log(ansi80, RenderingAttributes.EXTRA to ansi80) }
            expectThat(rendered).isEqualTo("""
                [4m[3mLorem ipsu[24;23m     [4m[3mLorem ipsum [36mdolor[39m sit[23m[24m ame
                [4;3mm [36mdolor[39m si[24;23m     t, [94mconsetetur sadipscing[39m 
                [4;3mt[23m[24m amet, [94mco[39m     elitr, sed diam nonumy ei
                [94mnsetetur s[39m     rmod.                    
                [94madipscing[39m      
                elitr, sed     
                 diam nonu     
                my eirmod.     
            """.trimIndent())
        }

        @Test
        fun TestSpan.`should not render event without matching column`() {
            val rendered = capturing {
                BlockRenderer(settings.copy(layout = twoColsLayout, printer = it)).event("unknown",
                    RenderableAttributes.of(ExecAttributes.NAME to "?"))
            }
            expectThat(rendered).isEmpty()
        }

        @Test
        fun TestSpan.`should leave column empty on missing attribute`() {
            val rendered = capturing { BlockRenderer(settings.copy(layout = twoColsLayout, printer = it)).log(ansi80) }
            expectThat(rendered).isEqualTo("""
                               [4m[3mLorem ipsum [36mdolor[39m sit[23m[24m ame
                               t, [94mconsetetur sadipscing[39m 
                               elitr, sed diam nonumy ei
                               rmod.                    
            """.trimIndent().prefixLinesWith("               "))
        }

        @Test
        fun TestSpan.`should not wrap links`() {
            val rendered =
                capturing {
                    BlockRenderer(settings.copy(layout = twoColsLayout, printer = it)).log(plain80,
                        RenderingAttributes.EXTRA to "https://1234567890.1234567890.1234567890.1234567890")
                }
            expectThat(rendered).isEqualTo("""
                https://1234567890.1234567890.1234567890.1234567890Lorem ipsum dolor sit ame
                               t, consetetur sadipscing 
                               elitr, sed diam nonumy ei
                               rmod.                    
            """.trimIndent())
        }

        @Test
        fun TestSpan.`should handle more than two columns`() {
            val durationKey: Key<Long, Duration> = Key.longKey("duration") { it.inWholeMilliseconds }
            val format =
                ColumnsLayout(RenderingAttributes.EXTRA columns 10, durationKey columns 10, RenderingAttributes.DESCRIPTION columns 40, maxColumns = 60)
            val rendered = capturing {
                BlockRenderer(settings.copy(layout = format, printer = it))
                    .log(plain80, RenderingAttributes.EXTRA to "foo-bar", durationKey to 2.seconds)
            }
            expectThat(rendered).isEqualTo("""
                foo-bar       2.00s        Lorem ipsum dolor sit amet, conse
                                           tetur sadipscing elitr, sed diam 
                                           nonumy eirmod.                   
            """.trimIndent())
        }


        @Test
        fun TestSpan.`should render exception`() {
            val rendered =
                capturing {
                    BlockRenderer(settings.copy(layout = twoColsLayout, printer = it))
                        .exception(RuntimeException("ex"), RenderableAttributes.of(RenderingAttributes.EXTRA to plain80))
                }
            expectThat(rendered).matchesCurlyPattern("""
                Lorem ipsu     java.lang.RuntimeException: ex
                m dolor si     	at koodies.tracing.rendering.BlockRendererTest{}
                t amet, co     	at koodies.tracing.rendering.BlockRendererTest{}
                {{}}
            """.trimIndent())
        }

        @Test
        fun TestSpan.`should render exception spanning all columns if no attributes provided`() {
            val rendered = capturing {
                BlockRenderer(settings.copy(layout = twoColsLayout, printer = it)).exception(RuntimeException("ex"), RenderableAttributes.EMPTY)
            }
            expectThat(rendered) {
                startsWith("java.lang.RuntimeException: ex".ansi.red)
                lines().any { length.isGreaterThan(40) }
            }
        }

        @Test
        fun TestSpan.`should log second columns on same column even if using wide characters`() {
            val rendered = capturing {
                BlockRenderer(settings.copy(layout = twoColsLayout, printer = it)).apply {
                    log("🔴🟠🟡🟢🔵🟣", RenderingAttributes.EXTRA to "🟥🟧🟨🟩🟦🟪")
                    log("1234567890".repeat(7))
                }
            }
            expectThat(rendered).isEqualTo("""
                🟥🟧🟨🟩🟦     🔴🟠🟡🟢🔵🟣
                🟪             
                               1234567890123456789012345
                               6789012345678901234567890
                               12345678901234567890     
            """.trimIndent())
        }
    }

    @Nested
    inner class Nesting {

        @Test
        fun TestSpan.`should increase left padding`() {
            val rendered = capturing {
                BlockRenderer(settings.copy(printer = it)).apply {
                    log("1234567890".repeat(10))
                    childRenderer().apply {
                        childRenderer().apply {
                            log("1234567890".repeat(10))
                        }
                        log("1234567890".repeat(10))
                    }
                    log("1234567890".repeat(10))
                }
            }
            expectThat(rendered).toStringIsEqualTo("""
                12345678901234567890123456789012345678901234567890123456789012345678901234567890
                12345678901234567890                                                            
                        123456789012345678901234567890123456789012345678901234567890123456789012
                        3456789012345678901234567890                                            
                    1234567890123456789012345678901234567890123456789012345678901234567890123456
                    789012345678901234567890                                                    
                12345678901234567890123456789012345678901234567890123456789012345678901234567890
                12345678901234567890                                                            
            """.trimIndent())
        }

        @Test
        fun TestSpan.`should throw if left column has no more space`() {
            expectThrows<IllegalArgumentException> {
                capturing {
                    BlockRenderer(settings.copy(
                        layout = ColumnsLayout(RenderingAttributes.DESCRIPTION columns 6, RenderingAttributes.EXTRA columns 5),
                        printer = it,
                    )).apply {
                        log("1234567890", RenderingAttributes.EXTRA to "1234567890")
                        childRenderer().apply {
                            log("1234567890", RenderingAttributes.EXTRA to "1234567890")
                            childRenderer().apply {
                                log("1234567890", RenderingAttributes.EXTRA to "1234567890")
                            }
                        }
                    }
                }
            }
        }

        @Test
        fun TestSpan.`should be customizable`() {
            val rendered = capturing {
                BlockRenderer(settings.copy(printer = it)).apply {
                    log("foo")
                    childRenderer { it(copy(contentFormatter = { content -> "!$content!" })) }.apply {
                        log("bar")
                    }
                    log("baz")
                }
            }
            expectThat(rendered).matchesCurlyPattern("""
                foo
                    !bar!
                baz
            """.trimIndent())
        }
    }
}

fun CurrentSpan.capturing(block: (Printer) -> Unit): String {
    val printer = InMemoryPrinter()
    block(TeePrinter(printer) { log(it) })
    return printer.toString()
}

fun Renderer.start(name: CharSequence) = start(TraceId.NOOP, SpanId.NOOP, Renderable.of(name))
