package com.bkahlert.kommons.debug

/** Exposes the [console API](https://developer.mozilla.org/en/DOM/console) to Kotlin. */
public external interface Console : kotlin.js.Console {

    /**
     * Outputs a stack trace to this [Console].
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/console/trace">console.trace()</a>
     */
    public fun trace(vararg objects: Any?)

    /**
     * Creates a new inline group with the specified [label] in this [Console] log,
     * causing any later console messages to be indented by an extra level,
     * until [Console.groupEnd] is called.
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/console/group">console.group()</a>
     */
    public fun group(label: String? = definedExternally)

    /**
     * Creates a new inline group with the specified [label] in this [Console].
     *
     * Unlike [Console.grouping], however, the new group is created collapsed.
     * The user needs to use the disclosure button next to it to expand it,
     * revealing the entries created in the group.
     *
     * Call [Console.groupEnd] to back out to the parent group.
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/console/groupCollapsed">console.groupCollapsed()</a>
     */
    public fun groupCollapsed(label: String? = definedExternally)

    /**
     * Exits the current inline group in this [Console].
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/console/groupEnd">console.groupEnd()</a>
     */
    public fun groupEnd()

    /**
     * Displays tabular [data] as a table of the specified [columns].
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/console/table">console.table()</a>
     */
    public fun table(data: Any, columns: Array<String>? = definedExternally)
}

/** Exposes the [console API](https://developer.mozilla.org/en/DOM/console) to Kotlin. */
public external val console: Console

/**
 * Creates a new inline group with the optionally specified [label] in this [Console] log,
 * causing any later console messages to be indented by an extra level,
 * until [Console.groupEnd] is called.
 *
 * In case of [Result.isSuccess] the specified [render] applied to the result is logged,
 * otherwise the thrown exception is logged as an error.
 */
public inline fun <reified R> Console.groupCatching(
    label: String? = null,
    collapsed: Boolean = true,
    render: (R) -> Any? = { it.toJson() },
    block: () -> R
): Result<R> {
    if (collapsed) label?.also { groupCollapsed(it) } ?: groupCollapsed()
    else label?.also { group(it) } ?: group()
    val result = runCatching(block)
        .onSuccess { log("${label?.let { "$label " }}returned", render(it)) }
        .onFailure { error("${label?.let { "$label " }}failed", it) }
    groupEnd()
    return result
}

/**
 * Creates a new inline group with the optionally specified [label] in this [Console] log,
 * causing any later console messages to be indented by an extra level,
 * until [Console.groupEnd] is called.
 *
 * In case of [Result.isSuccess] the specified [render] applied to the result is logged,
 * otherwise the thrown exception is logged as an error.
 */
public inline fun <reified R> Console.grouping(
    label: String? = null,
    collapsed: Boolean = true,
    render: (R) -> Any? = { it.toJson() },
    block: () -> R
): R =
    groupCatching(label, collapsed, render, block).getOrThrow()

/**
 * Prints a table containing the specified [data] converted [toJson].
 */
public fun Console.jsonTable(vararg data: Any) {
    table(data.toJsonArray())
}
