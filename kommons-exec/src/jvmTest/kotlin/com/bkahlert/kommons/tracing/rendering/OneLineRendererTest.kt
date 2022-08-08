package com.bkahlert.kommons.tracing.rendering

import com.bkahlert.kommons.exec.ExecAttributes
import com.bkahlert.kommons.test.Smoke
import com.bkahlert.kommons.test.junit.testEach
import com.bkahlert.kommons.test.junit.testing
import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.LineSeparators.isSingleLine
import com.bkahlert.kommons.text.ansiRemoved
import com.bkahlert.kommons.text.removeAnsi
import com.bkahlert.kommons.tracing.Key
import com.bkahlert.kommons.tracing.TestSpanScope
import com.bkahlert.kommons.tracing.rendering.ColumnsLayout.Companion.columns
import com.bkahlert.kommons.tracing.rendering.RenderableAttributes.Companion.EMPTY
import com.bkahlert.kommons.tracing.rendering.Renderer.Companion.log
import com.bkahlert.kommons.tracing.rendering.Styles.Dotted
import com.bkahlert.kommons.tracing.rendering.Styles.None
import com.bkahlert.kommons.tracing.rendering.Styles.Solid
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.contains

class OneLineRendererTest {

    private val EXTRA: Key<String, Any> = Key.stringKey("kommons.extra") { it.toString() }

    private val plain11 = "123 abc"
    private val ansi11 = "123".ansi.yellow.done + " " + "abc".ansi.green

    private val settings: Settings = Settings()

    @Disabled
    @Smoke @TestFactory
    fun TestSpanScope.`should render using styles`() = testEach(
        Solid to """
            ╶──╴One Two Three╶─╴123 ABC ╶──╴child-span╶─╴123 ABC ╶──╴child-span╶─╴123 ABC ϟ RuntimeException: Now Panic! at.(OneLineRendererTest.kt:*) ϟ RuntimeException: message at.(OneLineRendererTest.kt:*) ✔︎
        """.trimIndent(),
        Dotted to """
            ▶ One Two Three ▷ 123 ABC ▶ child-span ▷ 123 ABC ▶ child-span ▷ 123 ABC ϟ RuntimeException: Now Panic! at.(OneLineRendererTest.kt:*) ϟ RuntimeException: message at.(OneLineRendererTest.kt:*) ✔︎
        """.trimIndent(),
        None to """
            One Two Three ❱ 123 ABC child-span ❱ 123 ABC child-span ❱ 123 ABC ϟ RuntimeException: Now Panic! at.(OneLineRendererTest.kt:*) ϟ RuntimeException: message at.(OneLineRendererTest.kt:*) ✔︎
        """.trimIndent(),
    ) { (style, expected) ->
        val rendered = capturing { printer ->
            OneLineRenderer(
                Settings(
                    style = style,
                    layout = ColumnsLayout(RenderingAttributes.DESCRIPTION columns 40, EXTRA columns 20, maxColumns = 80),
                    contentFormatter = { it.toString().uppercase().ansi.random },
                    decorationFormatter = { it.ansi.brightRed },
                    returnValueTransform = { it },
                    printer = printer,
                )
            ).apply {

                start("One Two Three")
                log(ansi11, EXTRA to plain11)
                childRenderer().apply {
                    start("child-span")
                    log(ansi11, EXTRA to plain11)
                    childRenderer().apply {
                        start("child-span")
                        log(ansi11, EXTRA to plain11)
                        end(Result.failure<Unit>(RuntimeException("Now Panic!")))
                    }
                    end(Result.failure<Unit>(RuntimeException("message")))
                }

                end(Result.success(true))
            }
        }
        rendered shouldMatchGlob expected
    }

