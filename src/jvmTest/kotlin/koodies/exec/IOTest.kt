package koodies.exec

import koodies.exec.IO.Error
import koodies.exec.IO.Input
import koodies.exec.IO.Meta
import koodies.exec.IO.Meta.Dump
import koodies.exec.IO.Meta.Text
import koodies.exec.IO.Output
import koodies.test.toStringIsEqualTo
import koodies.text.LineSeparators.LF
import koodies.text.Unicode.characterTabulation
import koodies.text.containsAnsi
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
        inner class Text {

            private val text = "text"
            private val meta = Text(text)

            @Test
            fun `should have original text`() {
                expectThat(meta.text).toStringIsEqualTo(text)
            }

            @Test
            fun `should have formatted text`() {
                expectThat(meta).containsAnsi().toStringIsEqualTo(text)
            }

            @Test
            fun `should throw on blank`() {
                expectCatching { Meta typed "    " }.isFailure()
            }
        }

        @Nested
        inner class Dump {

            private val dump = "dump"
            private val meta = Dump(dump)

            @Test
            fun `should have original text`() {
                expectThat(meta.text).toStringIsEqualTo(dump)
            }

            @Test
            fun `should have formatted text`() {
                expectThat(meta).containsAnsi().toStringIsEqualTo(dump)
            }

            @Test
            fun `should throw on non-dump`() {
                expectCatching { Dump("whatever") }.isFailure()
            }
        }
    }

    @Nested
    inner class Input {

        private val `in` = Input typed "in"

        @Test
        fun `should have original text`() {
            expectThat(`in`.text).toStringIsEqualTo("in")
        }

        @Test
        fun `should have formatted text`() {
            expectThat(`in`).containsAnsi().toStringIsEqualTo("in")
        }
    }

    @Nested
    inner class Output {

        private val out = Output typed "out"

        @Test
        fun `should have original text`() {
            expectThat(out.text).toStringIsEqualTo("out")
        }

        @Test
        fun `should have formatted text`() {
            expectThat(out).containsAnsi().toStringIsEqualTo("out")
        }
    }

    @Nested
    inner class Error {

        private val err = Error(RuntimeException("err"))

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
            expectThat(err).containsAnsi().toStringMatchesCurlyPattern("""
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
            expectThat(IO_LIST.meta.toList()).containsExactly(IO_LIST.toList().subList(0, 2))
        }

        @Test
        fun `should filter in`() {
            expectThat(IO_LIST.input.toList()).containsExactly(IO_LIST.toList().subList(2, 3))
        }

        @Test
        fun `should filter out`() {
            expectThat(IO_LIST.output.toList()).containsExactly(IO_LIST.toList().subList(3, 4))
        }

        @Test
        fun `should filter err`() {
            expectThat(IO_LIST.error.toList()).containsExactly(IO_LIST.toList().subList(4, 5))
        }

        @Test
        fun `should filter out and err`() {
            expectThat(IO_LIST.outputAndError.toList()).containsExactly(IO_LIST.toList().subList(3, 5))
        }

        @Test
        fun `should remove ansi escape codes`() {
            expectThat(IO_LIST.ansiRemoved).not { containsAnsi() }
        }

        @Test
        fun `should keep ansi escape codes`() {
            expectThat(IO_LIST.ansiKept).containsAnsi()
        }

        @Test
        fun `should merge multiple types`() {
            expectThat(IO_LIST.take(2).merge<IO>(removeAnsi = true)).isEqualTo("text${LF}dump")
        }

        @Test
        fun `should merge single type`() {
            expectThat(IO_LIST.output.ansiRemoved).isEqualTo("out")
        }
    }

    companion object {
        val IO_LIST: IOSequence<IO> = IOSequence(
            Text("text"),
            Dump("dump"),
            Input typed "in",
            Output typed "out",
            Error(RuntimeException("err")),
        )
    }
}
