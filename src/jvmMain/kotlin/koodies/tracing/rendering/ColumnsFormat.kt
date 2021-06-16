package koodies.tracing.rendering

import koodies.text.Semantics.formattedAs
import koodies.tracing.Span
import koodies.tracing.Span.AttributeKeys
import kotlin.math.floor

/**
 * Specialized component that focuses on computing column-based layouts.
 */
public data class ColumnsFormat(

    /**
     * The columns to be used. Each pair describes a column's width and label.
     *
     * By default an 80 units wide column having the label [AttributeKeys.Description]
     * is assumed.
     */
    public val columnSpecs: List<Pair<Int, CharSequence>> = listOf(80 to Span.Description),

    /**
     * The gap between two neighbouring columns.
     */
    public val gap: Int = 5,

    /**
     * Sum of all columns and gaps. If specified explicitly, the provided columns are scaled accordingly
     * to meet this value.
     */
    public val maxColumns: Int = columnSpecs.sum(gap),
) {

    public constructor(

        /**
         * The columns to be used. Each pair describes a column's width and label.
         *
         * By default an 80 units wide column having the label [AttributeKeys.Description]
         * is assumed.
         */
        vararg columnSpecs: Pair<Int, CharSequence> = arrayOf(80 to Span.Description),

        /**
         * The gap between two neighbouring columns.
         */
        gap: Int = 5,

        /**
         * Sum of all columns and gaps. If specified explicitly, the provided columns are scaled accordingly
         * to meet this value.
         */
        maxColumns: Int = columnSpecs.sum(gap),
    ) : this(columnSpecs.toList(), gap, maxColumns)

    init {
        require(columnSpecs.isNotEmpty()) { "At least one column must be specified." }
    }

    /**
     * Contains the [columnSpecs] scaled proportionally.
     */
    public val scaled: Map<CharSequence, Int> = run {
        if (columnSpecs.isEmpty()) emptyMap()
        else {
            val gaps = (columnSpecs.size - 1).coerceAtLeast(0) * gap
            val requestedColumns = columnSpecs.sumOf { it.first }
            val factor = (maxColumns - gaps).toDouble() / requestedColumns
            val distributed = columnSpecs.map { (key, value) -> value to floor(key * factor).toInt() }.toMutableList()
            val actualColumns = distributed.sumOf { (_, c) -> c } + gaps
            distributed[0] = distributed[0].let { (name, c) -> name to c + maxColumns - actualColumns }
            distributed.toMap()
        }
    }

    /**
     * Extracts the column labels contained in [columnSpecs] from the given [description] and [attributes]
     * mapped to their scaled column size. Labels not present in the given [attributes] are mapped to `null`.
     */
    public fun extract(description: CharSequence, attributes: Map<CharSequence, CharSequence>): List<Pair<CharSequence?, Int>> =
        scaled.map { (label, width) ->
            when {
                label == Span.Description -> description to width
                attributes.containsKey(label) -> attributes[label] to width
                else -> null to width
            }
        }

    /**
     * Returns a copy of this configuration [columns] narrower.
     *
     * Only the left column is reduced in width so all columns stay aligned.
     * If no more columns are available, a [IllegalArgumentException] is thrown.
     */
    public fun shrinkBy(columns: Int): ColumnsFormat {
        val firstColumn = columnSpecs.first().first
        require(columns < firstColumn) { "Columns cannot be shrinked by ${columns.formattedAs.input} as the leftmost column is only $firstColumn wide." }
        val mapIndexed: List<Pair<Int, CharSequence>> = columnSpecs.mapIndexed { index, (c, l) ->
            if (index == 0) c - columns to l
            else c to l
        }
        return ColumnsFormat(mapIndexed, gap, maxColumns - columns)
    }

    private companion object {
        private fun List<Int>.sum(gap: Int): Int =
            sumOf { it } + (size - 1).coerceAtLeast(0) * gap

        @JvmName("pairSum?")
        private fun List<Pair<Int, Any?>>.sum(gap: Int): Int =
            map { it.first }.sum(gap)

        private fun Array<out Pair<Int, Any?>>.sum(gap: Int): Int =
            map { it.first }.sum(gap)
    }
}
