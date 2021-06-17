package koodies.tracing.rendering

import koodies.logging.ReturnValue
import koodies.test.tests
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ansiRemoved
import koodies.text.isSingleLine
import koodies.text.matchesCurlyPattern
import koodies.time.Now
import koodies.tracing.Span
import koodies.tracing.Span.State.Ended.Failed
import koodies.tracing.Span.State.Ended.Succeeded
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

    private val options: Settings = Settings()

    @Test
    fun Span.`should render`() {
        val rendered = capturing {
            OneLineRenderer("One Two Three", options.copy(
                contentFormatter = { it.ansi.underline },
                decorationFormatter = { it.ansi.brightMagenta },
            ), it).run {
                start()

                log(ansi11)
                nestedRenderer("child").apply {
                    start()
                    exception(RuntimeException("Now Panic!"))
                    log(plain11)
                    end(Succeeded(object : ReturnValue {
                        override val successful: Boolean? = null
                    }, Now.instant))
                }

                end(Succeeded("Done", Now.instant))
            }
        }
        expectThat(rendered)
            .matchesCurlyPattern("❰❰ One Two Three ❱ 123 abc ❱  child » RuntimeException: Now Panic! at.(OneLineRendererTest.kt:{}) » 123 abc » ⏳️  ❱ ✔︎ ❱❱")
    }

    @TestFactory
    fun Span.`should only render on end`() = tests {
        expecting { capturing { OneLineRenderer("name", options, it).start() } } that { isEmpty() }
        expecting { capturing { OneLineRenderer("name", options, it).log("event") } } that { isEmpty() }
        expecting { capturing { OneLineRenderer("name", options, it).exception(RuntimeException("exception")) } } that { isEmpty() }
        expecting {
            capturing {
                OneLineRenderer("name", options, it).apply {
                    start()
                    end(Succeeded("success", Now.instant))
                }
            }
        } that { ansiRemoved.isEqualTo("❰❰ name ❱ ✔︎ ❱❱") }
    }

    @Test
    fun Span.`should ignore non-primary attributes`() {
        val rendered = capturing {
            OneLineRenderer("name", options, it).run {
                start()
                log("event", "key" to "value")
                exception(RuntimeException("exception"), "key" to "value")
                end(Succeeded("success", Now.instant))
            }
        }
        expectThat(rendered) {
            not { contains("key") }
            not { contains("value") }
        }
    }

    @Test
    fun Span.`should render event`() {
        val rendered = capturing {
            OneLineRenderer("name", options, it).run {
                start()
                log("event", "key" to "value")
                end(Succeeded("success", Now.instant))
            }
        }
        expectThat(rendered).ansiRemoved.contains("❱ event ❱")
    }

    @Test
    fun Span.`should render exception`() {
        val rendered = capturing {
            OneLineRenderer("name", options, it).run {
                start()
                exception(RuntimeException("exception"), "key" to "value")
                end(Succeeded("success", Now.instant))
            }
        }
        expectThat(rendered).matchesCurlyPattern("❰❰ name ❱ RuntimeException: exception at.(OneLineRendererTest.kt:{}) ❱ ✔︎ ❱❱")
    }

    @Test
    fun Span.`should render success`() {
        val rendered = capturing {
            OneLineRenderer("name", options, it).run {
                start()
                end(Succeeded("success", Now.instant))
            }
        }
        expectThat(rendered).ansiRemoved.endsWith("❱ ✔︎ ❱❱")
    }

    @Test
    fun Span.`should render failure`() {
        val rendered = capturing {
            OneLineRenderer("name", options, it).run {
                start()
                end(Failed(RuntimeException("test"), Now.instant))
            }
        }
        expectThat(rendered).matchesCurlyPattern("❰❰ name ϟ RuntimeException: test at.(OneLineRendererTest.kt:{}) ❱❱")
    }

    @Test
    fun Span.`should not render event without primary attribute`() {
        val rendered = capturing {
            OneLineRenderer("name", options, it).apply {
                start()
                event("unknown", emptyMap())
                end(Succeeded("success", Now.instant))
            }
        }
        expectThat(rendered).matchesCurlyPattern("❰❰ name ❱ ✔︎ ❱❱")
    }

    @Test
    fun Span.`should render as single line`() {
        val rendered = capturing {
            OneLineRenderer("name", options, it).run {
                start()
                log("event\n2nd line")
                exception(RuntimeException("exception\n2nd line"))
                end(Succeeded("success\n2nd line", Now.instant))
            }
        }
        expectThat(rendered) {
            isSingleLine()
            contains("event⏎2nd line")
        }
    }

    @Test
    fun Span.`should be nestable`() {
        val rendered = capturing {
            OneLineRenderer("name", options, it).run {
                start()
                log("event")
                nestedRenderer("child").apply {
                    start()
                    log("child event")
                    end(Succeeded("success", Now.instant))
                }
                end(Succeeded("success", Now.instant))
            }
        }
        expectThat(rendered).matchesCurlyPattern("❰❰ name ❱ event ❱  child » child event » ✔︎  ❱ ✔︎ ❱❱")
    }

    @Test
    fun Span.`should be customizable`() {
        val rendered = capturing {
            OneLineRenderer("name", options, it).apply {
                start()
                log("foo")
                nestedRenderer("child") { copy(contentFormatter = { "!$it!" }) }.apply {
                    start()
                    log("bar")
                    end(Succeeded("success", Now.instant))
                }
                log("baz")
                end(Succeeded("success", Now.instant))
            }
        }
        expectThat(rendered).contains("!bar!")
    }

    @Test
    fun Span.`should inherit customizations`() {
        val rendered = capturing {
            OneLineRenderer("name", options.copy(contentFormatter = { "!$it!" }), it).apply {
                start()
                log("foo")
                nestedRenderer("child").apply {
                    start()
                    log("bar")
                    end(Succeeded("success", Now.instant))
                }
                log("baz")
                end(Succeeded("success", Now.instant))
            }
        }
        expectThat(rendered) {
            contains("!foo!")
            contains("!bar!")
            contains("!baz!")
        }
    }
}
