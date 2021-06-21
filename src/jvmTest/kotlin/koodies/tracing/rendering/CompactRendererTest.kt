package koodies.tracing.rendering

import koodies.logging.ReturnValue
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.matchesCurlyPattern
import koodies.tracing.TestSpan
import org.junit.jupiter.api.Test
import strikt.api.expectThat

class CompactRendererTest {

    private val plain11 = "123 abc"
    private val ansi11 = "123".ansi.yellow.done + " " + "abc".ansi.green

    private val options: Settings = Settings()

    @Test
    fun TestSpan.`should render`() {
        val rendered = capturing {
            CompactRenderer(options.copy(
                contentFormatter = { it.toString().ansi.underline },
                decorationFormatter = { it.toString().ansi.brightMagenta },
            ), it).run {
                start("One Two Three")

                log(ansi11)
                customizedChild().apply {
                    start("one-liner")
                    end(Result.failure<Unit>(RuntimeException("message")))
                }
                customizedChild().apply {
                    start("block")
                    log(plain11)
                    end(Result.success(object : ReturnValue {
                        override val successful: Boolean? = null
                    }))
                }

                end(Result.success(true))
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
    fun TestSpan.`should render one-line on immediate end`() {
        val rendered = capturing {
            CompactRenderer(options, it).run {
                start("name")
                end(Result.success(true))
            }
        }
        expectThat(rendered).matchesCurlyPattern("""
            ❰❰ name ❱ ✔︎ ❱❱
        """.trimIndent())
    }

    @Test
    fun TestSpan.`should render one-line on immediate nested end`() {
        val rendered = capturing {
            CompactRenderer(options, it).run {
                start("parent")
                customizedChild().run {
                    start("child")
                    end(Result.success(true))
                }
                end(Result.success(true))
            }
        }
        expectThat(rendered).matchesCurlyPattern("""
            ❰❰ parent ❱  child » ✔︎  ❱ ✔︎ ❱❱
        """.trimIndent())
    }

    @Test
    fun TestSpan.`should render block on delayed nested end`() {
        val rendered = capturing {
            CompactRenderer(options, it).run {
                start("parent")
                customizedChild().run {
                    start("child")
                    log("delay")
                    end(Result.success(true))
                }
                end(Result.success(true))
            }
        }
        expectThat(rendered).matchesCurlyPattern("""
            ╭──╴parent
            │
            │   ╭──╴child
            │   │
            │   │   delay                                    
            │   │
            │   ╰──╴✔︎
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Test
    fun TestSpan.`should render block on event`() {
        val rendered = capturing {
            CompactRenderer(options, it).run {
                start("name")
                log("event", "key" to "value")
                end(Result.success(true))
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
    fun TestSpan.`should render block on exception`() {
        val rendered = capturing {
            CompactRenderer(options, it).run {
                start("name")
                exception(RuntimeException("exception"), "key" to "value")
                end(Result.success(true))
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

    @Test
    fun TestSpan.`should render block on injected child`() {
        val rendered = capturing {
            CompactRenderer(options, it).run {
                start("parent")
                injectedChild { settings, printer -> OneLineRenderer(settings, printer) }.run {
                    start("child")
                    end(Result.success(true))
                }
                end(Result.success(true))
            }
        }
        expectThat(rendered).matchesCurlyPattern("""
            ╭──╴parent
            │
            │   ❰❰ child ❱ ✔︎ ❱❱
            │
            ╰──╴✔︎
        """.trimIndent())
    }
}
