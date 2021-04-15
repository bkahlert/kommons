package koodies.text

import koodies.text.AnsiString.Companion.asAnsiString
import koodies.text.AnsiStringTest.Companion.ansiString
import koodies.text.AnsiStringTest.Companion.nonAnsiString
import koodies.text.LineSeparators.LF
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.containsExactly
import koodies.text.Unicode.escape as ESC

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
                    "${ESC}[3;36m${ESC}[4mImportant:${ESC}[24m This line has ${ESC}[9mn${ESC}[23;39;29m".asAnsiString(),
                    "${ESC}[3;36;9mo${ESC}[29m ANSI escapes.${ESC}[23;39m".asAnsiString(),
                    "${ESC}[3;36mThis one's ${ESC}[1mbold!${ESC}[23;39;22m".asAnsiString(),
                    "${ESC}[3;36mLast one is clean.${ESC}[23;39m".asAnsiString(),
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
                    "${ESC}[3;36m${ESC}[4mImportant:${ESC}[24m This line has ${ESC}[9mn${ESC}[23;39;29m".asAnsiString(),
                    "${ESC}[3;36;9mo${ESC}[29m ANSI escapes.${ESC}[23;39m".asAnsiString(),
                    "${ESC}[3;36mThis one's ${ESC}[1mbold!${ESC}[23;39;22m".asAnsiString(),
                    "${ESC}[3;36mLast one is clean.${ESC}[23;39m".asAnsiString(),
                )
            }
        }
    }
}
