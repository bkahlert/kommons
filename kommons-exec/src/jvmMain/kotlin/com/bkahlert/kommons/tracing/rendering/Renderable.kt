package com.bkahlert.kommons.tracing.rendering

import com.bkahlert.kommons.text.ANSI
import com.bkahlert.kommons.text.AnsiString.Companion.toAnsiString
import com.bkahlert.kommons.text.Grapheme.Companion.graphemes
import com.bkahlert.kommons.text.LineSeparators
import com.bkahlert.kommons.text.LineSeparators.isMultiline
import com.bkahlert.kommons.text.UriRegex
import com.bkahlert.kommons.text.truncateEnd
import com.github.ajalt.mordant.table.ColumnWidth
import com.github.ajalt.mordant.table.column

/**
 * Implementors of this interface gain control on
 * how it is displayed in case of limited space.
 */
public interface Renderable : CharSequence {

    /**
     * Returns a representation of this object that fits in a box with
     * the specified amount of [columns] and [rows].
     */
    public fun render(columns: Int?, rows: Int?): String

    public companion object {

        /**
         * A renderable that renders nothing.
         */
        public object NULL : Renderable, CharSequence by "" {
            override fun render(columns: Int?, rows: Int?): String = ""
            override fun toString(): String = ""
        }

        /**
         * Creates a [Renderable] from the given [value] depending on its type:
         * - if a [Renderable] is provided, it will simply be returned
         * - if `null` is provided, an empty string is rendered
         * - in all other cases a [Any.toString] based heuristic is used to render
         *   - multi-line strings are treated like a box with too exceeding columns and rows cut-off
         *   - single-line strings are wrapped if the rows are not restricted;
         *     otherwise the string is truncated from the center
         */
        public fun of(value: Any?): Renderable =
            when (value) {
                is Renderable -> value
                is Any -> of(value.toAnsiString()) { columns, rows ->
                    val ansiString = this
                    if (isMultiline()) {
                        lineSequence()
                            .let { if (rows != null) it.take(rows) else it }
                            .let {
                                if (columns != null) it.map { line ->
                                    if (Regex.UriRegex.containsMatchIn(line)) line
                                    else line.truncateEnd(columns.graphemes)
                                } else it
                            }
                            .joinToString(LineSeparators.Default) { it.toString() }
                    } else {
                        if (Regex.UriRegex.containsMatchIn(this)) this.toString()
                        else if (columns != null && rows != null) toString().truncateEnd(columns.graphemes)
                        else if (columns != null && rows == null) ANSI.terminal.render(column {
                            width = ColumnWidth.Fixed(columns)
                            cell(ansiString)
                        })
                        else this.toString()
                    }
                }

                else -> NULL
            }

        /**
         * Creates a [Renderable] from the given [value] using [render].
         *
         * [Any.toString] and [CharSequence] are implemented using the [render] invoked with `null` arguments.
         */
        public fun <T> of(value: T, render: T.(columns: Int?, rows: Int?) -> String): Renderable {
            val string = value.render(null, null)
            return object : Renderable, CharSequence by string {
                override fun render(columns: Int?, rows: Int?): String = value.render(columns, rows)
                override fun toString(): String = string
            }
        }

        /**
         * Creates a [Renderable] using the given [render].
         *
         * [Any.toString] and [CharSequence] are implemented using the [render] invoked with `null` arguments.
         */
        public operator fun invoke(render: (columns: Int?, rows: Int?) -> String): Renderable {
            val string = render(null, null)
            return object : Renderable, CharSequence by string {
                override fun render(columns: Int?, rows: Int?): String = render(columns, rows)
                override fun toString(): String = string
            }
        }
    }
}