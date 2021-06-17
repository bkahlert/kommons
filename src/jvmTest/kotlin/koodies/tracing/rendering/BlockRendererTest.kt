package koodies.tracing.rendering

import koodies.logging.ReturnValue
import koodies.test.Smoke
import koodies.test.expectThrows
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import koodies.text.ANSI.FilteringFormatter
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.ansiRemoved
import koodies.text.AnsiString
import koodies.text.AnsiString.Companion.asAnsiString
import koodies.text.LineSeparators.prefixLinesWith
import koodies.text.lines
import koodies.text.matchesCurlyPattern
import koodies.text.toUpperCase
import koodies.time.Now
import koodies.time.seconds
import koodies.tracing.NOOP
import koodies.tracing.Span
import koodies.tracing.Span.State.Ended.Failed
import koodies.tracing.Span.State.Ended.Succeeded
import koodies.tracing.SpanId
import koodies.tracing.TraceId
import koodies.tracing.log
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

class BlockRendererTest {

    private val plain80 =
        "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod."
    private val ansi80 =
        "${"Lorem ipsum ${"dolor".ansi.cyan} sit".ansi.italic.underline} amet, ${"consetetur sadipscing".ansi.brightBlue} elitr, sed diam nonumy eirmod."

    private val settings = Settings(blockStyle = BlockStyles.None)

    @Smoke @TestFactory
    fun Span.`should render using styles`() = testEach(
        BlockStyles.Rounded to """
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
            │   ╵
            │   ╵
            │   ⏳️
            │
            ╰──╴✔︎
        """.trimIndent(),
        BlockStyles.Dotted to """
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
            · ⏳️
            ✔︎
        """.trimIndent(),
        BlockStyles.None to """
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
                ⏳️
            ✔︎
        """.trimIndent(),
    ) { (style, expected) ->
        val rendered = capturing { printer ->
            BlockRenderer("One Two Three", Settings(
                blockStyle = style,
                layout = ColumnsLayout(Span.Description to 40, "status" to 20),
                contentFormatter = { it.toUpperCase().ansi.random },
                decorationFormatter = { it.ansi.brightRed },
                returnValueFormatter = { it },
            ), printer).apply {

                start()
                log(ansi80, "status" to plain80)
                nestedRenderer("child-span").apply {
                    start()
                    log(ansi80, "status" to plain80)
                    nestedRenderer("child-span").apply {
                        start()
                        log(ansi80, "status" to plain80)
                        end(Failed(RuntimeException("Now Panic!"), Now.instant))
                    }
                    end(Succeeded(object : ReturnValue {
                        override val successful: Boolean? = null
                    }, Now.instant))
                }

                end(Succeeded("Done", Now.instant))
            }
        }
        expectThat(rendered).matchesCurlyPattern(expected)
    }

    @Nested
    inner class Header {

        @Test
        fun Span.`should render name`() {
            val rendered = capturing { BlockRenderer("name", settings, it).start() }
            expectThat(rendered.ansiRemoved).isEqualTo("name")
        }

        @Test
        fun Span.`should render multi-line name`() {
            val rendered = capturing { BlockRenderer("line #1\nline #2", settings, it).start() }
            expectThat(rendered.ansiRemoved).isEqualTo("""
                line #1
                line #2
            """.trimIndent())
        }
    }

    @Nested
    inner class Footer {

        @Test
        fun Span.`should render success`() {
            val rendered = capturing { BlockRenderer("name", settings, it).end(Succeeded("SUCCESS", Now.instant)) }
            expectThat(rendered.ansiRemoved).isEqualTo("✔︎")
        }

        @Test
        fun Span.`should render return value`() {
            val rendered = capturing { BlockRenderer("name", settings, it).end(Succeeded(ReturnValue.of(null), Now.instant)) }
            expectThat(rendered.ansiRemoved).isEqualTo("␀")
        }

        @Test
        fun Span.`should render exception`() {
            val rendered = capturing { BlockRenderer("name", settings, it).end(Failed(RuntimeException("failed"), Now.instant)) }
            expectThat(rendered.ansiRemoved).matchesCurlyPattern("ϟ RuntimeException: failed at.(BlockRendererTest.kt:{})")
        }
    }

