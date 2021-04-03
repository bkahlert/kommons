package koodies.logging

import koodies.concurrent.execute
import koodies.logging.BorderedRenderingLogger.Border.SOLID
import koodies.shell.ShellScript
import koodies.text.ANSI
import koodies.text.ANSI.Formatter.Companion.fromScratch
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectCatching
import strikt.assertions.isEmpty
import strikt.assertions.isFailure

@Execution(SAME_THREAD)
class LoggingOptionsTest {

    @Nested
    inner class BlockLog {

        @Nested
        inner class Sync {

            @Test
            fun InMemoryLogger.`should format multiple messages`() {
                countDownAndStart().execute {
                    block {
                        caption { "caption" }
                        contentFormatter { fromScratch { random() } }
                        decorationFormatter { fromScratch { brightYellow() } }
                        border = SOLID
                    }
                    null
                }
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   ╭──╴caption
                    │   │   
                    │   │   Executing {}
                    │   │   {} file:{}
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
                justStart().execute {
                    block {
                        caption { "caption" }
                        contentFormatter { fromScratch { random() } }
                        decorationFormatter { fromScratch { brightYellow() } }
                        border = SOLID
                    }
                    null
                }
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   ╭──╴caption
                    │   │   
                    │   │   Executing {}
                    │   │   {} file:{}
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
                countDownAndStart().execute {
                    processing { async }
                    block {
                        caption { "caption" }
                        contentFormatter { fromScratch { random() } }
                        decorationFormatter { fromScratch { brightYellow() } }
                        border = SOLID
                    }
                    null
                }.waitForTermination()
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   ╭──╴caption
                    │   │   
                    │   ╵
                    │   ╵
                    │   ⌛️ async computation
                    │   ⌛️ Executing {}
                    │   ⌛️ {} file:{}
                    │   ⌛️ Countdown!
                    │   ⌛️ 10
                    │   ⌛️ 9
                    │   ⌛️ 8
                    │   ⌛️ 7
                    │   ⌛️ 6
                    │   ⌛️ 5
                    │   ⌛️ 4
                    │   ⌛️ 3
                    │   ⌛️ 2
                    │   ⌛️ 1
                    │   ⌛️ 0
                    │   ⌛️ Take Off
                    │   ⌛️ Process {} terminated successfully at {}
                    │   ⌛️ ✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should format immediate result`() {
                justStart().execute {
                    processing { async }
                    block {
                        caption { "caption" }
                        contentFormatter { fromScratch { random() } }
                        decorationFormatter { fromScratch { brightYellow() } }
                        border = SOLID
                    }
                    null
                }.waitForTermination()
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   ╭──╴caption
                    │   │   
                    │   ╵
                    │   ╵
                    │   ⌛️ async computation
                    │   ⌛️ Executing {}
                    │   ⌛️ {} file:{}
                    │   ⌛️ Take Off
                    │   ⌛️ Process {} terminated successfully at {}
                    │   ⌛️ ✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }
    }

    @Nested
    inner class CompactLog {

        @Nested
        inner class Sync {

            @Test
            fun InMemoryLogger.`should compact log`() {
                countDownAndStart().execute {
                    compact {
                        caption { "caption" }
                        contentFormatter { ANSI.Colors.magenta + ANSI.Style.inverse }
                    }
                    null
                }
                expectThatLogged().matchesCurlyPattern("""
                        ╭──╴{}
                        │   
                        │   caption Executing {} file:{} Countdown! 10 9 8 7 6 5 4 3 2 1 0 Take Off Process {} terminated successfully at {} ✔︎
                        │
                        ╰──╴✔︎
                    """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should format immediate result`() {
                justStart().execute {
                    compact {
                        caption { "caption" }
                        contentFormatter { ANSI.Colors.magenta + ANSI.Style.inverse }
                    }
                    null
                }
                expectThatLogged().matchesCurlyPattern("""
                        ╭──╴{}
                        │   
                        │   caption Executing {} file:{} Take Off Process {} terminated successfully at {} ✔︎
                        │
                        ╰──╴✔︎
                    """.trimIndent())
            }
        }

        @Nested
        inner class Async {

            @Test
            fun InMemoryLogger.`should compact log`() {
                countDownAndStart().execute {
                    processing { async }
                    compact {
                        caption { "caption" }
                        contentFormatter { ANSI.Colors.magenta + ANSI.Style.inverse }
                    }
                    null
                }.waitForTermination()
                expectThatLogged().matchesCurlyPattern("""
                        ╭──╴{}
                        │   
                        │   caption ⌛️ async computation
                        │   ⌛️ Executing {}
                        │   ⌛️ {} file:{}
                        │   ⌛️ Countdown!
                        │   ⌛️ 10
                        │   ⌛️ 9
                        │   ⌛️ 8
                        │   ⌛️ 7
                        │   ⌛️ 6
                        │   ⌛️ 5
                        │   ⌛️ 4
                        │   ⌛️ 3
                        │   ⌛️ 2
                        │   ⌛️ 1
                        │   ⌛️ 0
                        │   ⌛️ Take Off
                        │   ⌛️ Process {} terminated successfully at {}
                        │   ⌛️ ✔︎
                        │
                        ╰──╴✔︎
                    """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should format immediate result`() {
                justStart().execute {
                    processing { async }
                    compact {
                        caption { "caption" }
                        contentFormatter { ANSI.Colors.magenta + ANSI.Style.inverse }
                    }
                    null
                }.waitForTermination()
                expectThatLogged().matchesCurlyPattern("""
                        ╭──╴{}
                        │   
                        │   caption ⌛️ async computation
                        │   ⌛️ Executing {}
                        │   ⌛️ {} file:{}
                        │   ⌛️ Take Off
                        │   ⌛️ Process {} terminated successfully at {}
                        │   ⌛️ ✔︎
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
                countDownAndStart().execute {
                    smart {
                        caption { "caption" }
                        contentFormatter { fromScratch { random() } }
                        decorationFormatter { fromScratch { brightYellow() } }
                        border = SOLID
                    }
                    null
                }
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   ╭──╴caption
                    │   │   
                    │   │   Executing {}
                    │   │   {} file:{}
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
                justStart().execute {
                    smart {
                        caption { "caption" }
                        contentFormatter { fromScratch { random() } }
                        decorationFormatter { fromScratch { brightYellow() } }
                        border = SOLID
                    }
                    null
                }
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   ╭──╴caption
                    │   │   
                    │   │   Executing {}
                    │   │   {} file:{}
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
                countDownAndStart().execute {
                    processing { async }
                    smart {
                        caption { "caption" }
                        contentFormatter { fromScratch { random() } }
                        decorationFormatter { fromScratch { brightYellow() } }
                        border = SOLID
                    }
                    null
                }.waitForTermination()
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   ╭──╴caption
                    │   │   
                    │   ╵
                    │   ╵
                    │   ⌛️ async computation
                    │   ⌛️ Executing {}
                    │   ⌛️ 📄 file://{}
                    │   ⌛️ Countdown!
                    │   ⌛️ 10
                    │   ⌛️ 9
                    │   ⌛️ 8
                    │   ⌛️ 7
                    │   ⌛️ 6
                    │   ⌛️ 5
                    │   ⌛️ 4
                    │   ⌛️ 3
                    │   ⌛️ 2
                    │   ⌛️ 1
                    │   ⌛️ 0
                    │   ⌛️ Take Off
                    │   ⌛️ Process {} terminated successfully at {}
                    │   ⌛️ ✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should format immediate result`() {
                justStart().execute {
                    processing { async }
                    smart {
                        caption { "caption" }
                        contentFormatter { fromScratch { random() } }
                        decorationFormatter { fromScratch { brightYellow() } }
                        border = SOLID
                    }
                    null
                }.waitForTermination()
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   ╭──╴caption
                    │   │   
                    │   ╵
                    │   ╵
                    │   ⌛️ async computation
                    │   ⌛️ Executing {}
                    │   ⌛️ 📄 file://{}
                    │   ⌛️ Take Off
                    │   ⌛️ Process {} terminated successfully at {}
                    │   ⌛️ ✔︎
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
                countDownAndStart().execute {
                    summary("caption")
                    null
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
                justStart().execute {
                    summary("caption")
                    null
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
                countDownAndStart().execute {
                    processing { async }
                    summary("caption")
                    null
                }.waitForTermination()
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   caption ⌛️ async computation
                    │   ⌛️ ➜ Countdown!
                    │   ⌛️ ➜ 10
                    │   ⌛️ ➜ 9
                    │   ⌛️ ➜ 8
                    │   ⌛️ ➜ 7
                    │   ⌛️ ➜ 6
                    │   ⌛️ ➜ 5
                    │   ⌛️ ➜ 4
                    │   ⌛️ ➜ 3
                    │   ⌛️ ➜ 2
                    │   ⌛️ ➜ 1
                    │   ⌛️ ➜ 0
                    │   ⌛️ ➜ Take Off
                    │   ⌛️ ✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should format immediate result`() {
                justStart().execute {
                    processing { async }
                    summary("caption")
                    null
                }.waitForTermination()
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   caption ⌛️ async computation
                    │   ⌛️ ➜ Take Off
                    │   ⌛️ ✔︎
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
                countDownAndStart().execute {
                    noDetails("caption")
                    null
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
                justStart().execute {
                    noDetails("caption")
                    null
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
                countDownAndStart().execute {
                    processing { async }
                    noDetails("caption")
                    null
                }.waitForTermination()
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   caption ⌛️ async computation
                    │   ⌛️ ✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should format immediate result`() {
                justStart().execute {
                    processing { async }
                    noDetails("caption")
                    null
                }.waitForTermination()
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   caption ⌛️ async computation
                    │   ⌛️ ✔︎
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
                countDownAndStart().execute {
                    errorsOnly("caption")
                    null
                }
                expectThatLogged().isEmpty()
            }

            @Test
            fun InMemoryLogger.`should display ERR`() {
                countDownAndBoom().execute {
                    ignoreExitValue()
                    errorsOnly("caption")
                    null
                }
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   caption: 4
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should display exceptions`() {
                expectCatching {
                    countDownAndBoom().execute {
                        errorsOnly("caption")
                        null
                    }
                }.isFailure()
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    {}
                    │   ϟ {}Exception: {}
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should hide regular result`() {
                countDownAndStart().execute {
                    ignoreExitValue()
                    errorsOnly("caption")
                    null
                }
                expectThatLogged().isEmpty()
            }
        }

        @Nested
        inner class Async {

            @Test
            fun InMemoryLogger.`should be empty if no error occurs`() {
                countDownAndStart().execute {
                    processing { async }
                    errorsOnly("caption")
                    null
                }.waitForTermination()
                expectThatLogged().isEmpty()
            }

            @Test
            fun InMemoryLogger.`should display ERR`() {
                countDownAndBoom().execute {
                    processing { async }
                    ignoreExitValue()
                    errorsOnly("caption")
                    null
                }.waitForTermination()
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   caption: 4
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should display exceptions`() {
                expectCatching {
                    countDownAndBoom().execute {
                        processing { async }
                        errorsOnly("caption")
                        null
                    }.waitForTermination()
                }.isFailure()
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    {}
                    │   ϟ {}Exception: {}
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should hide regular result`() {
                countDownAndStart().execute {
                    ignoreExitValue()
                    errorsOnly("caption")
                    null
                }.waitForTermination()
                expectThatLogged().isEmpty()
            }

            @Test
            fun InMemoryLogger.`should hide incomplete`() {
                justStart().execute {
                    processing { async }
                    errorsOnly("caption")
                    null
                }.waitForTermination()
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
        !"exit -1"
    }

    private fun justStart() = ShellScript {
        !"echo 'Take Off'"
    }
}
