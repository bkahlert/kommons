package koodies.logging

import koodies.concurrent.execute
import koodies.shell.ShellScript
import koodies.text.ANSI
import koodies.text.ANSI.Formatter.Companion.fromScratch
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD

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
                        border using true
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
                        border using true
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
                        border using true
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
                    │   caption ⌛️ Executing {}
                    │   caption ⌛️ {} file:{}
                    │   caption ⌛️ Countdown!
                    │   caption ⌛️ 10
                    │   caption ⌛️ 9
                    │   caption ⌛️ 8
                    │   caption ⌛️ 7
                    │   caption ⌛️ 6
                    │   caption ⌛️ 5
                    │   caption ⌛️ 4
                    │   caption ⌛️ 3
                    │   caption ⌛️ 2
                    │   caption ⌛️ 1
                    │   caption ⌛️ 0
                    │   caption ⌛️ Take Off
                    │   caption ⌛️ Process {} terminated successfully at {}
                    │   caption ⌛️ ✔︎
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
                        border using true
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
                    │   caption ⌛️ Executing {}
                    │   caption ⌛️ {} file:{}
                    │   caption ⌛️ Take Off
                    │   caption ⌛️ Process {} terminated successfully at {}
                    │   caption ⌛️ ✔︎
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
                        │   caption ⌛️ Executing {}
                        │   caption ⌛️ {} file:{}
                        │   caption ⌛️ Countdown!
                        │   caption ⌛️ 10
                        │   caption ⌛️ 9
                        │   caption ⌛️ 8
                        │   caption ⌛️ 7
                        │   caption ⌛️ 6
                        │   caption ⌛️ 5
                        │   caption ⌛️ 4
                        │   caption ⌛️ 3
                        │   caption ⌛️ 2
                        │   caption ⌛️ 1
                        │   caption ⌛️ 0
                        │   caption ⌛️ Take Off
                        │   caption ⌛️ Process {} terminated successfully at {}
                        │   caption ⌛️ ✔︎
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
                        │   caption ⌛️ Executing {}
                        │   caption ⌛️ {} file:{}
                        │   caption ⌛️ Take Off
                        │   caption ⌛️ Process {} terminated successfully at {}
                        │   caption ⌛️ ✔︎
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
                        border using true
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
                        border using true
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
                        border using true
                    }
                    null
                }.waitForTermination()
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   caption ⌛️ async computation
                    │   caption ⌛️ Executing {}
                    │   caption ⌛️ {} file:{}
                    │   caption ⌛️ Countdown!
                    │   caption ⌛️ 10
                    │   caption ⌛️ 9
                    │   caption ⌛️ 8
                    │   caption ⌛️ 7
                    │   caption ⌛️ 6
                    │   caption ⌛️ 5
                    │   caption ⌛️ 4
                    │   caption ⌛️ 3
                    │   caption ⌛️ 2
                    │   caption ⌛️ 1
                    │   caption ⌛️ 0
                    │   caption ⌛️ Take Off
                    │   caption ⌛️ Process {} terminated successfully at {}
                    │   caption ⌛️ ✔︎
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
                        border using true
                    }
                    null
                }.waitForTermination()
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   caption ⌛️ async computation
                    │   caption ⌛️ Executing {}
                    │   caption ⌛️ {} file:{}
                    │   caption ⌛️ Take Off
                    │   caption ⌛️ Process {} terminated successfully at {}
                    │   caption ⌛️ ✔︎
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
                    │   caption ⌛️ ➜ Countdown!
                    │   caption ⌛️ ➜ 10
                    │   caption ⌛️ ➜ 9
                    │   caption ⌛️ ➜ 8
                    │   caption ⌛️ ➜ 7
                    │   caption ⌛️ ➜ 6
                    │   caption ⌛️ ➜ 5
                    │   caption ⌛️ ➜ 4
                    │   caption ⌛️ ➜ 3
                    │   caption ⌛️ ➜ 2
                    │   caption ⌛️ ➜ 1
                    │   caption ⌛️ ➜ 0
                    │   caption ⌛️ ➜ Take Off
                    │   caption ⌛️ ✔︎
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
                    │   caption ⌛️ ➜ Take Off
                    │   caption ⌛️ ✔︎
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
                    │   caption ⌛️ ✔︎
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
                    │   caption ⌛️ ✔︎
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }
    }

    private fun countDownAndStart() = ShellScript {
        !"echo 'Countdown!'"
        (10 downTo 0).forEach { !"echo '$it'" }
        !"echo 'Take Off'"
    }

    private fun justStart() = ShellScript {
        !"echo 'Take Off'"
    }
}
