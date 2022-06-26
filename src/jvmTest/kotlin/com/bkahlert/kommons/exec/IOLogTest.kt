package com.bkahlert.kommons.exec

import com.bkahlert.kommons.LineSeparators.LF
import com.bkahlert.kommons.exec.IO.Meta
import com.bkahlert.kommons.io.path.textContent
import com.bkahlert.kommons.io.path.writeText
import com.bkahlert.kommons.runtime.daemon
import com.bkahlert.kommons.test.AnsiRequiring
import com.bkahlert.kommons.test.junit.SimpleId
import com.bkahlert.kommons.test.toStringIsEqualTo
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.ansiRemoved
import com.bkahlert.kommons.text.containsAnsi
import com.bkahlert.kommons.time.poll
import com.bkahlert.kommons.time.sleep
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
import kotlin.io.path.pathString
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class IOLogTest {

    @Test
    fun `should provide thread-safe access to log`() {
        var stop = false
        val ioLog = IOLog()
        daemon {
            var i = 0
            while (!stop) {
                ioLog + (Meta typed "being busy $i times")
                10.milliseconds.sleep()
                i++
            }
        }

        poll { ioLog.count() > 0 }.every(10.milliseconds).forAtMost(1.seconds) { fail("No I/O logged in one second.") }

        expectThat(ioLog.toList()) {
            isNotEmpty()
            contains(Meta typed "being busy 0 times")
        }
        stop = true
    }

    @Test
    internal fun `should provide filtered access`() {
        val ioLog = createIOLog()

        expectThat(ioLog.merge<IO.Output>()).isEqualTo(
            """
            processing
            awaiting input:${" "}
        """.trimIndent()
        )
    }

    @Nested
    inner class DumpIO {

        private val ioLog = createIOLog()

        @Test
        fun `should dump IO to specified directory`(simpleId: SimpleId) = withTempDir(simpleId) {
            val dumps: Map<String, Path> = ioLog.dump(this, 123)
            expectThat(dumps.values) {
                hasSize(2)
                all { textContent.ansiRemoved.startsWith("processing") }
            }
        }

        @Test
        fun `should throw if IO could not be dumped`(simpleId: SimpleId) = withTempDir(simpleId) {
            val logPath = resolve("kommons.exec.123.log").writeText("already exists")
            logPath.toFile().setReadOnly()
            expectCatching { ioLog.dump(this, 123) }.isFailure().isA<IOException>()
            logPath.toFile().setWritable(true)
        }

        @AnsiRequiring @Test
        fun `should dump IO to file with ansi formatting`(simpleId: SimpleId) = withTempDir(simpleId) {
            val dumps = ioLog.dump(this, 123).values
            expectThat(dumps).filter { !it.pathString.endsWith("ansi-removed.log") }
                .single().textContent
                .containsAnsi()
                .toStringIsEqualTo(
                    """
                    processing
                    awaiting input:${" "}
                    cancel
                    invalid input
                    an abnormal error has occurred (errno 99)
                """.trimIndent()
                )
        }

        @Test
        fun `should dump IO to file without ansi formatting`(simpleId: SimpleId) = withTempDir(simpleId) {
            val dumps = ioLog.dump(this, 123).values
            expectThat(dumps).filter { it.pathString.endsWith("ansi-removed.log") }
                .single().textContent
                .not { containsAnsi() }
                .toStringIsEqualTo(
                    """
                    processing
                    awaiting input:${" "}
                    cancel
                    invalid input
                    an abnormal error has occurred (errno 99)
                """.trimIndent()
                )
        }
    }
}

fun createIOLog(): IOLog = IOLog().apply {
    output + "processing$LF".toByteArray()
    output + "${"awaiting input:".ansi.blue} $LF".toByteArray()
    input + "cancel$LF".toByteArray()
    error + "invalid input$LF".toByteArray()
    error + "an abnormal error has occurred (errno 99)$LF".toByteArray()
}
