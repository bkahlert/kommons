package koodies.tracing.rendering

import koodies.text.LineSeparators.removeTrailingLineSeparator

public typealias Printer = (CharSequence) -> Unit

/**
 * A printer that collects the arguments of all invocations
 * and provides access to them via [toString].
 */
public class InMemoryPrinter : Printer {
    private val printed = StringBuilder()

    override fun invoke(text: CharSequence) {
        printed.appendLine(text.toString())
    }

    override fun toString(): String = printed.toString().removeTrailingLineSeparator
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
