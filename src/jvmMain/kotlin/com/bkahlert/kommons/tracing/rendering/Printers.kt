package com.bkahlert.kommons.tracing.rendering

import io.opentelemetry.api.trace.Span
import com.bkahlert.kommons.asString
import com.bkahlert.kommons.exec.IO
import com.bkahlert.kommons.text.ANSI.Formatter
import com.bkahlert.kommons.text.ANSI.ansiRemoved
import com.bkahlert.kommons.text.LineSeparators.mapLines
import com.bkahlert.kommons.text.LineSeparators.trailingLineSeparatorRemoved
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.tracing.SpanId
import com.bkahlert.kommons.tracing.SpanScope
import com.bkahlert.kommons.tracing.TracingDsl
import com.bkahlert.kommons.tracing.rendering.Styles.Solid
import com.bkahlert.kommons.tracing.spanScope
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

public typealias Printer = (CharSequence) -> Unit

/**
 * A printer that collects the arguments of all invocations
 * and provides access to them via [toString].
 */
public class InMemoryPrinter(
    /**
     * Whether this printer is allowed to collect.
     */
    public var enabled: Boolean = true,
) : Printer {
    private val printed = StringBuffer()

    override fun invoke(text: CharSequence) {
        if (enabled) printed.appendLine(text.toString())
    }

    override fun toString(): String = printed.toString().trailingLineSeparatorRemoved
}

/**
 * A printer forwards the argument of each invocation to all specified [printers].
 */
public class TeePrinter(vararg printers: Printer, printer: Printer = {}) : Printer {

    private val printers = arrayOf(*printers, printer)

    override fun invoke(text: CharSequence) {
        printers.forEach { it.invoke(text) }
    }
}

/**
 * That printer that wraps the given [delegate] in order to make it thread-safe.
 */
public class ThreadSafePrinter(private val delegate: Printer) : Printer {
    private val lock = ReentrantLock()
    override fun invoke(text: CharSequence): Unit = lock.withLock { delegate.invoke(text) }
}

/**
 * A printer that can be used by multiple processes concurrently.
 */
public open class SharedPrinter(private val print: (CharSequence) -> Unit) : Printer {

    private var exclusive: SpanId? = null

    override fun invoke(message: CharSequence) {
        if (exclusive == null || nestedExclusive()) print(message)
    }

    // FIXME very hacky as it relies on Span.toString() to contain the parent span ID; therefore only works for one nesting level at best
    private fun nestedExclusive() = Span.current().toString().contains(exclusive.toString())

    @TracingDsl
    public fun <R> runExclusive(block: SpanScope.() -> R): R =
        com.bkahlert.kommons.runWrapping({ exclusive = SpanId.current }, { exclusive = null }) {
            val customize: Settings.() -> Settings = {
                copy(
                    decorationFormatter = Formatter.fromScratch { formattedAs.warning },
                    style = Solid,
                )
            }
            spanScope(renderer = { it.create(customize().copy(printer = { print(it) })) }, block = block)
        }

    override fun toString(): String = asString {
        ::exclusive to exclusive
    }
}

/**
 * A special [SharedPrinter] that logs to the background:
 * - the output to specifically formatted to be distinguishable from regular log messages
 * - the output is not captured when testing
 */
public object BackgroundPrinter : Printer {

    /**
     * Printer used to print background messages.
     */
    public var printer: Printer = ::println

    override fun invoke(message: CharSequence) {
//        val xxx = if (message.contains("✔︎")) message.toString().replace("✔︎", "✔︎ — " + currentStackTrace.joinToString(" ||| ")) else message
//        val backgroundMessage = xxx.mapLines { "${IO.ERASE_MARKER}${it.ansiRemoved.formattedAs.meta}" }
        val backgroundMessage = message.mapLines { "${IO.ERASE_MARKER}${it.ansiRemoved.formattedAs.meta}" }
        printer(backgroundMessage)
    }
}
