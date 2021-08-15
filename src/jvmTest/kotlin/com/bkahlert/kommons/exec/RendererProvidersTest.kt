package com.bkahlert.kommons.exec

import com.bkahlert.kommons.shell.ShellScript
import com.bkahlert.kommons.test.tests
import com.bkahlert.kommons.text.ANSI.FilteringFormatter
import com.bkahlert.kommons.text.ANSI.FilteringFormatter.Companion.fromScratch
import com.bkahlert.kommons.text.ANSI.Formatter
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.matchesCurlyPattern
import com.bkahlert.kommons.tracing.TestSpanScope
import com.bkahlert.kommons.tracing.rendering.Printer
import com.bkahlert.kommons.tracing.rendering.Styles.Solid
import com.bkahlert.kommons.tracing.rendering.TeePrinter
import com.bkahlert.kommons.tracing.rendering.capturing
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory

class RendererProvidersTest {

    private fun TestSpanScope.testLogOfSyncAndAsyncExec(
        expectedCurlyPattern: String,
        executable: Executable<Exec>,
        invocation: Executor<out Exec>.(Printer) -> Exec,
    ): List<DynamicNode> = tests {
        expecting { capturing { executable.exec.invocation(it) } } that { matchesCurlyPattern(expectedCurlyPattern) }
        expecting { capturing { executable.exec.async.invocation(it).waitFor() } } that { matchesCurlyPattern(expectedCurlyPattern) }
    }

    @Nested
    inner class BlockLog {

        @TestFactory
        fun TestSpanScope.`should format multiple messages`() = testLogOfSyncAndAsyncExec("""
                ╭──╴{}
                │
                │   Countdown!
                │   10
                │   9
                │   8
                │   7
                │   6
                │   5
                │   4
                │   3
                │   2
                │   1
                │   0
                │   Take Off
                │
                ╰──╴✔︎
            """.trimIndent(), countDownAndStart()) {
            logging(renderer = RendererProviders.block {
                copy(
                    contentFormatter = fromScratch { random },
                    decorationFormatter = Formatter.fromScratch { brightYellow },
                    style = Solid,
                    printer = TeePrinter(printer, it),
                )
            })
        }

        @TestFactory
        fun TestSpanScope.`should format immediate result`() = testLogOfSyncAndAsyncExec("""
                ╭──╴{}
                │
                │   Take Off
                │
                ╰──╴✔︎
            """.trimIndent(), justStart()) {
            logging(renderer = RendererProviders.block {
                copy(
                    contentFormatter = fromScratch { random },
                    decorationFormatter = Formatter.fromScratch { brightYellow },
                    style = Solid,
                    printer = TeePrinter(printer, it),
                )
            })
        }
    }

    @Nested
    inner class OneLineLog {

        private val formatter: FilteringFormatter<CharSequence> = FilteringFormatter { it.ansi.inverse.magenta }

        @TestFactory
        fun TestSpanScope.`should compact log`() = testLogOfSyncAndAsyncExec("""
                ╶──╴{}╶─╴Countdown!╶─╴10╶─╴9╶─╴8╶─╴7╶─╴6╶─╴5╶─╴4╶─╴3╶─╴2╶─╴1╶─╴0╶─╴Take Off ✔︎
            """.trimIndent(), countDownAndStart()) {
            logging(renderer = RendererProviders.oneLine {
                copy(
                    contentFormatter = formatter,
                    printer = TeePrinter(printer, it),
                )
            })
        }

        @TestFactory
        fun TestSpanScope.`should format immediate result`() = testLogOfSyncAndAsyncExec("""
                ╶──╴{}╶─╴Take Off ✔︎
            """.trimIndent(), justStart()) {
            logging(renderer = RendererProviders.oneLine {
                copy(
                    contentFormatter = formatter,
                    printer = TeePrinter(printer, it),
                )
            })
        }
    }


