package koodies.concurrent.process

import koodies.concurrent.process.IO.Type
import koodies.concurrent.process.IO.Type.ERR
import koodies.terminal.AnsiColors.brightBlue
import koodies.terminal.AnsiColors.gray
import koodies.terminal.AnsiColors.red
import koodies.terminal.AnsiColors.yellow
import koodies.terminal.AnsiFormats.bold
import koodies.terminal.AnsiFormats.dim
import koodies.terminal.AnsiFormats.italic
import koodies.terminal.AnsiString
import koodies.text.mapLines

// TODO make sealed class and refactor types to inherited IOs
/**
 * Instances are ANSI formatted output with a certain [Type].
 */
public class IO(
    /**
     * Contains the originally encountered [IO].
     */
    public val text: AnsiString,
    /**
     * Contains the [Type] of this [IO].
     */
    public val type: Type,
) : AnsiString(text.toString(withoutAnsi = false)) {

    /**
     * Contains this [text] with the format of it's [type] applied.
     */
    public val formatted: String by lazy { type.format(this) }

    private val lines by lazy { text.lines().map { type typed it }.toList() }

    /**
     * Splits this [IO] into separate lines while keeping the ANSI formatting intact.
     */
    public fun lines(): List<IO> = lines

    override fun toString(): String = formatted

    public companion object {
        /**
         * Formats a [Throwable] as an [ERR].
         */
        public fun Throwable.format(): String = ERR.format(stackTraceToString().asAnsiString())
    }

    /**
     * Classifier for different types of [IO].
     */
    public enum class Type(
        @Suppress("unused") private val symbol: String,
        /**
         * Formats a strings to like an output of this [Type].
         */
        public val formatAnsi: (AnsiString) -> String,
    ) {

        /**
         * An [IO] that represents information about a [Process].
         */
        META("𝕄", { value -> value.mapLines { it.gray().italic() } }),

        /**
         * An [IO] (of another process) serving as an input.
         */
        IN("𝕀", { value -> value.mapLines { it.brightBlue().dim().italic() } }),

        /**
         * An [IO] that is neither [META], [IN] nor [ERR].
         */
        OUT("𝕆", { value -> value.mapLines { it.yellow() } }),

        /**
         * An [IO] that represents a errors.
         */
        ERR("𝔼", { value -> value.unformatted.mapLines { it.red().bold() } }) {
            /**
             * Factory to classify an [ERR] [IO].
             */
            public infix fun typed(value: Result<*>): IO {
                require(value.isFailure)
                val message = value.exceptionOrNull()?.stackTraceToString() ?: throw IllegalStateException("Exception was unexpectedly null")
                return IO(message.asAnsiString(), ERR)
            }
        };

        /**
         * Instance representing an empty [IO].
         */
        private val EMPTY: IO by lazy { IO(AnsiString.EMPTY, this) }

        /**
         * Factory to classify different [Type]s of [IO].
         */
        public infix fun typed(value: CharSequence?): IO = if (value?.isEmpty() == true) EMPTY else IO(value?.asAnsiString() ?: "❔".asAnsiString(), this)

        /**
         * Factory to classify different [Type]s of [IO]s.
         */
        public infix fun <T : CharSequence> typed(value: Iterable<T>): List<IO> = value.map { typed(it) }

        public infix fun formatted(string: String): String = formatAnsi(string.asAnsiString())
        public infix fun formatted(string: AnsiString): String = formatAnsi(string)
        public fun format(string: String): String = formatAnsi(string.asAnsiString())
        public fun format(string: AnsiString): String = formatAnsi(string)
    }
}
