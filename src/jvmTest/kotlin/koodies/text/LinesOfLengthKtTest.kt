package koodies.text

import koodies.terminal.AnsiCode
import koodies.terminal.AnsiString.Companion.asAnsiString
import koodies.terminal.AnsiStringTest.Companion.ansiString
import koodies.terminal.AnsiStringTest.Companion.nonAnsiString
import koodies.text.LineSeparators.LF
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.containsExactly

@Execution(CONCURRENT)
class LinesOfLengthKtTest {

    @Nested
    inner class NonAnsiString {
        @TestFactory
        fun `should be split with maximum line length`(): List<DynamicNode> = listOf(
            "sequence" to "$nonAnsiString$LF".linesOfLengthSequence(26).toList(),
            "list" to "$nonAnsiString$LF".linesOfLength(26),
        ).map { (method, lines) ->
            dynamicTest("using $method") {
                expectThat(lines).containsExactly(
                    "Important: This line has n",
                    "o ANSI escapes.",
                    "This one's bold!",
                    "Last one is clean.",
                    "",
                )
            }
        }

        @TestFactory
        fun `should be split with maximum line length with trailing line removed`(): List<DynamicNode> = listOf(
            "sequence" to "$nonAnsiString$LF".linesOfLengthSequence(26, ignoreTrailingSeparator = true).toList(),
            "list" to "$nonAnsiString$LF".linesOfLength(26, ignoreTrailingSeparator = true),
        ).map { (method, lines) ->
            dynamicTest("using $method") {
                expectThat(lines).containsExactly(
                    "Important: This line has n",
                    "o ANSI escapes.",
                    "This one's bold!",
                    "Last one is clean.",
                )
            }
        }
    }


    @Nested
    inner class AnsiString {
        @TestFactory
        fun `should be split with maximum line length`(): List<DynamicNode> = listOf(
            "sequence" to (ansiString + LF).linesOfLengthSequence(26).toList(),
            "list" to (ansiString + LF).linesOfLength(26),
        ).map { (method, lines) ->
            dynamicTest("using $method") {
                expectThat(lines).containsExactly(
                    "${AnsiCode.ESC}[3;36m${AnsiCode.ESC}[4mImportant:${AnsiCode.ESC}[24m This line has ${AnsiCode.ESC}[9mn${AnsiCode.ESC}[23;39;29m".asAnsiString(),
                    "${AnsiCode.ESC}[3;36;9mo${AnsiCode.ESC}[29m ANSI escapes.${AnsiCode.ESC}[23;39m".asAnsiString(),
                    "${AnsiCode.ESC}[3;36mThis one's ${AnsiCode.ESC}[1mbold!${AnsiCode.ESC}[23;39;22m".asAnsiString(),
                    "${AnsiCode.ESC}[3;36mLast one is clean.${AnsiCode.ESC}[23;39m".asAnsiString(),
                    "".asAnsiString(),
                )
            }
        }

        @TestFactory
        fun `should be split with maximum line length with trailing line removed`(): List<DynamicNode> = listOf(
            "sequence" to (ansiString + LF).linesOfLengthSequence(26, ignoreTrailingSeparator = true).toList(),
            "list" to (ansiString + LF).linesOfLength(26, ignoreTrailingSeparator = true),
        ).map { (method, lines) ->
            dynamicTest("using $method") {
                expectThat(lines).containsExactly(
                    "${AnsiCode.ESC}[3;36m${AnsiCode.ESC}[4mImportant:${AnsiCode.ESC}[24m This line has ${AnsiCode.ESC}[9mn${AnsiCode.ESC}[23;39;29m".asAnsiString(),
                    "${AnsiCode.ESC}[3;36;9mo${AnsiCode.ESC}[29m ANSI escapes.${AnsiCode.ESC}[23;39m".asAnsiString(),
                    "${AnsiCode.ESC}[3;36mThis one's ${AnsiCode.ESC}[1mbold!${AnsiCode.ESC}[23;39;22m".asAnsiString(),
                    "${AnsiCode.ESC}[3;36mLast one is clean.${AnsiCode.ESC}[23;39m".asAnsiString(),
                )
            }
        }
    }
}
