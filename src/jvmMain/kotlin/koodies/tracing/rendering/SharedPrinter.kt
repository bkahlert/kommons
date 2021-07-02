package koodies.tracing.rendering

import io.opentelemetry.api.trace.Span
import koodies.asString
import koodies.exec.IO
import koodies.text.ANSI.Formatter
import koodies.text.LineSeparators.prefixLinesWith
import koodies.text.Semantics.formattedAs
import koodies.tracing.CurrentSpan
import koodies.tracing.SpanId
import koodies.tracing.TracingDsl
import koodies.tracing.rendering.BlockStyles.Solid
import koodies.tracing.tracing

public class SharedPrinter(private val print: (CharSequence) -> Unit) : Printer {

    private var exclusive: SpanId? = null

    override fun invoke(message: CharSequence) {
        if (exclusive == null || nestedExclusive()) print(message)
    }

    // FIXME very hacky as it relies on Span.toString() to contain the parent span ID; therefor only works for one nesting level at best
    private fun nestedExclusive() = Span.current().toString().contains(exclusive.toString())

    @TracingDsl
    public fun <R> runExclusive(block: CurrentSpan.() -> R): R =
        koodies.runWrapping({ exclusive = SpanId.current }, { exclusive = null }) {
            val customize: Settings.() -> Settings = {
                copy(
                    decorationFormatter = Formatter.fromScratch { formattedAs.warning },
                    blockStyle = ::Solid,
                )
            }
            tracing(renderer = { it(customize().copy(printer = { print(it) })) }, block = block)
        }

    override fun toString(): String = asString {
        ::exclusive to exclusive
    }

    public companion object {
        public val BACKGROUND: SharedPrinter = SharedPrinter {
            val message = it.prefixLinesWith(IO.ERASE_MARKER)
            println(message)
        }
    }
}
