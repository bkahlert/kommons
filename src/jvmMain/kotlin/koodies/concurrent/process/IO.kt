package koodies.concurrent.process

import koodies.concurrent.process.IO.Companion.ERASE_MARKER
import koodies.concurrent.process.IO.ERR
import koodies.concurrent.process.IO.INPUT
import koodies.concurrent.process.IO.META
import koodies.concurrent.process.IO.OUT
import koodies.exec.Process
import koodies.logging.ReturnValue
import koodies.terminal.AnsiString
import koodies.text.ANSI.Colors.brightBlue
import koodies.text.ANSI.Colors.red
import koodies.text.ANSI.Colors.yellow
import koodies.text.ANSI.Style
import koodies.text.ANSI.Style.bold
import koodies.text.ANSI.Style.dim
import koodies.text.ANSI.Style.italic
import koodies.text.LineSeparators
import koodies.text.LineSeparators.lines
import koodies.text.Semantics.Symbols
import koodies.text.Semantics.formattedAs
import koodies.text.mapLines
import koodies.time.Now
import java.nio.file.Path

/**
 * Instances are ANSI formatted output with a certain [Type].
 */
public sealed class IO(
    /**
     * Contains the originally encountered [IO].
     */
    public val text: AnsiString,
    /**
     * Formats a strings to like an output of this [Type].
     */
    private val formatAnsi: (AnsiString) -> String,
) : AnsiString(text.toString(removeEscapeSequences = false)) { // TODO check if delegation can be used

    /**
     * Contains this [text] with the format of it's [type] applied.
     */
    public val formatted: String by lazy { formatAnsi(text) }

    override fun toString(): String = formatted

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as IO

        if (text != other.text) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + text.hashCode()
        return result
    }

    /**
     * An [IO] that represents information about a [Process].
     */
    public sealed class META(text: String) : IO(text.asAnsiString(), { text.formattedAs.meta }) {
        public class STARTING(public val commandLine: CommandLine) : META("Executing ${commandLine.commandLine}")
        public class FILE(path: Path) : META("${Symbols.Document} ${path.toUri()}")
        public class TEXT(text: String) : META(text)
        public class DUMP(dump: String) : META(dump.also { require(it.contains("dump")) { "Please use ${TEXT::class.simpleName} for free-form text." } })
        public class TERMINATED(process: Process) : META("Process ${process.pid} terminated successfully at $Now."), ReturnValue by process

        public companion object {
            public infix fun typed(file: Path): FILE = FILE(file)

            public infix fun typed(text: CharSequence): TEXT =
                filter(text).toString().takeIf { it.isNotBlank() }?.let { TEXT(it) } ?: error("Non-blank string required.")
        }
    }

    /**
     * An [IO] (of another process) serving as an input.
     */
    public class INPUT(text: AnsiString) : IO(text, { text.mapLines { it.brightBlue().dim().italic() } }) {
        public companion object {
            private val EMPTY: INPUT = INPUT(AnsiString.EMPTY)

            /**
             * Factory to classify different [Type]s of [IO].
             */
            public infix fun typed(text: CharSequence): INPUT = if (text.isEmpty()) EMPTY else INPUT(filter(text).asAnsiString())
        }

        private val lines: List<INPUT> by lazy { text.lines().map { INPUT typed it }.toList() }

        /**
         * Splits this [IO] into separate lines while keeping the ANSI formatting intact.
         */
        public fun lines(): List<INPUT> = lines
    }

    /**
     * An [IO] that is neither [META], [INPUT] nor [ERR].
     */
    public class OUT(text: AnsiString) : IO(text, { text.mapLines { it.yellow() } }) {
        public companion object {
            private val EMPTY: OUT = OUT(AnsiString.EMPTY)

            /**
             * Factory to classify different [Type]s of [IO].
             */
            public infix fun typed(text: CharSequence): OUT = if (text.isEmpty()) EMPTY else OUT(filter(text).asAnsiString())
        }

        private val lines by lazy { text.lines().map { OUT typed it }.toList() }

        /**
         * Splits this [IO] into separate lines while keeping the ANSI formatting intact.
         */
        public fun lines(): List<IO> = lines
    }

    /**
     * An [IO] that represents an error.
     */
    public class ERR(text: AnsiString) : IO(text, { text.mapLines { it.red().bold() } }) {

        /**
         * Creates a new error IO from the given [exception].
         */
        public constructor(exception: Throwable) : this(exception.stackTraceToString().asAnsiString())

        public companion object {
            private val EMPTY: ERR = ERR(AnsiString.EMPTY)

            /**
             * Factory to classify different [Type]s of [IO].
             */
            public infix fun typed(text: CharSequence): ERR = if (text.isEmpty()) EMPTY else ERR(filter(text).asAnsiString())
        }
    }

    public companion object {
        /**
         * Marker that when appears at the beginning of a text
         * will be filtered out.
         */
        public val ERASE_MARKER: String = Style.hidden("﹗").toString()

        /**
         * Filters text that starts with [ERASE_MARKER].
         */
        private fun filter(text: CharSequence): CharSequence {
            fun filterText(text: CharSequence) = text.lines().mapNotNull { line ->
                line.takeUnless<CharSequence> { it.startsWith(ERASE_MARKER) }
            }.joinToString(LineSeparators.LF)
            return filterText(text)
        }
    }
}


/**
 * Contains a filtered copy only consisting of [META].
 */
public val Sequence<IO>.meta: Sequence<META> get() = filterIsInstance<META>()

/**
 * Contains a filtered copy only consisting of [META].
 */
public val Iterable<IO>.meta: Iterable<META> get() = filterIsInstance<META>()

/**
 * Contains a filtered copy only consisting of [INPUT].
 */
public val Sequence<IO>.input: Sequence<INPUT> get() = filterIsInstance<INPUT>()

/**
 * Contains a filtered copy only consisting of [INPUT].
 */
public val Iterable<IO>.input: Iterable<INPUT> get() = filterIsInstance<INPUT>()

/**
 * Contains a filtered copy only consisting of [OUT].
 */
public val Sequence<IO>.out: Sequence<OUT> get() = filterIsInstance<OUT>()

/**
 * Contains a filtered copy only consisting of [OUT].
 */
public val Iterable<IO>.out: Iterable<OUT> get() = filterIsInstance<OUT>()

/**
 * Contains a filtered copy only consisting of [ERR].
 */
public val Sequence<IO>.err: Sequence<ERR> get() = filterIsInstance<ERR>()

/**
 * Contains a filtered copy only consisting of [ERR].
 */
public val Iterable<IO>.err: Iterable<ERR> get() = filterIsInstance<ERR>()

/**
 * Contains a filtered copy of I/O merged to a string.
 */
public val Sequence<IO>.merged: String get() = merge<IO>(removeEscapeSequences = false)

/**
 * Contains a filtered copy of I/O merged to a string.
 */
public val Iterable<IO>.merged: String get() = merge<IO>(removeEscapeSequences = false)
