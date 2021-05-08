package koodies.logging

import koodies.exec.Process.State.Exited.Failed
import koodies.exec.hasState
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
import koodies.shell.ShellScript
import koodies.text.ANSI
import koodies.text.ANSI.Formatter.Companion.fromScratch
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEmpty

class LoggingOptionsTest {

    @Nested
    inner class BlockLog {

        @Nested
        inner class Sync {

            @Test
            fun InMemoryLogger.`should format multiple messages`() {
                countDownAndStart().exec.logging(this) {
                    block {
                        caption { "caption" }
                        contentFormatter { fromScratch { random } }
                        decorationFormatter { fromScratch { brightYellow } }
                        border = SOLID
                    }
                }
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   ╭──╴caption
                    │   │
                    │   │   Executing {}
                    │   │   Countdown!
                    │   │   10
                    │   │   9
                    │   │   8
                    │   │   7
                    │   │   6
                    │   │   5
                    │   │   4
                    │   │   3
                    │   │   2
                    │   │   1
                    │   │   0
                    │   │   Take Off
                    │   │   Process {} terminated successfully at {}
                    │   │
                    │   ╰──╴✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should format immediate result`() {
                justStart().exec.logging(this) {
                    block {
                        caption { "caption" }
                        contentFormatter { fromScratch { random } }
                        decorationFormatter { fromScratch { brightYellow } }
                        border = SOLID
                    }
                }
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   ╭──╴caption
                    │   │
                    │   │   Executing {}
                    │   │   Take Off
                    │   │   Process {} terminated successfully at {}
                    │   │
                    │   ╰──╴✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }

        @Nested
        inner class Async {

            @Test
            fun InMemoryLogger.`should format multiple messages`() {
                countDownAndStart().exec.async.logging(this) {
                    block {
                        caption { "caption" }
                        contentFormatter { fromScratch { random } }
                        decorationFormatter { fromScratch { brightYellow } }
                        border = SOLID
                    }
                }.waitFor()
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   ╭──╴caption
                    │   │
                    │   ╵
                    │   ╵
                    │   ⏳️ async computation
                    │   ⏳️ Executing {}
                    │   ⏳️ Countdown!
                    │   ⏳️ 10
                    │   ⏳️ 9
                    │   ⏳️ 8
                    │   ⏳️ 7
                    │   ⏳️ 6
                    │   ⏳️ 5
                    │   ⏳️ 4
                    │   ⏳️ 3
                    │   ⏳️ 2
                    │   ⏳️ 1
                    │   ⏳️ 0
                    │   ⏳️ Take Off
                    │   ⏳️ Process {} terminated successfully at {}
                    │   ⏳️ ✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should format immediate result`() {
                justStart().exec.async.logging(this) {
                    block {
                        caption { "caption" }
                        contentFormatter { fromScratch { random } }
                        decorationFormatter { fromScratch { brightYellow } }
                        border = SOLID
                    }
                }.waitFor()
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   ╭──╴caption
                    │   │
                    │   ╵
                    │   ╵
                    │   ⏳️ async computation
                    │   ⏳️ Executing {}
                    │   ⏳️ Take Off
                    │   ⏳️ Process {} terminated successfully at {}
                    │   ⏳️ ✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }
    }

    @Nested
    inner class CompactLog {

        private val formatter: ANSI.Formatter = ANSI.Formatter { it.ansi.inverse.magenta }

        @Nested
        inner class Sync {

            @Test
            fun InMemoryLogger.`should compact log`() {
                countDownAndStart().exec.logging(this) {
                    compact {
                        caption { "caption" }
                        contentFormatter { formatter }
                    }
                }
                expectThatLogged().matchesCurlyPattern("""
                        ╭──╴{}
                        │
                        │   caption Executing {} Countdown! 10 9 8 7 6 5 4 3 2 1 0 Take Off Process {} terminated successfully at {} ✔︎
                        │
                        ╰──╴✔︎
                    """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should format immediate result`() {
                justStart().exec.logging(this) {
                    compact {
                        caption { "caption" }
                        contentFormatter { formatter }
                    }
                }
                expectThatLogged().matchesCurlyPattern("""
                        ╭──╴{}
                        │
                        │   caption Executing {} Take Off Process {} terminated successfully at {} ✔︎
                        │
                        ╰──╴✔︎
                    """.trimIndent())
            }
        }

        @Nested
        inner class Async {

            @Test
            fun InMemoryLogger.`should compact log`() {
                countDownAndStart().exec.async.logging(this) {
                    compact {
                        caption { "caption" }
                        contentFormatter { formatter }
                    }
                }.waitFor()
                expectThatLogged().matchesCurlyPattern("""
                        ╭──╴{}
                        │
                        │   caption ⏳️ async computation
                        │   ⏳️ Executing {}
                        │   ⏳️ Countdown!
                        │   ⏳️ 10
                        │   ⏳️ 9
                        │   ⏳️ 8
                        │   ⏳️ 7
                        │   ⏳️ 6
                        │   ⏳️ 5
                        │   ⏳️ 4
                        │   ⏳️ 3
                        │   ⏳️ 2
                        │   ⏳️ 1
                        │   ⏳️ 0
                        │   ⏳️ Take Off
                        │   ⏳️ Process {} terminated successfully at {}
                        │   ⏳️ ✔︎
                        │
                        ╰──╴✔︎
                    """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should format immediate result`() {
                justStart().exec.async.logging(this) {
                    compact {
                        caption { "caption" }
                        contentFormatter { formatter }
                    }
                }.waitFor()
                expectThatLogged().matchesCurlyPattern("""
                        ╭──╴{}
                        │
                        │   caption ⏳️ async computation
                        │   ⏳️ Executing {}
                        │   ⏳️ Take Off
                        │   ⏳️ Process {} terminated successfully at {}
                        │   ⏳️ ✔︎
                        │
                        ╰──╴✔︎
                    """.trimIndent())
            }
        }
    }


    @Nested
    inner class SmartLog {

        @Nested
        inner class Sync {

            @Test
            fun InMemoryLogger.`should format multiple messages`() {
                countDownAndStart().exec.logging(this) {
                    smart {
                        caption { "caption" }
                        contentFormatter { fromScratch { random } }
                        decorationFormatter { fromScratch { brightYellow } }
                        border = SOLID
                    }
                }
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   ╭──╴caption
                    │   │
                    │   │   Executing {}
                    │   │   Countdown!
                    │   │   10
                    │   │   9
                    │   │   8
                    │   │   7
                    │   │   6
                    │   │   5
                    │   │   4
                    │   │   3
                    │   │   2
                    │   │   1
                    │   │   0
                    │   │   Take Off
                    │   │   Process {} terminated successfully at {}
                    │   │
                    │   ╰──╴✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should format immediate result`() {
                justStart().exec.logging(this) {
                    smart {
                        caption { "caption" }
                        contentFormatter { fromScratch { random } }
                        decorationFormatter { fromScratch { brightYellow } }
                        border = SOLID
                    }
                }
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   ╭──╴caption
                    │   │
                    │   │   Executing {}
                    │   │   Take Off
                    │   │   Process {} terminated successfully at {}
                    │   │
                    │   ╰──╴✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }


        @Nested
        inner class Async {

            @Test
            fun InMemoryLogger.`should format multiple messages`() {
                countDownAndStart().exec.async.logging(this) {
                    smart {
                        caption { "caption" }
                        contentFormatter { fromScratch { random } }
                        decorationFormatter { fromScratch { brightYellow } }
                        border = SOLID
                    }
                }.waitFor()
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   ╭──╴caption
                    │   │
                    │   ╵
                    │   ╵
                    │   ⏳️ async computation
                    │   ⏳️ Executing {}
                    │   ⏳️ Countdown!
                    │   ⏳️ 10
                    │   ⏳️ 9
                    │   ⏳️ 8
                    │   ⏳️ 7
                    │   ⏳️ 6
                    │   ⏳️ 5
                    │   ⏳️ 4
                    │   ⏳️ 3
                    │   ⏳️ 2
                    │   ⏳️ 1
                    │   ⏳️ 0
                    │   ⏳️ Take Off
                    │   ⏳️ Process {} terminated successfully at {}
                    │   ⏳️ ✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should format immediate result`() {
                justStart().exec.async.logging(this) {
                    smart {
                        caption { "caption" }
                        contentFormatter { fromScratch { random } }
                        decorationFormatter { fromScratch { brightYellow } }
                        border = SOLID
                    }
                }.waitFor()
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   ╭──╴caption
                    │   │
                    │   ╵
                    │   ╵
                    │   ⏳️ async computation
                    │   ⏳️ Executing {}
                    │   ⏳️ Take Off
                    │   ⏳️ Process {} terminated successfully at {}
                    │   ⏳️ ✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }
    }

    @Nested
    inner class SummaryLog {

        @Nested
        inner class Sync {

            @Test
            fun InMemoryLogger.`should format multiple messages`() {
                countDownAndStart().exec.logging(this) {
                    summary("caption")
                }
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   caption ➜ Countdown! ➜ 10 ➜ 9 ➜ 8 ➜ 7 ➜ 6 ➜ 5 ➜ 4 ➜ 3 ➜ 2 ➜ 1 ➜ 0 ➜ Take Off ✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should format immediate result`() {
                justStart().exec.logging(this) {
                    summary("caption")
                }
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   caption ➜ Take Off ✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }

        @Nested
        inner class Async {

            @Test
            fun InMemoryLogger.`should format multiple messages`() {
                countDownAndStart().exec.async.logging(this) {
                    summary("caption")
                }.waitFor()
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   caption ⏳️ async computation
                    │   ⏳️ ➜ Countdown!
                    │   ⏳️ ➜ 10
                    │   ⏳️ ➜ 9
                    │   ⏳️ ➜ 8
                    │   ⏳️ ➜ 7
                    │   ⏳️ ➜ 6
                    │   ⏳️ ➜ 5
                    │   ⏳️ ➜ 4
                    │   ⏳️ ➜ 3
                    │   ⏳️ ➜ 2
                    │   ⏳️ ➜ 1
                    │   ⏳️ ➜ 0
                    │   ⏳️ ➜ Take Off
                    │   ⏳️ ✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should format immediate result`() {
                justStart().exec.async.logging(this) {
                    summary("caption")
                }.waitFor()
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   caption ⏳️ async computation
                    │   ⏳️ ➜ Take Off
                    │   ⏳️ ✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }
    }

    @Nested
    inner class NoDetailsLog {

        @Nested
        inner class Sync {

            @Test
            fun InMemoryLogger.`should format multiple messages`() {
                countDownAndStart().exec.logging(this) {
                    noDetails("caption")
                }
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   caption ✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should format immediate result`() {
                justStart().exec.logging(this) {
                    noDetails("caption")
                }
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   caption ✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }

        @Nested
        inner class Async {

            @Test
            fun InMemoryLogger.`should format multiple messages`() {
                countDownAndStart().exec.async.logging(this) {
                    noDetails("caption")
                }.waitFor()
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   caption ⏳️ async computation
                    │   ⏳️ ✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should format immediate result`() {
                justStart().exec.async.logging(this) {
                    noDetails("caption")
                }.waitFor()
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   caption ⏳️ async computation
                    │   ⏳️ ✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }
    }

    @Nested
    inner class ErrorsOnlyLog {

        @Nested
        inner class Sync {

            @Test
            fun InMemoryLogger.`should be empty if no error occurs`() {
                countDownAndStart().exec.logging(this) {
                    errorsOnly("caption")
                }
                expectThatLogged().isEmpty()
            }

            @Test
            fun InMemoryLogger.`should display ERR`() {
                countDownAndBoom().exec.logging(this) {
                    errorsOnly("caption")
                }
                expectThatLogged().matchesCurlyPattern("""
                    {{}}
                    │   caption: 4
                    │   ϟ Process {} terminated with exit code {}
                    │   ➜ A dump has been written to:
                    │     - file://{}
                    │     - file://{}
                    │   ➜ The last 10 lines are:
                    │     8
                    {{}}
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should display failed`() {
                expectThat(
                    countDownAndBoom().exec.logging(this) {
                        errorsOnly("caption")
                    })
                    .hasState<Failed>()
                expectThatLogged().matchesCurlyPattern("""
                    {{}}
                    │   caption: 4
                    │   ϟ Process {} terminated with exit code {}
                    │   ➜ A dump has been written to:
                    │     - file://{}
                    │     - file://{}
                    │   ➜ The last 10 lines are:
                    │     8
                    {{}}
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should hide regular result`() {
                countDownAndStart().exec.logging(this) {
                    errorsOnly("caption")
                }
                expectThatLogged().isEmpty()
            }
        }

        @Nested
        inner class Async {

            @Test
            fun InMemoryLogger.`should be empty if no error occurs`() {
                countDownAndStart().exec.async.logging(this) {
                    errorsOnly("caption")
                }.waitFor()
                expectThatLogged().isEmpty()
            }

            @Test
            fun InMemoryLogger.`should display ERR`() {
                countDownAndBoom().exec.async.logging(this) {
                    errorsOnly("caption")
                }.waitFor()
                expectThatLogged().matchesCurlyPattern("""
                    {{}}
                    │   caption: 4
                    │   ϟ Process {} terminated with exit code {}
                    │   ➜ A dump has been written to:
                    │     - file://{}
                    │     - file://{}
                    │   ➜ The last 10 lines are:
                    {{}}
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should display failed`() {
                expectThat(
                    countDownAndBoom().exec.async.logging(this) {
                        errorsOnly("caption")
                    }.waitFor())
                    .isA<Failed>()
                expectThatLogged().matchesCurlyPattern("""
                    {{}}
                    │   caption: 4
                    │   ϟ Process {} terminated with exit code {}
                    │   ➜ A dump has been written to:
                    │     - file://{}
                    │     - file://{}
                    │   ➜ The last 10 lines are:
                    {{}}
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should hide regular result`() {
                countDownAndStart().exec.async.logging(this) {
                    errorsOnly("caption")
                }.waitFor()
                expectThatLogged().isEmpty()
            }

            @Test
            fun InMemoryLogger.`should hide incomplete`() {
                justStart().exec.async.logging(this) {
                    errorsOnly("caption")
                }.waitFor()
                expectThatLogged().isEmpty()
            }
        }
    }

    private fun countDownAndStart() = ShellScript {
        !"echo 'Countdown!'"
        (10 downTo 0).forEach { !"echo '$it'" }
        !"echo 'Take Off'"
    }

    private fun countDownAndBoom() = ShellScript {
        !"echo 'Countdown!'"
        (10 downTo 5).forEach { !"echo '$it'" }
        !">&2 echo '4'"
        (3 downTo 0).forEach { !"echo '$it'" }
        !"exit 1"
    }

    private fun justStart() = ShellScript {
        !"echo 'Take Off'"
    }
}
