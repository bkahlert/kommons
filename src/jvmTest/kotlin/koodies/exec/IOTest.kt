package koodies.exec

import koodies.exec.IO.Error
import koodies.exec.IO.Input
import koodies.exec.IO.Meta
import koodies.exec.IO.Meta.Dump
import koodies.exec.IO.Meta.Starting
import koodies.exec.IO.Meta.Terminated
import koodies.exec.IO.Meta.Text
import koodies.exec.IO.Output
import koodies.exec.mock.ExecMock
import koodies.exec.mock.JavaProcessMock
import koodies.io.path.Locations
import koodies.io.path.asPath
import koodies.logging.MutedRenderingLogger
import koodies.test.toStringIsEqualTo
import koodies.text.LineSeparators.LF
import koodies.text.Semantics.Symbols
import koodies.text.Unicode.characterTabulation
import koodies.text.containsEscapeSequences
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

class IOTest {

    @Nested
    inner class Meta {

        @Nested
        inner class Starting {

            private val commandLine = CommandLine("command", "arg")
            private val meta = IO.Meta.Starting(commandLine)

            @Test
            fun `should have original text`() {
                expectThat(meta.text).toStringIsEqualTo("Executing command arg")
            }

            @Test
            fun `should have formatted text`() {
                expectThat(meta).containsEscapeSequences().toStringIsEqualTo("Executing command arg")
            }
        }

        @Nested
        inner class File {

            private val file = "file".asPath()
            private val meta = IO.Meta typed file

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
            private val meta = IO.Meta.Text(text)

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
                expectCatching { IO.Meta typed "    " }.isFailure()
            }
        }

        @Nested
        inner class Dump {

            private val dump = "dump"
            private val meta = IO.Meta.Dump(dump)

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
                expectCatching { IO.Meta.Dump("whatever") }.isFailure()
            }
        }

        @Nested
        inner class Terminated {

            private val process = JavaExec(JavaProcessMock(MutedRenderingLogger), Locations.Temp, CommandLine("echo", JavaProcessMock::class.simpleName!!))
            private val meta = IO.Meta.Terminated(process)

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
    inner class Input {

        private val `in` = IO.Input typed "in"

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
    inner class Output {

        private val out = IO.Output typed "out"

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
    inner class Error {

        private val err = IO.Error(RuntimeException("err"))

        @Test
        fun `should have plain stacktrace`() {
            expectThat(err.text).toStringMatchesCurlyPattern("""
                {}.RuntimeException: err
                ${characterTabulation}at koodies.{}
                {{}}
            """.trimIndent())
        }

        @Test
        fun `should have formatted stacktrace`() {
            expectThat(err).containsEscapeSequences().toStringMatchesCurlyPattern("""
                {}.RuntimeException: err
                ${characterTabulation}at koodies.{}
                {{}}
            """.trimIndent())
        }
    }

    @Nested
    inner class IOProperties {

        @Test
        fun `should filter meta`() {
            expectThat(IO_LIST.meta.toList()).containsExactly(IO_LIST.toList().subList(0, 5))
        }

        @Test
        fun `should filter in`() {
            expectThat(IO_LIST.input.toList()).containsExactly(IO_LIST.toList().subList(5, 6))
        }

        @Test
        fun `should filter out`() {
            expectThat(IO_LIST.output.toList()).containsExactly(IO_LIST.toList().subList(6, 7))
        }

        @Test
        fun `should filter err`() {
            expectThat(IO_LIST.error.toList()).containsExactly(IO_LIST.toList().subList(7, 8))
        }

        @Test
        fun `should filter out and err`() {
            expectThat(IO_LIST.outputAndError.toList()).containsExactly(IO_LIST.toList().subList(6, 8))
        }

        @Test
        fun `should remove ansi escape codes`() {
            expectThat(IO_LIST.ansiRemoved).not { containsEscapeSequences() }
        }

        @Test
        fun `should keep ansi escape codes`() {
            expectThat(IO_LIST.ansiKept).containsEscapeSequences()
        }

        @Test
        fun `should merge multiple types`() {
            expectThat(IO_LIST.drop(2).take(2).merge<IO>(removeEscapeSequences = true)).isEqualTo("text${LF}dump")
        }

        @Test
        fun `should merge single type`() {
            expectThat(IO_LIST.output.ansiRemoved).isEqualTo("out")
        }
    }

    companion object {
        val IO_LIST: IOSequence<IO> = IOSequence(
            Starting(CommandLine("command", "arg")),
            Meta typed "file".asPath(),
            Text("text"),
            Dump("dump"),
            Terminated(ExecMock(JavaProcessMock(MutedRenderingLogger))),
            Input typed "in",
            Output typed "out",
            Error(RuntimeException("err")),
        )
    }
}
