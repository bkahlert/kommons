package koodies.exec

import koodies.exec.IO.Meta
import koodies.exec.IO.Meta.Starting
import koodies.io.path.pathString
import koodies.io.path.text
import koodies.io.path.writeText
import koodies.junit.UniqueId
import koodies.jvm.daemon
import koodies.test.toStringIsEqualTo
import koodies.test.withTempDir
import koodies.text.LineSeparators.LF
import koodies.text.ansiRemoved
import koodies.text.containsAnsi
import koodies.time.poll
import koodies.time.seconds
import koodies.time.sleep
import koodies.unit.milli
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.contains
import strikt.assertions.filter
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNotEmpty
import strikt.assertions.single
import strikt.assertions.startsWith
import java.io.IOException
import java.nio.file.Path

class IOLogTest {

    @Test
    fun `should provide thread-safe access to log`() {
        var stop = false
        val ioLog = IOLog()
        daemon {
            var i = 0
            while (!stop) {
                ioLog + (Meta typed "being busy $i times")
                10.milli.seconds.sleep()
                i++
            }
        }

        poll { ioLog.count() > 0 }.every(10.milli.seconds).forAtMost(1.seconds) { fail("No I/O logged in one second.") }

        expectThat(ioLog.toList()) {
            isNotEmpty()
            contains(Meta typed "being busy 0 times")
        }
        stop = true
    }

    @Test
    internal fun `should provide filtered access`() {
        val ioLog = createIOLog()

        expectThat(ioLog.merge<IO.Output>()).isEqualTo("""
            processing
            awaiting input: 
        """.trimIndent())
    }

    @Nested
    inner class DumpIO {

        private val ioLog = createIOLog()

        @Test
        fun `should dump IO to specified directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val dumps: Map<String, Path> = ioLog.dump(this, 123)
            expectThat(dumps.values) {
                hasSize(2)
                all { text.ansiRemoved.startsWith("Executing command arg") }
            }
        }

        @Test
        fun `should throw if IO could not be dumped`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val logPath = resolve("koodies.exec.123.log").writeText("already exists")
            logPath.toFile().setReadOnly()
            expectCatching { ioLog.dump(this, 123) }.isFailure().isA<IOException>()
            logPath.toFile().setWritable(true)
        }

        @Test
        fun `should dump IO to file with ansi formatting`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val dumps = ioLog.dump(this, 123).values
            expectThat(dumps).filter { !it.pathString.endsWith("ansi-removed.log") }
                .single().text
                .containsAnsi()
                .toStringIsEqualTo("""
                    Executing command arg
                    processing
                    awaiting input: 
                    cancel
                    invalid input
                    an abnormal error has occurred (errno 99)
                """.trimIndent())
        }

        @Test
        fun `should dump IO to file without ansi formatting`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val dumps = ioLog.dump(this, 123).values
            expectThat(dumps).filter { it.pathString.endsWith("ansi-removed.log") }
                .single().text
                .not { containsAnsi() }
                .toStringIsEqualTo("""
                    Executing command arg
                    processing
                    awaiting input: 
                    cancel
                    invalid input
                    an abnormal error has occurred (errno 99)
                """.trimIndent())
        }
    }
}

fun createIOLog(): IOLog = IOLog().apply {
    this + Starting(CommandLine("command", "arg"))
    output + "processing$LF".toByteArray()
    output + "awaiting input: $LF".toByteArray()
    input + "cancel$LF".toByteArray()
    error + "invalid input$LF".toByteArray()
    error + "an abnormal error has occurred (errno 99)$LF".toByteArray()
}
