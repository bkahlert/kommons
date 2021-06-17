package koodies.logging

import koodies.exec.Exec
import koodies.exec.Executable
import koodies.exec.Executor
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
import koodies.shell.ShellScript
import koodies.test.tests
import koodies.text.ANSI
import koodies.text.ANSI.FilteringFormatter.Companion.fromScratch
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory


class LoggingOptionsTest {

    private fun InMemoryLogger.testLogOfSyncAndAsyncExec(
        expectedCurlyPattern: String,
        executable: Executable<Exec>,
        invocation: Executor<out Exec>.(SimpleRenderingLogger) -> Exec,
    ): List<DynamicNode> {
        return tests {
            expecting { capturing { executable.exec.invocation(it) } } that { matchesCurlyPattern(expectedCurlyPattern) }
            expecting { capturing { executable.exec.async.invocation(it).waitFor() } } that { matchesCurlyPattern(expectedCurlyPattern) }
        }
    }

    @Nested
    inner class BlockLog {

        @TestFactory
        fun InMemoryLogger.`should format multiple messages`(): List<DynamicNode> = testLogOfSyncAndAsyncExec("""
                ╭──╴name
                │
                │   Executing {}
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
                │   Process {} terminated successfully at {}
                │
                ╰──╴✔︎
            """.trimIndent(), countDownAndStart()) {
            logging(it) {
                block {
                    name { "name" }
                    contentFormatter { fromScratch { random } }
                    decorationFormatter { Formatter.fromScratch { brightYellow } }
                    border = SOLID
                }
            }
        }

        @TestFactory
        fun InMemoryLogger.`should format immediate result`(): List<DynamicNode> = testLogOfSyncAndAsyncExec("""
                ╭──╴name
                │
                │   Executing {}
                │   Take Off
                │   Process {} terminated successfully at {}
                │
                ╰──╴✔︎
            """.trimIndent(), justStart()) {
            logging(it) {
                block {
                    name { "name" }
                    contentFormatter { fromScratch { random } }
                    decorationFormatter { Formatter.fromScratch { brightYellow } }
                    border = SOLID
                }
            }
        }
    }

    @Nested
    inner class CompactLog {

        private val formatter: ANSI.FilteringFormatter = ANSI.FilteringFormatter { it.ansi.inverse.magenta }

        @TestFactory
        fun InMemoryLogger.`should compact log`(): List<DynamicNode> = testLogOfSyncAndAsyncExec("""
                name Executing {} Countdown! 10 9 8 7 6 5 4 3 2 1 0 Take Off Process {} terminated successfully at {} ✔︎
            """.trimIndent(), countDownAndStart()) {
            logging(it) {
                compact {
                    name { "name" }
                    contentFormatter { formatter }
                }
            }
        }

        @TestFactory
        fun InMemoryLogger.`should format immediate result`(): List<DynamicNode> = testLogOfSyncAndAsyncExec("""
                name Executing {} Take Off Process {} terminated successfully at {} ✔︎
            """.trimIndent(), justStart()) {
            logging(it) {
                compact {
                    name { "name" }
                    contentFormatter { formatter }
                }
            }
        }
    }


    @Nested
    inner class SmartLog {

        @TestFactory
        fun InMemoryLogger.`should format multiple messages`(): List<DynamicNode> = testLogOfSyncAndAsyncExec("""
                ╭──╴name
                │
                │   Executing {}
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
                │   Process {} terminated successfully at {}
                │
                ╰──╴✔︎
            """.trimIndent(), countDownAndStart()) {
            logging(it) {
                smart {
                    name { "name" }
                    contentFormatter { fromScratch { random } }
                    decorationFormatter { Formatter.fromScratch { brightYellow } }
                    border = SOLID
                }
            }
        }

        @TestFactory
        fun InMemoryLogger.`should format immediate result`(): List<DynamicNode> = testLogOfSyncAndAsyncExec("""
                ╭──╴name
                │
                │   Executing {}
                │   Take Off
                │   Process {} terminated successfully at {}
                │
                ╰──╴✔︎
            """.trimIndent(), justStart()) {
            logging(it) {
                smart {
                    name { "name" }
                    contentFormatter { fromScratch { random } }
                    decorationFormatter { Formatter.fromScratch { brightYellow } }
                    border = SOLID
                }
            }
        }
    }

    @Nested
    inner class SummaryLog {


        @TestFactory
        fun InMemoryLogger.`should format multiple messages`(): List<DynamicNode> = testLogOfSyncAndAsyncExec("""
                name ➜ Countdown! ➜ 10 ➜ 9 ➜ 8 ➜ 7 ➜ 6 ➜ 5 ➜ 4 ➜ 3 ➜ 2 ➜ 1 ➜ 0 ➜ Take Off ✔︎
                """.trimIndent(), countDownAndStart()) {
            logging(it) {
                summary("name")
            }
        }

        @TestFactory
        fun InMemoryLogger.`should format immediate result`(): List<DynamicNode> = testLogOfSyncAndAsyncExec("""
                name ➜ Take Off ✔︎
            """.trimIndent(), justStart()) {
            logging(it) {
                summary("name")
            }
        }
    }

    @Nested
    inner class NoDetailsLog {

        @TestFactory
        fun InMemoryLogger.`should format multiple messages`(): List<DynamicNode> = testLogOfSyncAndAsyncExec("""
                name ✔︎
            """.trimIndent(), countDownAndStart()) {
            logging(it) {
                noDetails("name")
            }
        }

        @TestFactory
        fun InMemoryLogger.`should format immediate result`(): List<DynamicNode> = testLogOfSyncAndAsyncExec("""
                name ✔︎
            """.trimIndent(), justStart()) {
            logging(it) {
                noDetails("name")
            }
        }
    }

    @Nested
    inner class ErrorsOnlyLog {

        @TestFactory
        fun InMemoryLogger.`should be empty if no error occurs`(): List<DynamicNode> = testLogOfSyncAndAsyncExec("", countDownAndStart()) {
            logging(it) {
                errorsOnly("name")
            }
        }

        @TestFactory
        fun InMemoryLogger.`should display ERR`(): List<DynamicNode> = testLogOfSyncAndAsyncExec("""
                name: 4
                ϟ Process {} terminated with exit code {}
                ➜ A dump has been written to:
                  - file://{}
                  - file://{}
                ➜ The last 10 lines are:
                {{}}
            """.trimIndent(), countDownAndBoom()) {
            logging(it) {
                errorsOnly("name")
            }
        }

        @TestFactory
        fun InMemoryLogger.`should hide regular result`(): List<DynamicNode> = testLogOfSyncAndAsyncExec("", countDownAndStart()) {
            logging(it) {
                errorsOnly("name")
            }
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
        exit(1)
    }

    private fun justStart() = ShellScript {
        echo("Take Off")
    }
}