    @Nested
    inner class CompactLog {

        @TestFactory
        fun TestSpanScope.`should format multiple messages`() = testLogOfSyncAndAsyncExec("""
                ╭──╴{}
                │
                │   Countdown!
                │   10
                │   9
                │   8
                │   7
                │   6
                │   5
                │   4
                │   3
                │   2
                │   1
                │   0
                │   Take Off
                │
                ╰──╴✔︎
            """.trimIndent(), countDownAndStart()) {
            logging(renderer = RendererProviders.compact {
                copy(
                    contentFormatter = fromScratch { random },
                    decorationFormatter = Formatter.fromScratch { brightYellow },
                    style = Solid,
                    printer = TeePrinter(printer, it),
                )
            })
        }

        @TestFactory
        fun TestSpanScope.`should format immediate result`() = testLogOfSyncAndAsyncExec("""
                ╭──╴{}
                │
                │   Take Off
                │
                ╰──╴✔︎
            """.trimIndent(), justStart()) {
            logging(renderer = RendererProviders.compact {
                copy(
                    contentFormatter = fromScratch { random },
                    decorationFormatter = Formatter.fromScratch { brightYellow },
                    style = Solid,
                    printer = TeePrinter(printer, it),
                )
            })
        }
    }

    @Nested
    inner class SummaryLog {

        @TestFactory
        fun TestSpanScope.`should format multiple messages`() = testLogOfSyncAndAsyncExec("""
                ╶──╴{}╶─╴Countdown!╶─╴10╶─╴9╶─╴8╶─╴7╶─╴6╶─╴5╶─╴4╶─╴3╶─╴2╶─╴1╶─╴0╶─╴Take Off ✔︎
                """.trimIndent(), countDownAndStart()) {
            logging(renderer = RendererProviders.summary {
                copy(printer = TeePrinter(printer, it))
            })
        }

        @TestFactory
        fun TestSpanScope.`should format immediate result`() = testLogOfSyncAndAsyncExec("""
                ╶──╴{}╶─╴Take Off ✔︎
            """.trimIndent(), justStart()) {
            logging(renderer = RendererProviders.summary {
                copy(printer = TeePrinter(printer, it))
            })
        }
    }

    @Nested
    inner class NoDetailsLog {

        @TestFactory
        fun TestSpanScope.`should format multiple messages`() = testLogOfSyncAndAsyncExec("""
                {} ✔︎
            """.trimIndent(), countDownAndStart()) {
            logging(renderer = RendererProviders.noDetails {
                copy(printer = TeePrinter(printer, it))
            })
        }

        @TestFactory
        fun TestSpanScope.`should format immediate result`() = testLogOfSyncAndAsyncExec("""
                {} ✔︎
            """.trimIndent(), justStart()) {
            logging(renderer = RendererProviders.noDetails {
                copy(printer = TeePrinter(printer, it))
            })
        }
    }

    @Nested
    inner class ErrorsOnlyLog {

        @TestFactory
        fun TestSpanScope.`should be empty if no error occurs`() = testLogOfSyncAndAsyncExec("", countDownAndStart()) {
            logging(renderer = RendererProviders.errorsOnly {
                copy(printer = TeePrinter(printer, it))
            })
        }

        @TestFactory
        fun TestSpanScope.`should display ERR`() = testLogOfSyncAndAsyncExec("""
                ϟ Process {} terminated with exit code {}
                ➜ A dump has been written to:
                  - file://{}
                  - file://{}
                ➜ The last 10 lines are:
                {{}}
            """.trimIndent(), countDownAndBoom()) {
            logging(renderer = RendererProviders.errorsOnly {
                copy(printer = TeePrinter(printer, it))
            })
        }

        @TestFactory
        fun TestSpanScope.`should hide regular result`() = testLogOfSyncAndAsyncExec("", countDownAndStart()) {
            logging(renderer = RendererProviders.errorsOnly {
                copy(printer = TeePrinter(printer, it))
            })
        }
    }

    private fun countDownAndStart() = ShellScript {
        echo("Countdown!")
        (10 downTo 0).forEach { echo(it) }
        echo("Take Off")
    }

    private fun countDownAndBoom() = ShellScript {
        echo("Countdown!")
        (10 downTo 5).forEach { echo(it) }
        !">&2 echo '4'"
        (3 downTo 0).forEach { echo(it) }
        exit(1u)
    }

    private fun justStart() = ShellScript {
        echo("Take Off")
    }
}
