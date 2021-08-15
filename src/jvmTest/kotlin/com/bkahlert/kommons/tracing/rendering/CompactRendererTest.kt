package com.bkahlert.kommons.tracing.rendering

import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.matchesCurlyPattern
import com.bkahlert.kommons.tracing.TestSpanScope
import com.bkahlert.kommons.tracing.rendering.RenderableAttributes.Companion.EMPTY
import com.bkahlert.kommons.tracing.rendering.Renderer.Companion.log
import org.junit.jupiter.api.Test
import strikt.api.expectThat

class CompactRendererTest {

    private val plain11 = "123 abc"
    private val ansi11 = "123".ansi.yellow.done + " " + "abc".ansi.green

    private val settings: Settings = Settings()

    @Test
    fun TestSpanScope.`should render`() {
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
            │   ╶──╴one-liner ϟ RuntimeException: message at.(CompactRendererTest.kt:{})
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
    fun TestSpanScope.`should render one-line on immediate end`() {
        val rendered = capturing {
            CompactRenderer(settings.copy(printer = it)).run {
                start("name")
                end(Result.success(true))
            }
        }
        expectThat(rendered).matchesCurlyPattern("""
            ╶──╴name ✔︎
        """.trimIndent())
    }

    @Test
    fun TestSpanScope.`should render one-line on immediate nested end`() {
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
            ╶──╴parent ╶──╴child ✔︎ ✔︎
        """.trimIndent())
    }

    @Test
    fun TestSpanScope.`should render block on multi-line start`() {
        val rendered = capturing {
            CompactRenderer(settings.copy(printer = it)).run {
                start("parent")
                childRenderer().run {
                    start("child\nmulti-line")
                    end(Result.success(true))
                }
                end(Result.success(true))
            }
        }
        expectThat(rendered).matchesCurlyPattern("""
            ╭──╴parent
            │
            │   ╭──╴child
            │   │   multi-line
            │   │
            │   │
            │   ╰──╴✔︎
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Test
    fun TestSpanScope.`should render block on delayed nested end`() {
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
    fun TestSpanScope.`should render block on immediate nested multi-line end`() {
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
    fun TestSpanScope.`should render block on delayed nested multi-line end`() {
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
    fun TestSpanScope.`should render block on event`() {
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
    fun TestSpanScope.`should render block on exception`() {
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
    fun TestSpanScope.`should render block on injected child`() {
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
            │   ╶──╴child ✔︎
            │
            ╰──╴✔︎
        """.trimIndent())
    }
}
