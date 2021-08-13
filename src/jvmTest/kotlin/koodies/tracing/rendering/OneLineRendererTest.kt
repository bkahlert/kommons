package koodies.tracing.rendering

import koodies.exec.ExecAttributes
import koodies.test.Smoke
import koodies.test.testEach
import koodies.test.tests
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ansiRemoved
import koodies.text.isSingleLine
import koodies.text.matchesCurlyPattern
import koodies.text.toUpperCase
import koodies.tracing.Key
import koodies.tracing.TestSpanScope
import koodies.tracing.rendering.ColumnsLayout.Companion.columns
import koodies.tracing.rendering.RenderableAttributes.Companion.EMPTY
import koodies.tracing.rendering.Renderer.Companion.log
import koodies.tracing.rendering.Styles.Dotted
import koodies.tracing.rendering.Styles.None
import koodies.tracing.rendering.Styles.Solid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.endsWith
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo

class OneLineRendererTest {

    private val EXTRA: Key<String, Any> = Key.stringKey("koodies.extra") { it.toString() }

    private val plain11 = "123 abc"
    private val ansi11 = "123".ansi.yellow.done + " " + "abc".ansi.green

    private val settings: Settings = Settings()

    @Smoke @TestFactory
    fun TestSpanScope.`should render using styles`() = testEach(
        Solid to """
            ╶──╴One Two Three╶─╴123 ABC ╶──╴child-span╶─╴123 ABC ╶──╴child-span╶─╴123 ABC ϟ RuntimeException: Now Panic! at.(OneLineRendererTest.kt:{}) ϟ RuntimeException: message at.(OneLineRendererTest.kt:{}) ✔︎
        """.trimIndent(),
        Dotted to """
            ▶ One Two Three ▷ 123 ABC ▶ child-span ▷ 123 ABC ▶ child-span ▷ 123 ABC ϟ RuntimeException: Now Panic! at.(OneLineRendererTest.kt:{}) ϟ RuntimeException: message at.(OneLineRendererTest.kt:{}) ✔︎
        """.trimIndent(),
        None to """
            One Two Three ❱ 123 ABC child-span ❱ 123 ABC child-span ❱ 123 ABC ϟ RuntimeException: Now Panic! at.(OneLineRendererTest.kt:{}) ϟ RuntimeException: message at.(OneLineRendererTest.kt:{}) ✔︎
        """.trimIndent(),
    ) { (style, expected) ->
        val rendered = capturing { printer ->
            OneLineRenderer(Settings(
                style = style,
                layout = ColumnsLayout(RenderingAttributes.DESCRIPTION columns 40, EXTRA columns 20, maxColumns = 80),
                contentFormatter = { it.toString().toUpperCase().ansi.random },
                decorationFormatter = { it.ansi.brightRed },
                returnValueTransform = { it },
                printer = printer,
            )).apply {

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
        expecting { rendered } that { matchesCurlyPattern(expected) }
    }

    @TestFactory
    fun TestSpanScope.`should only render on end`() = tests {
        expecting { capturing { OneLineRenderer(settings.copy(printer = it)).start("name") } } that { isEmpty() }
        expecting { capturing { OneLineRenderer(settings.copy(printer = it)).log("event") } } that { isEmpty() }
        expecting { capturing { OneLineRenderer(settings.copy(printer = it)).exception(RuntimeException("exception"), EMPTY) } } that { isEmpty() }
        expecting {
            capturing {
                OneLineRenderer(settings.copy(printer = it)).apply {
                    start("name")
                    end(Result.success(true))
                }
            }
        } that { ansiRemoved.isEqualTo("╶──╴name ✔︎") }
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
        expectThat(rendered).ansiRemoved.contains("╶─╴event")
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
        expectThat(rendered).matchesCurlyPattern("╶──╴name╶─╴RuntimeException: exception at.(OneLineRendererTest.kt:{}) ✔︎")
    }

    @Test
    fun TestSpanScope.`should render success`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(printer = it)).run {
                start("name")
                end(Result.success(true))
            }
        }
        expectThat(rendered).ansiRemoved.endsWith("╶──╴name ✔︎")
    }

    @Test
    fun TestSpanScope.`should render failure`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(printer = it)).run {
                start("name")
                end(Result.failure<Unit>(RuntimeException("test")))
            }
        }
        expectThat(rendered).matchesCurlyPattern("╶──╴name ϟ RuntimeException: test at.(OneLineRendererTest.kt:{})")
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
        expectThat(rendered).matchesCurlyPattern("╶──╴name ✔︎")
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
        expectThat(rendered) {
            isSingleLine()
            contains("event⏎2nd line")
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
        expectThat(rendered).matchesCurlyPattern("╶──╴name╶─╴event ╶──╴child╶─╴child event ✔︎ ✔︎")
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
        expectThat(rendered).contains("!bar!")
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
        expectThat(rendered) {
            contains("!foo!")
            contains("!bar!")
            contains("!baz!")
        }
    }
}
