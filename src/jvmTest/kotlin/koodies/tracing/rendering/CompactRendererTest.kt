package koodies.tracing.rendering

import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.LineSeparators.LF
import koodies.text.matchesCurlyPattern
import koodies.tracing.TestSpan
import koodies.tracing.rendering.RenderableAttributes.Companion.EMPTY
import koodies.tracing.rendering.Renderer.Companion.log
import org.junit.jupiter.api.Test
import strikt.api.expectThat

class CompactRendererTest {

    private val plain11 = "123 abc"
    private val ansi11 = "123".ansi.yellow.done + " " + "abc".ansi.green

    private val settings: Settings = Settings()

    @Test
    fun TestSpan.`should render`() {
        val rendered = capturing {
            CompactRenderer(settings.copy(
                contentFormatter = { it.ansi.underline },
                decorationFormatter = { it.ansi.brightMagenta },
                printer = it,
            )).run {
                start("One Two Three")

                log(ansi11)
                childRenderer().apply {
                    start("one-liner")
                    end(Result.failure<Unit>(RuntimeException("message")))
                }
                childRenderer().apply {
                    start("block")
                    log(plain11)
                    end(Result.failure<Unit>(RuntimeException("message")))
                }

                end(Result.success(true))
            }
        }
        expectThat(rendered).matchesCurlyPattern("""
            ╭──╴One Two Three
            │
            │   123 abc                                                                         
            │   one-liner ϟ RuntimeException: message at.(CompactRendererTest.kt:{})
            │   ╭──╴block
            │   │
            │   │   123 abc                                                                     
            │   ϟ
            │   ╰──╴RuntimeException: message at.(CompactRendererTest.kt:{})
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Test
    fun TestSpan.`should render one-line on immediate end`() {
        val rendered = capturing {
            CompactRenderer(settings.copy(printer = it)).run {
                start("name")
                end(Result.success(true))
            }
        }
        expectThat(rendered).matchesCurlyPattern("""
            name ✔︎
        """.trimIndent())
    }

    @Test
    fun TestSpan.`should render one-line on immediate nested end`() {
        val rendered = capturing {
            CompactRenderer(settings.copy(printer = it)).run {
                start("parent")
                childRenderer().run {
                    start("child")
                    end(Result.success(true))
                }
                end(Result.success(true))
            }
        }
        expectThat(rendered).matchesCurlyPattern("""
            parent ❱❱ child ✔︎ ❱❱ ✔︎
        """.trimIndent())
    }

    @Test
    fun TestSpan.`should render block on delayed nested end`() {
        val rendered = capturing {
            CompactRenderer(settings.copy(printer = it)).run {
                start("parent")
                childRenderer().run {
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
    fun TestSpan.`should render block on immediate nested multi-line end`() {
        val rendered = capturing {
            CompactRenderer(settings.copy(printer = it)).run {
                start("parent")
                childRenderer().run {
                    start("child")
                    end(Result.success(object : ReturnValue {
                        override val successful: Boolean = false
                        override val textRepresentation: String = "line 1${LF}line2"
                    }))
                }
                end(Result.success(true))
            }
        }
        expectThat(rendered).matchesCurlyPattern("""
            ╭──╴parent
            │
            │   ╭──╴child
            │   │
            │   ϟ
            │   ╰──╴line 1
            │   line2
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Test
    fun TestSpan.`should render block on delayed nested multi-line end`() {
        val rendered = capturing {
            CompactRenderer(settings.copy(printer = it)).run {
                start("parent")
                childRenderer().run {
                    start("child")
                    log("delay")
                    end(Result.success(object : ReturnValue {
                        override val successful: Boolean = false
                        override val textRepresentation: String = "line 1${LF}line2"
                    }))
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
            │   ϟ
            │   ╰──╴line 1
            │   line2
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Test
    fun TestSpan.`should render block on event`() {
        val rendered = capturing {
            CompactRenderer(settings.copy(printer = it)).run {
                start("name")
                log("event")
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
            CompactRenderer(settings.copy(printer = it)).run {
                start("name")
                exception(RuntimeException("exception"), EMPTY)
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
            CompactRenderer(settings.copy(printer = it)).run {
                start("parent")
                childRenderer { OneLineRenderer(this) }.run {
                    start("child")
                    end(Result.success(true))
                }
                end(Result.success(true))
            }
        }
        expectThat(rendered).matchesCurlyPattern("""
            ╭──╴parent
            │
            │   child ✔︎
            │
            ╰──╴✔︎
        """.trimIndent())
    }
}
