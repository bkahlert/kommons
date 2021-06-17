package koodies.tracing.rendering

import koodies.logging.ReturnValue
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.matchesCurlyPattern
import koodies.time.Now
import koodies.tracing.Span
import koodies.tracing.Span.State.Ended.Failed
import koodies.tracing.Span.State.Ended.Succeeded
import org.junit.jupiter.api.Test
import strikt.api.expectThat

class CompactRendererTest {

    private val plain11 = "123 abc"
    private val ansi11 = "123".ansi.yellow.done + " " + "abc".ansi.green

    private val options: Settings = Settings()

    @Test
    fun Span.`should render`() {
        val rendered = capturing {
            CompactRenderer("One Two Three", options.copy(
                contentFormatter = { it.ansi.underline },
                decorationFormatter = { it.ansi.brightMagenta },
            ), it).run {
                start()

                log(ansi11)
                nestedRenderer("one-liner").apply {
                    start()
                    end(Failed(RuntimeException("message"), Now.instant))
                }
                nestedRenderer("block").apply {
                    start()
                    log(plain11)
                    end(Succeeded(object : ReturnValue {
                        override val successful: Boolean? = null
                    }, Now.instant))
                }

                end(Succeeded("Done", Now.instant))
            }
        }
        expectThat(rendered).matchesCurlyPattern("""
            ╭──╴One Two Three
            │
            │   123 abc                                                                         
            │   ❰❰ one-liner ϟ RuntimeException: message at.(CompactRendererTest.kt:{}) ❱❱
            │   ╭──╴block
            │   │
            │   │   123 abc                                                                     
            │   ╵
            │   ╵
            │   ⏳️
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Test
    fun Span.`should render one-line on immediate end`() {
        val rendered = capturing {
            CompactRenderer("name", options, it).run {
                start()
                end(Succeeded("success", Now.instant))
            }
        }
        expectThat(rendered).matchesCurlyPattern("""
            ❰❰ name ❱ ✔︎ ❱❱
        """.trimIndent())
    }

    @Test
    fun Span.`should render block on event`() {
        val rendered = capturing {
            CompactRenderer("name", options, it).run {
                start()
                log("event", "key" to "value")
                end(Succeeded("success", Now.instant))
            }
        }
        expectThat(rendered).matchesCurlyPattern("""
            ╭──╴name
            │
            │   event
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Test
    fun Span.`should render block on exception`() {
        val rendered = capturing {
            CompactRenderer("name", options, it).run {
                start()
                exception(RuntimeException("exception"), "key" to "value")
                end(Succeeded("success", Now.instant))
            }
        }
        expectThat(rendered).matchesCurlyPattern("""
            ╭──╴name
            │
            │   java.lang.RuntimeException: exception
            {{}}
            ╰──╴✔︎
        """.trimIndent())
    }
}