    @Nested
    inner class SingleColumn {

        @Test
        fun Span.`should render one plain event`() {
            val rendered = capturing { BlockRenderer("name", settings, it).log(plain80) }
            expectThat(rendered).isEqualTo(plain80)
        }

        @Test
        fun Span.`should render one ansi event`() {
            val rendered = capturing { BlockRenderer("name", settings, it).log(ansi80) }
            expectThat(rendered).isEqualTo(ansi80)
        }

        @Test
        fun Span.`should not render event without matching column`() {
            val rendered = capturing { BlockRenderer("name", settings, it).event("unknown") }
            expectThat(rendered).isEmpty()
        }

        @Test
        fun Span.`should wrap too long plain event`() {
            val rendered = capturing { BlockRenderer("name", settings.copy(layout = ColumnsLayout(totalWidth = 40)), it).log(plain80) }
            expectThat(rendered).isEqualTo("""
                Lorem ipsum dolor sit amet, consetetur s
                adipscing elitr, sed diam nonumy eirmod.
            """.trimIndent())
        }

        @Test
        fun Span.`should wrap too long ansi event`() {
            val rendered = capturing { BlockRenderer("name", settings.copy(layout = ColumnsLayout(totalWidth = 40)), it).log(ansi80) }
            expectThat(rendered).isEqualTo("""
                [4m[3mLorem ipsum [36mdolor[39m sit[23m[24m amet, [94mconsetetur s[39m
                [94madipscing[39m elitr, sed diam nonumy eirmod.
            """.trimIndent())
        }

        @Test
        fun Span.`should not wrap links`() {
            val rendered =
                capturing {
                    BlockRenderer("name", settings.copy(layout = ColumnsLayout(totalWidth = 40)), it)
                        .log("http://1234567890.1234567890.1234567890.1234567890")
                }
            expectThat(rendered).isEqualTo("http://1234567890.1234567890.1234567890.1234567890")
        }
    }

