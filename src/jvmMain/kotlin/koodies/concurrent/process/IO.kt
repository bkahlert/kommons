package koodies.concurrent.process

import koodies.concurrent.process.IO.Companion.ERASE_MARKER
import koodies.concurrent.process.IO.ERR
import koodies.concurrent.process.IO.INPUT
import koodies.concurrent.process.IO.META
import koodies.concurrent.process.IO.OUT
import koodies.exec.CommandLine
import koodies.exec.Process
import koodies.logging.ReturnValue
import koodies.text.ANSI.Style
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.AnsiString
import koodies.text.LineSeparators
import koodies.text.LineSeparators.lines
import koodies.text.Semantics.Symbols
import koodies.text.Semantics.formattedAs
import koodies.text.mapLines
import koodies.time.Now
import java.nio.file.Path

/**
 * Instances are ANSI formatted output with a certain type.
 */
public sealed class IO(
    /**
     * Contains the originally encountered [IO].
     */
    public val text: AnsiString,
    /**
     * Formats a strings to like an output of this type.
     */
    private val formatAnsi: (AnsiString) -> String,
) : AnsiString(text.toString(removeEscapeSequences = false)) {

    /**
     * Contains this [text] with the format of this type applied.
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

        /**
         * Information that a [Process] is starting.
         */
        public class STARTING(public val commandLine: CommandLine) : META("Executing ${commandLine.commandLine}")

        /**
         * Information that a [Path] is a resource used to start a [Process].
         */
        public class FILE(path: Path) : META("${Symbols.Document} ${path.toUri()}")

        /**
         * Not further specified information about a [Process].
         */
        public class TEXT(text: String) : META(text)

        /**
         * Information about a created [Process] dump.
         */
        public class DUMP(dump: String) : META(dump.also { require(it.contains("dump")) { "Please use ${TEXT::class.simpleName} for free-form text." } })

        /**
         * Information about the termination of a [Process].
         */
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
    public class INPUT(text: AnsiString) : IO(text, { text.mapLines { it.ansi.brightBlue.dim.italic.done } }) {
        public companion object {
            private val EMPTY: INPUT = INPUT(AnsiString.EMPTY)

            /**
             * Factory to classify different types of [IO].
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
    public class OUT(text: AnsiString) : IO(text, { text.mapLines { it.ansi.yellow } }) {
        public companion object {
            private val EMPTY: OUT = OUT(AnsiString.EMPTY)

            /**
             * Factory to classify different types of [IO].
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
    public class ERR(text: AnsiString) : IO(text, { text.mapLines { it.ansi.red.bold } }) {

        /**
         * Creates a new error IO from the given [exception].
         */
        public constructor(exception: Throwable) : this(exception.stackTraceToString().asAnsiString())

        public companion object {
            private val EMPTY: ERR = ERR(AnsiString.EMPTY)

            /**
             * Factory to classify different types of [IO].
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
 * Read optimized [Sequence] of [IO] that can be used
 * as a lazily populating sequence of [IO] or
 * as a means to access all encompassed [IO] merged to
 * a string—either with [ansiRemoved] or [ansiKept].
 */
public class IOSequence<out T : IO>(seq: Sequence<T>) : Sequence<T> by seq {
    public constructor(io: Iterable<T>) : this(io.asSequence())
    public constructor(vararg io: T) : this(io.asSequence())

    /**
     * Contains all encompassed [IO] merged to a string.
     *
     * Eventually existing [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) are removed.
     *
     * ***Note:** Accessing this property triggers the construction
     * of a string representing all encompassed [IO].*
     *
     * @see ansiKept
     */
    public val ansiRemoved: String by lazy { merge<IO>(removeEscapeSequences = true) }

    /**
     * Contains all encompassed [IO] merged to a string.
     *
     * Eventually existing [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) are kept.
     *
     * ***Note:** Accessing this property triggers the construction
     * of a string representing all encompassed [IO].*
     *
     * @see ansiRemoved
     */
    public val ansiKept: String by lazy { merge<IO>(removeEscapeSequences = false) }

    /**
     * Returns all encompassed [IO] merged to a string.
     *
     * Eventually existing [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) are removed.
     *
     * ***Note:** Accessing this property triggers the construction
     * of a string representing all encompassed [IO].*
     *
     * @see ansiRemoved
     * @see ansiKept
     */
    override fun toString(): String = ansiRemoved

    public companion object {
        public val EMPTY: IOSequence<IO> = IOSequence(emptySequence())
    }
}

/**
 * Contains a filtered copy only consisting of [META].
 */
public val IOSequence<IO>.meta: IOSequence<META> get() = IOSequence(filterIsInstance<META>())

/**
 * Contains a filtered copy only consisting of [INPUT].
 */
public val IOSequence<IO>.input: IOSequence<INPUT> get() = IOSequence(filterIsInstance<INPUT>())

/**
 * Contains a filtered copy only consisting of [OUT].
 */
public val IOSequence<IO>.out: IOSequence<OUT> get() = IOSequence(filterIsInstance<OUT>())

/**
 * Contains a filtered copy only consisting of [ERR].
 */
public val IOSequence<IO>.err: IOSequence<ERR> get() = IOSequence(filterIsInstance<ERR>())

/**
 * Contains a filtered copy only consisting of [OUT] and [ERR].
 */
public val IOSequence<IO>.outAndErr: IOSequence<IO> get() = IOSequence(filter { it is OUT || it is ERR })
