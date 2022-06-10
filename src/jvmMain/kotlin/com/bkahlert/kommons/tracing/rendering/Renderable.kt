package com.bkahlert.kommons.tracing.rendering

import com.bkahlert.kommons.LineSeparators
import com.bkahlert.kommons.regex.RegularExpressions.uriRegex
import com.bkahlert.kommons.text.AnsiString.Companion.toAnsiString
import com.bkahlert.kommons.text.LineSeparators.isMultiline
import com.bkahlert.kommons.text.LineSeparators.wrapLines
import com.bkahlert.kommons.text.truncateByColumns

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
                    if (isMultiline) {
                        lineSequence()
                            .let { if (rows != null) it.take(rows) else it }
                            .let {
                                if (columns != null) it.map { line ->
                                    if (uriRegex.containsMatchIn(line)) line
                                    else line.truncateByColumns(columns)
                                } else it
                            }
                            .joinToString(LineSeparators.Default) { it.toString() }
                    } else {
                        if (uriRegex.containsMatchIn(this)) this.toString()
                        else if (columns != null && rows != null) truncateByColumns(columns).toString()
                        else if (columns != null && rows == null) wrapLines(columns).toString()
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