    @Nested
    inner class MultipleColumns {

        private val format = ColumnsLayout("status" to 10, Span.Description to 25, maxColumns = 40)

        @Test
        fun Span.`should render one plain event`() {
            val rendered = capturing { BlockRenderer("name", settings.copy(layout = format), it).log(plain80, "status" to plain80) }
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
        fun Span.`should render one ansi event`() {
            val rendered = capturing { BlockRenderer("name", settings.copy(layout = format), it).log(ansi80, "status" to ansi80) }
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
        fun Span.`should not render event without matching column`() {
            val rendered = capturing { BlockRenderer("name", settings.copy(layout = format), it).event("unknown", mapOf("key" to "value")) }
            expectThat(rendered).isEmpty()
        }

        @Test
        fun Span.`should leave column empty on missing attribute`() {
            val rendered = capturing { BlockRenderer("name", settings.copy(layout = format), it).log(ansi80) }
            expectThat(rendered).isEqualTo("""
                               [4m[3mLorem ipsum [36mdolor[39m sit[23m[24m ame
                               t, [94mconsetetur sadipscing[39m 
                               elitr, sed diam nonumy ei
                               rmod.                    
            """.trimIndent().prefixLinesWith("               "))
        }

        @Test
        fun Span.`should not wrap links`() {
            val rendered =
                capturing {
                    BlockRenderer("name", settings.copy(layout = format), it).log(plain80,
                        "status" to "http://1234567890.1234567890.1234567890.1234567890")
                }
            expectThat(rendered).isEqualTo("""
                http://1234567890.1234567890.1234567890.1234567890Lorem ipsum dolor sit ame
                               t, consetetur sadipscing 
                               elitr, sed diam nonumy ei
                               rmod.                    
            """.trimIndent())
        }

        @Test
        fun Span.`should handle more than two columns`() {
            val format = ColumnsLayout("status" to 10, "duration" to 10, Span.Description to 40, maxColumns = 60)
            val rendered = capturing { BlockRenderer("name", settings.copy(layout = format), it).log(plain80, "status" to "foo-bar", "duration" to 2.seconds) }
            expectThat(rendered).isEqualTo("""
                foo-bar       2.00s        Lorem ipsum dolor sit amet, conse
                                           tetur sadipscing elitr, sed diam 
                                           nonumy eirmod.                   
            """.trimIndent())
        }

        @Test
        fun Span.`should keep mutable character sequences intact`() {
            val customSequence = "bold".ansi.bold.asAnsiString().also { check(it.length == 4) { "precondition not met" } }
            val format = ColumnsLayout(Span.Description to 20, "col-2" to 20, maxColumns = 60)
            val formatter = FilteringFormatter { if (it is AnsiString) "SUCCESS" else "CharSequence destroyed: $it" }
            val rendered =
                capturing {
                    BlockRenderer("name", settings.copy(layout = format, contentFormatter = formatter), it)
                        .log(customSequence, "col-1" to customSequence)
                }
            expectThat(rendered.trim()).isEqualTo("SUCCESS")
        }


        @Test
        fun Span.`should render exception`() {
            val rendered =
                capturing { BlockRenderer("name", settings.copy(layout = format), it).exception(RuntimeException("ex"), mapOf("status" to plain80)) }
            expectThat(rendered).matchesCurlyPattern("""
                Lorem ipsu     java.lang.RuntimeException: ex
                m dolor si     	at koodies.tracing.rendering.BlockRendererTest{}
                t amet, co     	at koodies.tracing.rendering.BlockRendererTest{}
                {{}}
            """.trimIndent())
        }

        @Test
        fun Span.`should render exception spanning all columns if no attributes provided`() {
            val rendered = capturing { BlockRenderer("name", settings.copy(layout = format), it).exception(RuntimeException("ex")) }
            expectThat(rendered) {
                startsWith("java.lang.RuntimeException: ex".ansi.red)
                lines().any { length.isGreaterThan(40) }
            }
        }

        @Test
        fun Span.`should log second columns on same column even if using wide characters`() {
            val rendered = capturing {
                BlockRenderer("name", settings.copy(layout = format), it).apply {
                    log("🔴🟠🟡🟢🔵🟣", "status" to "🟥🟧🟨🟩🟦🟪")
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
        fun Span.`should increase left padding`() {
            val rendered = capturing {
                BlockRenderer("name", settings, it).apply {
                    log("1234567890".repeat(10))
                    nestedRenderer("child").apply {
                        nestedRenderer("child").apply {
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
        fun Span.`should throw if left column has no more space`() {
            expectThrows<IllegalArgumentException> {
                capturing {
                    BlockRenderer("name", settings.copy(layout = ColumnsLayout("description" to 6, "right" to 5)), it).apply {
                        log("1234567890", "right" to "1234567890")
                        nestedRenderer("child").apply {
                            log("1234567890", "right" to "1234567890")
                            nestedRenderer("child").apply {
                                log("1234567890", "right" to "1234567890")
                            }
                        }
                    }
                }
            }
        }

        @Test
        fun Span.`should be customizable`() {
            val rendered = capturing {
                BlockRenderer("name", settings, it).apply {
                    log("foo")
                    nestedRenderer("child") { copy(contentFormatter = { ">> $it <<" }) }.apply {
                        log("bar")
                    }
                    log("baz")
                }
            }
            expectThat(rendered).matchesCurlyPattern("""
                foo
                    >> bar <<
                baz
            """.trimIndent())
        }
    }
}

fun Span.capturing(block: (Printer) -> Unit): String {
    val printer = InMemoryPrinter()
    block(TeePrinter(printer) { log(it) })
    return printer.toString()
}

fun Renderer.start() = start(TraceId.NOOP, SpanId.NOOP)
