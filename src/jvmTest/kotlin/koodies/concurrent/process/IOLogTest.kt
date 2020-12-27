package koodies.concurrent.process

import koodies.concurrent.daemon
import koodies.io.file.writeText
import koodies.io.path.asString
import koodies.io.path.hasContent
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.test.withTempDir
import koodies.time.poll
import koodies.time.sleep
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
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
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class IOLogTest {

    @Test
    fun `should provide thread-safe access to log`() {
        var stop = false
        val ioLog = IOLog()
        daemon {
            var i = 0
            while (!stop) {
                ioLog.add(IO.Type.META, "being busy $i times\n".toByteArray())
                10.milliseconds.sleep()
                i++
            }
        }

        poll { ioLog.logged.isNotEmpty() }.every(10.milliseconds).forAtMost(1.seconds) { fail("No I/O logged in one second.") }

        expectThat(ioLog.logged) {
            isNotEmpty()
            contains(IO.Type.META typed "being busy 0 times")
        }
        stop = true
    }

    @Nested
    inner class DumpIO {

        private val ioLog = createIOLog()

        @Test
        fun `should dump IO to specified directory`() = withTempDir {
            val dumps: Map<String, Path> = ioLog.dump(this, 123)
            expectThat(dumps.values.map { it.readText().removeEscapeSequences() }).hasSize(2).all {
                isEqualTo("""
                Starting process...
                processing
                awaiting input: 
                cancel
                invalid input
                an abnormal error has occurred (errno 99)
            """.trimIndent())
            }
        }

        @Test
        fun `should throw if IO could not be dumped`() = withTempDir {
            val logPath = resolve("koodies.process.123.log").writeText("already exists")
            logPath.toFile().setReadOnly()
            expectCatching { ioLog.dump(this, 123) }.isFailure().isA<IOException>()
            logPath.toFile().setWritable(true)
        }

        @Test
        fun `should dump IO to file with ansi formatting`() = withTempDir {
            val dumps = ioLog.dump(this, 123).values
            expectThat(dumps).filter { !it.asString().endsWith("no-ansi.log") }.single().hasContent("""
                ${IO.Type.META.format("Starting process...")}
                ${IO.Type.OUT.format("processing")}
                ${IO.Type.OUT.format("awaiting input: ")}
                ${IO.Type.IN.format("cancel")}
                ${IO.Type.ERR.format("invalid input")}
                ${IO.Type.ERR.format("an abnormal error has occurred (errno 99)")}
            """.trimIndent())
        }

        @Test
        fun `should dump IO to file without ansi formatting`() = withTempDir {
            val dumps = ioLog.dump(this, 123).values
            expectThat(dumps).filter { it.asString().endsWith("no-ansi.log") }.single().hasContent("""
                Starting process...
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
    add(IO.Type.META, "Starting process...\n".toByteArray())
    add(IO.Type.OUT, "processing\n".toByteArray())
    add(IO.Type.OUT, "awaiting input: \n".toByteArray())
    add(IO.Type.IN, "cancel\n".toByteArray())
    add(IO.Type.ERR, "invalid input\n".toByteArray())
    add(IO.Type.ERR, "an abnormal error has occurred (errno 99)\n".toByteArray())
}
