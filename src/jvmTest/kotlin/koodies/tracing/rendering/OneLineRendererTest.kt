package koodies.tracing.rendering

import koodies.test.tests
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ansiRemoved
import koodies.text.isSingleLine
import koodies.text.matchesCurlyPattern
import koodies.tracing.TestSpan
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.endsWith
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo

class OneLineRendererTest {

    private val plain11 = "123 abc"
    private val ansi11 = "123".ansi.yellow.done + " " + "abc".ansi.green

    private val settings: Settings = Settings()

    @Test
    fun TestSpan.`should render`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(
                contentFormatter = { it.toString().ansi.underline },
                decorationFormatter = { it.toString().ansi.brightMagenta },
                printer = it,
            )).run {
                start("One Two Three")

                log(ansi11)
                nestedRenderer().apply {
                    start("child")
                    exception(RuntimeException("Now Panic!"))
                    log(plain11)
                    end(Result.failure<Unit>(RuntimeException("message")))
                }

                end(Result.success(true))
            }
        }
        expectThat(rendered)
            .matchesCurlyPattern("One Two Three ❱ 123 abc ❱❱ child ❱ RuntimeException: Now Panic! at.({}.kt:{}) ❱ 123 abc ϟ RuntimeException: message at.({}.kt:{}) ❱❱ ✔︎")
    }

    @TestFactory
    fun TestSpan.`should only render on end`() = tests {
        expecting { capturing { OneLineRenderer(settings.copy(printer = it)).start("name") } } that { isEmpty() }
        expecting { capturing { OneLineRenderer(settings.copy(printer = it)).log("event") } } that { isEmpty() }
        expecting { capturing { OneLineRenderer(settings.copy(printer = it)).exception(RuntimeException("exception")) } } that { isEmpty() }
        expecting {
            capturing {
                OneLineRenderer(settings.copy(printer = it)).apply {
                    start("name")
                    end(Result.success(true))
                }
            }
        } that { ansiRemoved.isEqualTo("name ✔︎") }
    }

    @Test
    fun TestSpan.`should ignore non-primary attributes`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(printer = it)).run {
                start("name")
                log("event", "key" to "value")
                exception(RuntimeException("exception"), "key" to "value")
                end(Result.success(true))
            }
        }
        expectThat(rendered) {
            not { contains("key") }
            not { contains("value") }
        }
    }

    @Test
    fun TestSpan.`should render event`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(printer = it)).run {
                start("name")
                log("event", "key" to "value")
                end(Result.success(true))
            }
        }
        expectThat(rendered).ansiRemoved.contains("❱ event")
    }

    @Test
    fun TestSpan.`should render exception`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(printer = it)).run {
                start("name")
                exception(RuntimeException("exception"), "key" to "value")
                end(Result.success(true))
            }
        }
        expectThat(rendered).matchesCurlyPattern("name ❱ RuntimeException: exception at.(OneLineRendererTest.kt:{}) ✔︎")
    }

    @Test
    fun TestSpan.`should render success`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(printer = it)).run {
                start("name")
                end(Result.success(true))
            }
        }
        expectThat(rendered).ansiRemoved.endsWith("name ✔︎")
    }

    @Test
    fun TestSpan.`should render failure`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(printer = it)).run {
                start("name")
                end(Result.failure<Unit>(RuntimeException("test")))
            }
        }
        expectThat(rendered).matchesCurlyPattern("name ϟ RuntimeException: test at.(OneLineRendererTest.kt:{})")
    }

    @Test
    fun TestSpan.`should not render event without primary attribute`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(printer = it)).apply {
                start("name")
                event("unknown", emptyMap())
                end(Result.success(true))
            }
        }
        expectThat(rendered).matchesCurlyPattern("name ✔︎")
    }

    @Test
    fun TestSpan.`should render as single line`() {
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
    fun TestSpan.`should be nestable`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(printer = it)).run {
                start("name")
                log("event")
                nestedRenderer().apply {
                    start("child")
                    log("child event")
                    end(Result.success(true))
                }
                end(Result.success(true))
            }
        }
        expectThat(rendered).matchesCurlyPattern("name ❱ event ❱❱ child ❱ child event ✔︎ ❱❱ ✔︎")
    }

    @Test
    fun TestSpan.`should be customizable`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(printer = it)).apply {
                start("name")
                log("foo")
                nestedRenderer { it(copy(contentFormatter = { "!$it!" })) }.apply {
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
    fun TestSpan.`should inherit customizations`() {
        val rendered = capturing {
            OneLineRenderer(settings.copy(contentFormatter = { "!$it!" }, printer = it)).apply {
                start("name")
                log("foo")
                nestedRenderer().apply {
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
