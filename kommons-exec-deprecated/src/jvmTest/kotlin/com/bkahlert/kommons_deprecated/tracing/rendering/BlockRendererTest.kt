@file:Suppress("SpellCheckingInspection")

package com.bkahlert.kommons_deprecated.tracing.rendering

import com.bkahlert.kommons.ansiRemoved
import com.bkahlert.kommons.test.junit.testEach
import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons_deprecated.exec.ExecAttributes
import com.bkahlert.kommons_deprecated.test.AnsiRequiring
import com.bkahlert.kommons_deprecated.test.Smoke
import com.bkahlert.kommons_deprecated.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons_deprecated.text.Semantics.formattedAs
import com.bkahlert.kommons_deprecated.tracing.Key
import com.bkahlert.kommons_deprecated.tracing.NOOP
import com.bkahlert.kommons_deprecated.tracing.SpanId
import com.bkahlert.kommons_deprecated.tracing.SpanScope
import com.bkahlert.kommons_deprecated.tracing.TestSpanScope
import com.bkahlert.kommons_deprecated.tracing.TraceId
import com.bkahlert.kommons_deprecated.tracing.rendering.ColumnsLayout.Companion.columns
import com.bkahlert.kommons_deprecated.tracing.rendering.Renderer.Companion.log
import com.bkahlert.kommons_deprecated.tracing.rendering.RenderingAttributes.Keys.DESCRIPTION
import com.bkahlert.kommons_deprecated.tracing.rendering.Styles.Dotted
import com.bkahlert.kommons_deprecated.tracing.rendering.Styles.None
import com.bkahlert.kommons_deprecated.tracing.rendering.Styles.Solid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAny
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class BlockRendererTest {

    private val EXTRA: Key<String, Any> = Key.stringKey("kommons.extra") { it.toString() }

    private val plain80 =
        "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod"
    private val ansi80 =
        "\u001B[3;4mLorem ipsum \u001B[36mdolor\u001B[39m sit\u001B[23;24m amet, \u001B[94mconsetetur sadipscing\u001B[39m elitr, sed diam nonumy eirmod"

    private val settings = Settings(style = None)

    @Smoke @TestFactory
    fun TestSpanScope.`should render using styles`() = testEach(
        Solid to """
            ╭──╴One Two Three
            │
            │   LOREM IPSUM DOLOR SIT AMET, CONSETETUR             LOREM IPSUM DOLOR  
            │   SADIPSCING ELITR, SED DIAM NONUMY EIRMOD           SIT AMET, CONSETETUR
            │                                                      SADIPSCING ELITR,  
            │                                                      SED DIAM NONUMY    
            │                                                      EIRMOD              
            │   ╭──╴child-span
            │   │
            │   │   LOREM IPSUM DOLOR SIT AMET, CONSETETUR         LOREM IPSUM DOLOR  
            │   │   SADIPSCING ELITR, SED DIAM NONUMY EIRMOD       SIT AMET, CONSETETUR
            │   │                                                  SADIPSCING ELITR,  
            │   │                                                  SED DIAM NONUMY    
            │   │                                                  EIRMOD              
            │   │   ╭──╴child-span
            │   │   │
            │   │   │   LOREM IPSUM DOLOR SIT AMET,               LOREM IPSUM DOLOR SIT
            │   │   │   CONSETETUR SADIPSCING ELITR, SED DIAM     AMET, CONSETETUR    
            │   │   │   NONUMY EIRMOD                             SADIPSCING ELITR, SED
            │   │   │                                             DIAM NONUMY EIRMOD   
            │   │   ϟ
            │   │   ╰──╴RuntimeException: Now Panic! at.(BlockRendererTest.kt:*)
            │   ϟ
            │   ╰──╴RuntimeException: message at.(BlockRendererTest.kt:*)
            │
            ╰──╴✔︎
        """.trimIndent(),
        Dotted to """
            ▶ One Two Three
            · LOREM IPSUM DOLOR SIT AMET, CONSETETUR               LOREM IPSUM DOLOR  
            · SADIPSCING ELITR, SED DIAM NONUMY EIRMOD             SIT AMET, CONSETETUR
            ·                                                      SADIPSCING ELITR,  
            ·                                                      SED DIAM NONUMY    
            ·                                                      EIRMOD              
            · ▶ child-span
            · · LOREM IPSUM DOLOR SIT AMET, CONSETETUR             LOREM IPSUM DOLOR  
            · · SADIPSCING ELITR, SED DIAM NONUMY EIRMOD           SIT AMET, CONSETETUR
            · ·                                                    SADIPSCING ELITR,  
            · ·                                                    SED DIAM NONUMY    
            · ·                                                    EIRMOD              
            · · ▶ child-span
            · · · LOREM IPSUM DOLOR SIT AMET, CONSETETUR           LOREM IPSUM DOLOR  
            · · · SADIPSCING ELITR, SED DIAM NONUMY EIRMOD         SIT AMET, CONSETETUR
            · · ·                                                  SADIPSCING ELITR,  
            · · ·                                                  SED DIAM NONUMY    
            · · ·                                                  EIRMOD              
            · · ϟ RuntimeException: Now Panic! at.(BlockRendererTest.kt:*)
            · ϟ RuntimeException: message at.(BlockRendererTest.kt:*)
            ✔︎
        """.trimIndent(),
        None to """
            One Two Three                                                                  
            LOREM IPSUM DOLOR SIT AMET, CONSETETUR       LOREM IPSUM DOLOR SIT AMET, 
            SADIPSCING ELITR, SED DIAM NONUMY EIRMOD     CONSETETUR SADIPSCING ELITR,
                                                         SED DIAM NONUMY EIRMOD       
                child-span                                                                  
                LOREM IPSUM DOLOR SIT AMET, CONSETETUR             LOREM IPSUM DOLOR  
                SADIPSCING ELITR, SED DIAM NONUMY EIRMOD           SIT AMET, CONSETETUR
                                                                   SADIPSCING ELITR,  
                                                                   SED DIAM NONUMY    
                                                                   EIRMOD              
                    child-span                                                              
                    LOREM IPSUM DOLOR SIT AMET, CONSETETUR         LOREM IPSUM DOLOR  
                    SADIPSCING ELITR, SED DIAM NONUMY EIRMOD       SIT AMET, CONSETETUR
                                                                   SADIPSCING ELITR,  
                                                                   SED DIAM NONUMY    
                                                                   EIRMOD              
                    ϟ RuntimeException: Now Panic! at.(BlockRendererTest.kt:*)
                ϟ RuntimeException: message at.(BlockRendererTest.kt:*)
            ✔︎
        """.trimIndent(),
    ) { (style, expected) ->
        val rendered = capturing { printer ->
            BlockRenderer(
                Settings(
                    style = style,
                    layout = ColumnsLayout(DESCRIPTION columns 40, EXTRA columns 20, maxColumns = 80),
                    contentFormatter = { it.toString().uppercase().ansi.random },
                    decorationFormatter = { it.ansi.brightRed },
                    returnValueTransform = { it },
                    printer = printer,
                )
            ).apply {

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
        rendered.ansiRemoved shouldMatchGlob expected
    }

    @Nested
    inner class Header {
        @Disabled
        @Test
        fun TestSpanScope.`should render name`() {
            val rendered = capturing { BlockRenderer(settings.copy(printer = it)).start("name") }
            rendered shouldMatchGlob "name*"
        }

        @Test
        fun TestSpanScope.`should render multi-line name`() {
            val rendered = capturing { BlockRenderer(settings.copy(printer = it)).start("line #1\nline #2") }
            rendered.ansiRemoved shouldBe """
                line #1
                line #2
            """.trimIndent()
        }
    }

    @Nested
    inner class Body {

        @Nested
        inner class SingleColumn {

            @Test
            fun TestSpanScope.`should render one plain event`() {
                val rendered = capturing { BlockRenderer(settings.copy(printer = it)).log(plain80) }
                rendered shouldBe plain80
            }

            @AnsiRequiring @Test
            fun TestSpanScope.`should render one ansi event`() {
                val rendered = capturing { BlockRenderer(settings.copy(printer = it)).log(ansi80) }
                rendered shouldBe ansi80
            }

            @Test
            fun TestSpanScope.`should not render event without matching column`() {
                val rendered = capturing { BlockRenderer(settings.copy(printer = it)).event("unknown", RenderableAttributes.EMPTY) }
                rendered.shouldBeEmpty()
            }

            @Test
            fun TestSpanScope.`should wrap too long plain event`() {
                val rendered = capturing { BlockRenderer(settings.copy(layout = ColumnsLayout(totalWidth = 40), printer = it)).log(plain80) }
                rendered shouldBe """
                    Lorem ipsum dolor sit amet, consetetur
                    sadipscing elitr, sed diam nonumy eirmod
                """.trimIndent()
            }

            @AnsiRequiring @Test
            fun TestSpanScope.`should wrap too long ansi event`() {
                val rendered = capturing { BlockRenderer(settings.copy(layout = ColumnsLayout(totalWidth = 40), printer = it)).log(ansi80) }
                rendered shouldBe """
                    [3;4mLorem ipsum [36mdolor[39m sit[23;24m amet, [94mconsetetur  [39m
                    [94msadipscing[39m elitr, sed diam nonumy eirmod
                """.trimIndent()
            }

            @Disabled
            @Test
            fun TestSpanScope.`should not wrap links`() {
                val rendered = capturing {
                    BlockRenderer(settings.copy(layout = ColumnsLayout(totalWidth = 40), printer = it))
                        .log("https://1234567890.1234567890.1234567890.1234567890")
                }
                rendered shouldBe "https://1234567890.1234567890.1234567890.1234567890"
            }

            @Disabled
            @Test
            fun TestSpanScope.`should delegate wrapping to rendereable`() {
                val rendereable = Renderable.of("!") { columns, rows -> "$this $columns x $rows" }
                val rendered = capturing {
                    BlockRenderer(settings.copy(layout = ColumnsLayout(DESCRIPTION columns 5), printer = it)).log(rendereable)
                }
                rendered shouldMatchGlob "! 5 x$LF null"
            }
        }

        @Nested
        inner class MultipleColumns {

            private val twoColsLayout = ColumnsLayout(EXTRA columns 10, DESCRIPTION columns 25, maxColumns = 40)

            @Disabled
            @Test
            fun TestSpanScope.`should render one plain event`() {
                val rendered =
                    capturing { BlockRenderer(settings.copy(layout = twoColsLayout, printer = it)).log(plain80, EXTRA to plain80) }
                rendered shouldMatchGlob """
                        Lorem ipsu     Lorem ipsum dolor sit ame
                        m dolor si     t, consetetur sadipscing
                        t amet, co     elitr, sed diam nonumy ei
                        nsetetur s     rmod.
                        adipscing
                        elitr, sed
                         diam nonu
                        my eirmod.
                    """.trimIndent()
            }

            @Disabled
            @AnsiRequiring @Smoke @Test
            fun TestSpanScope.`should render one ansi event`() {
                val rendered = capturing { BlockRenderer(settings.copy(layout = twoColsLayout, printer = it)).log(ansi80, EXTRA to ansi80) }
                rendered shouldMatchGlob """
                [4m[3mLorem ipsu[24;23m     [4m[3mLorem ipsum [36mdolor[39m sit[23m[24m ame
                        [4;3mm [36mdolor[39m si[24;23m     t, [94mconsetetur sadipscing[39m
                        [4;3mt[23m[24m amet, [94mco[39m     elitr, sed diam nonumy ei
                        [94mnsetetur s[39m     rmod.
                        [94madipscing[39m
                        elitr, sed
                         diam nonu
                        my eirmod.
                    """.trimIndent()
            }

            @Test
            fun TestSpanScope.`should not render event without matching column`() {
                val rendered = capturing {
                    BlockRenderer(settings.copy(layout = twoColsLayout, printer = it)).event(
                        "unknown",
                        RenderableAttributes.of(ExecAttributes.NAME to "?")
                    )
                }
                rendered.shouldBeEmpty()
            }

            @Disabled
            @AnsiRequiring @Test
            fun TestSpanScope.`should leave column empty on missing attribute`() {
                val rendered = capturing { BlockRenderer(settings.copy(layout = twoColsLayout, printer = it)).log(ansi80) }
                rendered shouldBe """
                       [4m[3mLorem ipsum [36mdolor[39m sit[23m[24m ame
                       t, [94mconsetetur sadipscing[39m
                       elitr, sed diam nonumy ei
                       rmod.
                   """.trimIndent().prependIndent("               ")
            }

            @Disabled
            @Test
            fun TestSpanScope.`should not wrap links`() {
                val rendered = capturing {
                    BlockRenderer(settings.copy(layout = twoColsLayout, printer = it)).log(
                        plain80,
                        EXTRA to "https://1234567890.1234567890.1234567890.1234567890"
                    )
                }
                rendered shouldBe """
                        https://1234567890.1234567890.1234567890.1234567890Lorem ipsum dolor sit ame
                                       t, consetetur sadipscing
                                       elitr, sed diam nonumy ei
                                       rmod.
                    """.trimIndent()
            }

            @Disabled
            @Test
            fun TestSpanScope.`should delegate wrapping to rendereable`() {
                val rendereable = Renderable.of("!") { columns, rows -> "$this $columns x $rows" }
                val rendered = capturing {
                    BlockRenderer(settings.copy(layout = ColumnsLayout(DESCRIPTION columns 5, EXTRA columns 4), printer = it))
                        .log(rendereable, EXTRA to rendereable)
                }
                rendered shouldBe "! 5 x null! 4 x null"
            }

            @Disabled
            @Test
            fun TestSpanScope.`should handle more than two columns`() {
                val durationKey: Key<Long, Duration> = Key.longKey("duration") { it.inWholeMilliseconds }
                val format = ColumnsLayout(EXTRA columns 10, durationKey columns 10, DESCRIPTION columns 40, maxColumns = 60)
                val rendered = capturing {
                    BlockRenderer(settings.copy(layout = format, printer = it))
                        .log(plain80, EXTRA to "foo-bar", durationKey to 2.seconds)
                }
                rendered shouldBe """
                        foo-bar       2s           Lorem ipsum dolor sit amet, conse
                                                   tetur sadipscing elitr, sed diam
                                                   nonumy eirmod.
                    """.trimIndent()
            }

            @Disabled
            @Test
            fun TestSpanScope.`should render exception`() {
                val rendered =
                    capturing {
                        BlockRenderer(settings.copy(layout = twoColsLayout, printer = it))
                            .exception(RuntimeException("ex"), RenderableAttributes.of(EXTRA to plain80))
                    }
                rendered shouldMatchGlob """
                        Lorem ipsu     java.lang.RuntimeException: ex
                        m dolor si     	at com.bkahlert.kommons_deprecated.tracing.rendering.BlockRendererTest*
                        t amet, co     	at com.bkahlert.kommons_deprecated.tracing.rendering.BlockRendererTest*
                        **
                    """.trimIndent()
            }

            @Test
            fun TestSpanScope.`should render exception spanning all columns if no attributes provided`() {
                val rendered = capturing {
                    BlockRenderer(settings.copy(layout = twoColsLayout, printer = it)).exception(RuntimeException("ex"), RenderableAttributes.EMPTY)
                }
                rendered should {
                    it shouldStartWith "java.lang.RuntimeException: ex".ansi.red
                    it.lines().forAny { it.length shouldBeGreaterThan 40 }
                }
            }
        }
    }

    @Nested
    inner class Footer {

        @Test
        fun TestSpanScope.`should render success`() {
            val rendered = capturing { BlockRenderer(settings.copy(printer = it)).end(Result.success(true)) }
            rendered.ansiRemoved shouldBe "✔︎"
        }

        @Disabled
        @Test
        fun TestSpanScope.`should render exception`() {
            val rendered = capturing { BlockRenderer(settings.copy(printer = it)).end(Result.failure<Unit>(RuntimeException("failed"))) }
            rendered shouldMatchGlob "ϟ RuntimeException: failed at.(BlockRendererTest.kt:*)"
        }
    }

    @Nested
    inner class Nesting {
        @Disabled
        @Test
        fun TestSpanScope.`should increase left padding`() {
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
            rendered shouldBe """
                12345678901234567890123456789012345678901234567890123456789012345678901234567890
                12345678901234567890
                        123456789012345678901234567890123456789012345678901234567890123456789012
                        3456789012345678901234567890
                    1234567890123456789012345678901234567890123456789012345678901234567890123456
                    789012345678901234567890
                12345678901234567890123456789012345678901234567890123456789012345678901234567890
                12345678901234567890
            """.trimIndent()
        }

        @Test
        fun TestSpanScope.`should throw if left column has no more space`() {
            shouldThrow<IllegalArgumentException> {
                capturing {
                    BlockRenderer(
                        settings.copy(
                            layout = ColumnsLayout(DESCRIPTION columns 6, EXTRA columns 5),
                            printer = it,
                        )
                    ).apply {
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
        fun TestSpanScope.`should be customizable`() {
            val rendered = capturing {
                BlockRenderer(settings.copy(printer = it)).apply {
                    log("foo")
                    childRenderer { it.create(copy(contentFormatter = { content -> "!$content!" })) }.apply {
                        log("bar")
                    }
                    log("baz")
                }
            }
            rendered shouldMatchGlob """
                foo
                    !bar!
                baz
            """.trimIndent()
        }

        @Disabled
        @AnsiRequiring @Test
        fun TestSpanScope.`should support ANSI`() {
            val rendered = capturing {
                BlockRenderer(settings.copy(printer = it))
                    .childRenderer()
                    .end(Result.success(ReturnValue.successful("line 1${LF}line2") { formattedAs.debug }))
            }
            rendered shouldBe "    \u001B[32m✔︎\u001B[39m \u001B[96mline 1\u001B[39m\n" +
                "    \u001B[96mline2\u001B[39m"
        }
    }
}

fun SpanScope.capturing(block: (Printer) -> Unit): String {
    val printer = InMemoryPrinter()
    block(TeePrinter(printer) { log(it) })
    return printer.toString()
}

fun Renderer.start(name: CharSequence) = start(TraceId.NOOP, SpanId.NOOP, Renderable.of(name))
