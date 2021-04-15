package koodies.concurrent.process

import koodies.io.path.asPath
import koodies.logging.MutedRenderingLogger
import koodies.test.toStringIsEqualTo
import koodies.text.ANSI.ansiRemoved
import koodies.text.LineSeparators.LF
import koodies.text.Semantics.Symbols
import koodies.text.containsEscapeSequences
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

@Execution(CONCURRENT)
class IOTest {

    @Nested
    inner class Meta {

        @Nested
        inner class Starting {

            private val commandLine = CommandLine("command", "arg")
            private val meta = IO.META.STARTING(commandLine)

            @Test
            fun `should have original text`() {
                expectThat(meta.text).toStringIsEqualTo("Executing ${commandLine.commandLine}")
            }

            @Test
            fun `should have formatted text`() {
                expectThat(meta).containsEscapeSequences().toStringIsEqualTo("Executing ${commandLine.commandLine}")
            }
        }

        @Nested
        inner class File {

            private val file = "file".asPath()
            private val meta = IO.META typed file

            @Test
            fun `should have original text`() {
                expectThat(meta.text).toStringIsEqualTo("${Symbols.Document} ${file.toUri()}")
            }

            @Test
            fun `should have formatted text`() {
                expectThat(meta).containsEscapeSequences().toStringIsEqualTo("${Symbols.Document} ${file.toUri()}")
            }
        }

        @Nested
        inner class Text {

            private val text = "text"
            private val meta = IO.META.TEXT(text)

            @Test
            fun `should have original text`() {
                expectThat(meta.text).toStringIsEqualTo(text)
            }

            @Test
            fun `should have formatted text`() {
                expectThat(meta).containsEscapeSequences().toStringIsEqualTo(text)
            }

            @Test
            fun `should throw on blank`() {
                expectCatching { IO.META typed "    " }.isFailure()
            }
        }

        @Nested
        inner class Dump {

            private val dump = "dump"
            private val meta = IO.META.DUMP(dump)

            @Test
            fun `should have original text`() {
                expectThat(meta.text).toStringIsEqualTo(dump)
            }

            @Test
            fun `should have formatted text`() {
                expectThat(meta).containsEscapeSequences().toStringIsEqualTo(dump)
            }

            @Test
            fun `should throw on non-dump`() {
                expectCatching { IO.META.DUMP("whatever") }.isFailure()
            }
        }

        @Nested
        inner class Terminated {

            private val process = ExecMock(JavaProcessMock(MutedRenderingLogger()) { ProcessExitMock.immediateSuccess() })
            private val meta = IO.META.TERMINATED(process)

            @Test
            fun `should have original text`() {
                expectThat(meta.text).matchesCurlyPattern("Process ${process.pid} terminated successfully at {}.")
            }

            @Test
            fun `should have formatted text`() {
                expectThat(meta).containsEscapeSequences().matchesCurlyPattern("Process ${process.pid} terminated successfully at {}.")
            }
        }
    }

    @Nested
    inner class In {

        private val `in` = IO.INPUT typed "in"

        @Test
        fun `should have original text`() {
            expectThat(`in`.text).toStringIsEqualTo("in")
        }

        @Test
        fun `should have formatted text`() {
            expectThat(`in`).containsEscapeSequences().toStringIsEqualTo("in")
        }
    }

    @Nested
    inner class Out {

        private val out = IO.OUT typed "out"

        @Test
        fun `should have original text`() {
            expectThat(out.text).toStringIsEqualTo("out")
        }

        @Test
        fun `should have formatted text`() {
            expectThat(out).containsEscapeSequences().toStringIsEqualTo("out")
        }
    }

    @Nested
    inner class Err {

        private val err = IO.ERR(RuntimeException("err"))

        @Test
        fun `should have plain stacktrace`() {
            expectThat(err.text).toStringMatchesCurlyPattern("""
                {}.RuntimeException: err
                	at koodies.{}
                {{}}
            """.trimIndent())
        }

        @Test
        fun `should have formatted stacktrace`() {
            expectThat(err).containsEscapeSequences().toStringMatchesCurlyPattern("""
                {}.RuntimeException: err
                	at koodies.{}
                {{}}
            """.trimIndent())
        }
    }

    @Nested
    inner class IoProperties {

        @Test
        fun `should filter meta`() {
            expectThat(IO_LIST.meta).containsExactly(IO_LIST.subList(0, 5))
        }

        @Test
        fun `should filter in`() {
            expectThat(IO_LIST.input).containsExactly(IO_LIST.subList(5, 6))
        }

        @Test
        fun `should filter out`() {
            expectThat(IO_LIST.out).containsExactly(IO_LIST.subList(6, 7))
        }

        @Test
        fun `should filter err`() {
            expectThat(IO_LIST.err).containsExactly(IO_LIST.subList(7, 8))
        }

        @Test
        fun `should keep ansi escape codes when merging`() {
            expectThat(IO_LIST.merged).containsEscapeSequences()
        }

        @Test
        fun `should merge multiple types`() {
            expectThat(IO_LIST.drop(2).take(2).merged.ansiRemoved).isEqualTo("text${LF}dump")
        }

        @Test
        fun `should merge single type`() {
            expectThat(IO_LIST.out.merged.ansiRemoved).isEqualTo("out")
        }
    }

    companion object {
        val IO_LIST = listOf(
            IO.META.STARTING(CommandLine("command", "arg")),
            IO.META typed "file".asPath(),
            IO.META.TEXT("text"),
            IO.META.DUMP("dump"),
            IO.META.TERMINATED(ExecMock(JavaProcessMock(MutedRenderingLogger()) { ProcessExitMock.immediateSuccess() })),
            IO.INPUT typed "in",
            IO.OUT typed "out",
            IO.ERR(RuntimeException("err")),
        )
    }
}
