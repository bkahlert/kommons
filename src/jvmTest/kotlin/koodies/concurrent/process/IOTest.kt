package koodies.concurrent.process

import koodies.io.path.asPath
import koodies.logging.MutedRenderingLogger
import koodies.process.JavaProcessMock
import koodies.process.ManagedProcessMock
import koodies.process.ProcessExitMock
import koodies.test.toStringIsEqualTo
import koodies.text.Semantics
import koodies.text.containsEscapeSequences
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
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
                expectThat(meta.text).toStringIsEqualTo("${Semantics.Document} ${file.toUri()}")
            }

            @Test
            fun `should have formatted text`() {
                expectThat(meta).containsEscapeSequences().toStringIsEqualTo("${Semantics.Document} ${file.toUri()}")
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

            private val process = ManagedProcessMock(JavaProcessMock(MutedRenderingLogger()) { ProcessExitMock.immediateSuccess() })
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

        private val `in` = IO.IN typed "in"

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
}
