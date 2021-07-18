@file:Suppress("SpellCheckingInspection")

package koodies.tracing.rendering

import koodies.exec.ExecAttributes
import koodies.test.Smoke
import koodies.test.expectThrows
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.ansiRemoved
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.prefixLinesWith
import koodies.text.Semantics.formattedAs
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
import koodies.tracing.rendering.ColumnsLayout.Companion.columns
import koodies.tracing.rendering.Renderer.Companion.log
import koodies.tracing.rendering.RenderingAttributes.Keys.DESCRIPTION
import koodies.tracing.rendering.Styles.Dotted
import koodies.tracing.rendering.Styles.None
import koodies.tracing.rendering.Styles.Solid
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

    private val EXTRA: Key<String, Any> = Key.stringKey("koodies.extra") { it.toString() }

    private val plain80 =
        "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod."
    private val ansi80 =
        "${"Lorem ipsum ${"dolor".ansi.cyan} sit".ansi.italic.underline} amet, ${"consetetur sadipscing".ansi.brightBlue} elitr, sed diam nonumy eirmod."

    private val settings = Settings(style = None)

    @Smoke @TestFactory
    fun TestSpan.`should render using styles`() = testEach(
        Solid to """
            ╭──╴One Two Three
            │
            │   LOREM IPSUM DOLOR SIT AMET, CONSETETUR SADIPSC     LOREM IPSUM DOLOR SIT AME
            │   ING ELITR, SED DIAM NONUMY EIRMOD.                 T, CONSETETUR SADIPSCING 
            │                                                      ELITR, SED DIAM NONUMY EI
            │                                                      RMOD.                    
            │   ╭──╴child-span
            │   │
            │   │   LOREM IPSUM DOLOR SIT AMET, CONSETETUR SAD     LOREM IPSUM DOLOR SIT AME
            │   │   IPSCING ELITR, SED DIAM NONUMY EIRMOD.         T, CONSETETUR SADIPSCING 
            │   │                                                  ELITR, SED DIAM NONUMY EI
            │   │                                                  RMOD.                    
            │   │   ╭──╴child-span
            │   │   │
            │   │   │   LOREM IPSUM DOLOR SIT AMET, CONSETETU     LOREM IPSUM DOLOR SIT AMET
            │   │   │   R SADIPSCING ELITR, SED DIAM NONUMY E     , CONSETETUR SADIPSCING EL
            │   │   │   IRMOD.                                    ITR, SED DIAM NONUMY EIRMO
            │   │   │                                             D.                        
            │   │   ϟ
            │   │   ╰──╴RuntimeException: Now Panic! at.(BlockRendererTest.kt:{})
            │   ϟ
            │   ╰──╴RuntimeException: message at.(BlockRendererTest.kt:{})
            │
            ╰──╴✔︎
        """.trimIndent(),
        Dotted to """
            ▶ One Two Three
            · LOREM IPSUM DOLOR SIT AMET, CONSETETUR SADIPSCIN     LOREM IPSUM DOLOR SIT AME
            · G ELITR, SED DIAM NONUMY EIRMOD.                     T, CONSETETUR SADIPSCING
            ·                                                      ELITR, SED DIAM NONUMY EI
            ·                                                      RMOD.
            · ▶ child-span
            · · LOREM IPSUM DOLOR SIT AMET, CONSETETUR SADIPSC     LOREM IPSUM DOLOR SIT AME
            · · ING ELITR, SED DIAM NONUMY EIRMOD.                 T, CONSETETUR SADIPSCING
            · ·                                                    ELITR, SED DIAM NONUMY EI
            · ·                                                    RMOD.
            · · ▶ child-span
            · · · LOREM IPSUM DOLOR SIT AMET, CONSETETUR SADIP     LOREM IPSUM DOLOR SIT AME
            · · · SCING ELITR, SED DIAM NONUMY EIRMOD.             T, CONSETETUR SADIPSCING
            · · ·                                                  ELITR, SED DIAM NONUMY EI
            · · ·                                                  RMOD.
            · · ϟ RuntimeException: Now Panic! at.(BlockRendererTest.kt:{})
            · ϟ RuntimeException: message at.(BlockRendererTest.kt:{})
            ✔︎
        """.trimIndent(),
        None to """
            One Two Three
            LOREM IPSUM DOLOR SIT AMET, CONSETETUR SADIPSCING      LOREM IPSUM DOLOR SIT AME
            ELITR, SED DIAM NONUMY EIRMOD.                         T, CONSETETUR SADIPSCING
                                                                   ELITR, SED DIAM NONUMY EI
                                                                   RMOD.
                child-span
                LOREM IPSUM DOLOR SIT AMET, CONSETETUR SADIPSC     LOREM IPSUM DOLOR SIT AME
                ING ELITR, SED DIAM NONUMY EIRMOD.                 T, CONSETETUR SADIPSCING
                                                                   ELITR, SED DIAM NONUMY EI
                                                                   RMOD.
                    child-span
                    LOREM IPSUM DOLOR SIT AMET, CONSETETUR SAD     LOREM IPSUM DOLOR SIT AME
                    IPSCING ELITR, SED DIAM NONUMY EIRMOD.         T, CONSETETUR SADIPSCING
                                                                   ELITR, SED DIAM NONUMY EI
                                                                   RMOD.
                    ϟ RuntimeException: Now Panic! at.(BlockRendererTest.kt:{})
                ϟ RuntimeException: message at.(BlockRendererTest.kt:{})
            ✔︎
        """.trimIndent(),
    ) { (style, expected) ->
        val rendered = capturing { printer ->
            BlockRenderer(Settings(
                style = style,
                layout = ColumnsLayout(DESCRIPTION columns 40, EXTRA columns 20, maxColumns = 80),
                contentFormatter = { it.toString().toUpperCase().ansi.random },
                decorationFormatter = { it.ansi.brightRed },
                returnValueTransform = { it },
                printer = printer,
            )).apply {

                start("One Two Three")
                log(ansi80, EXTRA to plain80)
                childRenderer().apply {
                    start("child-span")
                    log(ansi80, EXTRA to plain80)
                    childRenderer().apply {
                        start("child-span")
                        log(ansi80, EXTRA to plain80)
                        end(Result.failure<Unit>(RuntimeException("Now Panic!")))
                    }
                    end(Result.failure<Unit>(RuntimeException("message")))
                }

                end(Result.success(true))
            }
        }
        expecting { rendered } that { matchesCurlyPattern(expected) }
    }

    @Nested
    inner class Header {

        @Test
        fun TestSpan.`should render name`() {
            val rendered = capturing { BlockRenderer(settings.copy(printer = it)).start("name") }
            expectThat(rendered).matchesCurlyPattern("name")
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
    inner class Body {

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
                val rendered = capturing {
                    BlockRenderer(settings.copy(layout = ColumnsLayout(totalWidth = 40), printer = it))
                        .log("https://1234567890.1234567890.1234567890.1234567890")
                }
                expectThat(rendered).isEqualTo("https://1234567890.1234567890.1234567890.1234567890")
            }

            @Test
            fun TestSpan.`should delegate wrapping to rendereable`() {
                val rendereable = Renderable.of("!") { columns, rows -> "$this $columns x $rows" }
                val rendered = capturing {
                    BlockRenderer(settings.copy(layout = ColumnsLayout(DESCRIPTION columns 5), printer = it)).log(rendereable)
                }
                expectThat(rendered).matchesCurlyPattern("! 5 x$LF null")
            }
        }

        @Nested
        inner class MultipleColumns {

            private val twoColsLayout = ColumnsLayout(EXTRA columns 10, DESCRIPTION columns 25, maxColumns = 40)

            @Test
            fun TestSpan.`should render one plain event`() {
                val rendered =
                    capturing { BlockRenderer(settings.copy(layout = twoColsLayout, printer = it)).log(plain80, EXTRA to plain80) }
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
                val rendered = capturing { BlockRenderer(settings.copy(layout = twoColsLayout, printer = it)).log(ansi80, EXTRA to ansi80) }
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
                val rendered = capturing {
                    BlockRenderer(settings.copy(layout = twoColsLayout, printer = it)).log(plain80,
                        EXTRA to "https://1234567890.1234567890.1234567890.1234567890")
                }
                expectThat(rendered).isEqualTo("""
                        https://1234567890.1234567890.1234567890.1234567890Lorem ipsum dolor sit ame
                                       t, consetetur sadipscing 
                                       elitr, sed diam nonumy ei
                                       rmod.                    
                    """.trimIndent())
            }

            @Test
            fun TestSpan.`should delegate wrapping to rendereable`() {
                val rendereable = Renderable.of("!") { columns, rows -> "$this $columns x $rows" }
                val rendered = capturing {
                    BlockRenderer(settings.copy(layout = ColumnsLayout(DESCRIPTION columns 5, EXTRA columns 4), printer = it))
                        .log(rendereable, EXTRA to rendereable)
                }
                expectThat(rendered).isEqualTo("! 5 x null! 4 x null")
            }

            @Test
            fun TestSpan.`should handle more than two columns`() {
                val durationKey: Key<Long, Duration> = Key.longKey("duration") { it.inWholeMilliseconds }
                val format = ColumnsLayout(EXTRA columns 10, durationKey columns 10, DESCRIPTION columns 40, maxColumns = 60)
                val rendered = capturing {
                    BlockRenderer(settings.copy(layout = format, printer = it))
                        .log(plain80, EXTRA to "foo-bar", durationKey to 2.seconds)
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
                            .exception(RuntimeException("ex"), RenderableAttributes.of(EXTRA to plain80))
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
                        log("🔴🟠🟡🟢🔵🟣", EXTRA to "🟥🟧🟨🟩🟦🟪")
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
                        layout = ColumnsLayout(DESCRIPTION columns 6, EXTRA columns 5),
                        printer = it,
                    )).apply {
                        log("1234567890", EXTRA to "1234567890")
                        childRenderer().apply {
                            log("1234567890", EXTRA to "1234567890")
                            childRenderer().apply {
                                log("1234567890", EXTRA to "1234567890")
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

        @Test
        fun TestSpan.`should support ANSI`() {
            val rendered = capturing {
                BlockRenderer(settings.copy(printer = it))
                    .childRenderer()
                    .end(Result.success(ReturnValue.successful("line 1${LF}line2") { formattedAs.debug }))
            }
            expectThat(rendered).isEqualTo(
                "    \u001B[32m✔︎\u001B[39m \u001B[96mline 1\u001B[39m\n" +
                    "    \u001B[96mline2\u001B[39m")
        }
    }
}

fun CurrentSpan.capturing(block: (Printer) -> Unit): String {
    val printer = InMemoryPrinter()
    block(TeePrinter(printer) { log(it) })
    return printer.toString()
}

fun Renderer.start(name: CharSequence) = start(TraceId.NOOP, SpanId.NOOP, Renderable.of(name))