    @TestFactory
    fun TestSpanScope.`should only render on end`() = testing {
        expecting { capturing { OneLineRenderer(settings.copy(printer = it)).start("name") } } that { it.shouldBeEmpty() }
        expecting { capturing { OneLineRenderer(settings.copy(printer = it)).log("event") } } that { it.shouldBeEmpty() }
        expecting { capturing { OneLineRenderer(settings.copy(printer = it)).exception(RuntimeException("exception"), EMPTY) } } that { it.shouldBeEmpty() }
        expecting {
            capturing {
                OneLineRenderer(settings.copy(printer = it)).apply {
                    start("name")
                    end(Result.success(true))
                }
            }
        } that { it.ansiRemoved shouldBe "╶──╴name ✔︎" }
    }

    @Test
    fun TestSpanScope.`should ignore non-primary attributes`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(printer = it)).run {
                start("name")
                log("event", ExecAttributes.NAME to "value")
                exception(RuntimeException("exception"), ExecAttributes.NAME to "value")
                end(Result.success(true))
            }
        }
        expectThat(rendered) {
            not { contains("key") }
            not { contains("value") }
        }
    }

    @Test
    fun TestSpanScope.`should render event`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(printer = it)).run {
                start("name")
                log("event")
                end(Result.success(true))
            }
        }
        expectThat(rendered).removeAnsi.contains("╶─╴event")
    }

    @Test
    fun TestSpanScope.`should render exception`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(printer = it)).run {
                start("name")
                exception(RuntimeException("exception"), EMPTY)
                end(Result.success(true))
            }
        }
        rendered.ansiRemoved shouldMatchGlob "╶──╴name╶─╴RuntimeException: exception at.(OneLineRendererTest.kt:*) ✔︎"
    }

    @Test
    fun TestSpanScope.`should render success`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(printer = it)).run {
                start("name")
                end(Result.success(true))
            }
        }
        rendered.ansiRemoved shouldEndWith "╶──╴name ✔︎"
    }

    @Test
    fun TestSpanScope.`should render failure`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(printer = it)).run {
                start("name")
                end(Result.failure<Unit>(RuntimeException("test")))
            }
        }
        rendered.ansiRemoved shouldMatchGlob "╶──╴name ϟ RuntimeException: test at.(OneLineRendererTest.kt:*)"
    }

    @Test
    fun TestSpanScope.`should not render event without primary attribute`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(printer = it)).apply {
                start("name")
                event("unknown")
                end(Result.success(true))
            }
        }
        rendered.ansiRemoved shouldMatchGlob "╶──╴name ✔︎"
    }

    @Test
    fun TestSpanScope.`should render as single line`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(printer = it)).run {
                start("name")
                log("event\n2nd line")
                exception(RuntimeException("exception\n2nd line"))
                end(Result.success("success\n2nd line"))
            }
        }
        rendered should {
            it.isSingleLine() shouldBe true
            it.shouldContain("event⏎2nd line")
        }
    }

    @Test
    fun TestSpanScope.`should be nestable`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(printer = it)).run {
                start("name")
                log("event")
                childRenderer().apply {
                    start("child")
                    log("child event")
                    end(Result.success(true))
                }
                end(Result.success(true))
            }
        }
        rendered.ansiRemoved shouldMatchGlob "╶──╴name╶─╴event ╶──╴child╶─╴child event ✔︎ ✔︎"
    }

    @Test
    fun TestSpanScope.`should be customizable`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(printer = it)).apply {
                start("name")
                log("foo")
                childRenderer { it.create(copy(contentFormatter = { "!$it!" })) }.apply {
                    start("child")
                    log("bar")
                    end(Result.success(true))
                }
                log("baz")
                end(Result.success(true))
            }
        }
        rendered shouldContain "!bar!"
    }

    @Test
    fun TestSpanScope.`should inherit customizations`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(contentFormatter = { "!$it!" }, printer = it)).apply {
                start("name")
                log("foo")
                childRenderer().apply {
                    start("child")
                    log("bar")
                    end(Result.success(true))
                }
                log("baz")
                end(Result.success(true))
            }
        }
        rendered should {
            it shouldContain "!foo!"
            it shouldContain "!bar!"
            it shouldContain "!baz!"
        }
    }
}
