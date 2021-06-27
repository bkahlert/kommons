package koodies.tracing.rendering

import io.opentelemetry.context.Context
import koodies.collections.synchronizedListOf
import koodies.debug.CapturedOutput
import koodies.jvm.completableFuture
import koodies.jvm.currentThread
import koodies.test.SystemIOExclusive
import koodies.text.ANSI.Colors
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.ansiRemoved
import koodies.text.toStringMatchesCurlyPattern
import koodies.tracing.TestSpan
import koodies.tracing.rendering.SharedPrinter.Companion.BACKGROUND
import koodies.tracing.spanning
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class SharedPrinterTest {

    @Test
    fun `should log with no extras`() {
        val messages = mutableListOf<CharSequence>()
        val sharedPrinter = SharedPrinter { messages.add(it) }

        sharedPrinter("message")

        expectThat(messages.map { it.ansiRemoved.trim() }).containsExactly("message")
    }

    @Test
    fun `should log to context out`() {
        val messages = mutableListOf<CharSequence>()
        val sharedPrinter = SharedPrinter { messages.add(it) }

        BlockRenderer(Settings(printer = sharedPrinter)).log("message")

        expectThat(messages.map { it.ansiRemoved.trim() }).containsExactly("│   message")
    }

    @Test
    fun `should record log`() {
        val messages = mutableListOf<CharSequence>()
        val sharedPrinter = SharedPrinter { messages.add(it) }

        BlockRenderer(Settings(printer = sharedPrinter)).log("message 1")
        BlockRenderer(Settings(printer = sharedPrinter)).log("message 2")
        BlockRenderer(Settings(printer = sharedPrinter)).log("message 3")

        expectThat(messages.map { it.ansiRemoved.trim() }).containsExactly(
            "│   message 1",
            "│   message 2",
            "│   message 3",
        )
    }

    @Test
    fun TestSpan.`should handle concurrent access`() {
        val messages = synchronizedListOf<CharSequence>()
        val sharedPrinter = SharedPrinter { messages.add(it); log(it) }
        val executor = Context.taskWrapping(Executors.newFixedThreadPool(3))

        val result = listOf("foo", "bar", "baz").map { name ->
            executor.completableFuture {
                val color = Colors.random(120)
                spanning(name, renderer = { BlockRenderer(Settings(decorationFormatter = color, contentFormatter = color, printer = sharedPrinter)) }) {
                    (0..20).forEach { log("${currentThread.name}: $name $it") }
                }
            }

        }.toTypedArray().let { CompletableFuture.allOf(*it) }

        val color = Colors.random(20)
        spanning("main", renderer = { BlockRenderer(Settings(decorationFormatter = color, contentFormatter = color, printer = sharedPrinter)) }) {
            (0..5).forEach { log("${currentThread.name}: shared $it") }
            with(sharedPrinter) {
                runExclusive {
                    (10..15).forEach { log("exclusive $it") }
                    spanning("exclusive child") {
                        (15..20).forEach { log("exclusive child $it") }
                    }
                }
            }
            (20..25).forEach { log("${currentThread.name}: shared $it") }
        }

        result.join()

        expectThat(messages.dropWhile { !it.contains("exclusive") }.map { it.ansiRemoved.trim() }).contains(
            "│   exclusive 10",
            "│   exclusive 11",
            "│   exclusive 12",
            "│   exclusive 13",
            "│   exclusive 14",
            "│   exclusive 15",
            "│   ╭──╴exclusive child",
            "│   │",
            "│   │   exclusive child 15",
            "│   │   exclusive child 16",
            "│   │   exclusive child 17",
            "│   │   exclusive child 18",
            "│   │   exclusive child 19",
            "│   │   exclusive child 20",
        )
    }

    @Nested
    inner class Global {

        @SystemIOExclusive
        @Test
        fun `should prefix log messages with IO erase marker`(output: CapturedOutput) {
            BACKGROUND("This does not appear in the captured output.")
            BACKGROUND("But it shows on the actual console.".ansi.italic)
            println("This message is captured.")

            expectThat(output).toStringMatchesCurlyPattern("This message is captured.")
        }
    }
}
