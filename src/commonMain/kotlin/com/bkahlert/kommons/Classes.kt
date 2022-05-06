package com.bkahlert.kommons

import com.bkahlert.kommons.collections.map
import com.bkahlert.kommons.text.ANSI.ansiRemoved
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.LineSeparators.isMultiline
import com.bkahlert.kommons.text.Semantics
import com.bkahlert.kommons.text.Semantics.FieldDelimiters
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.text.spaced
import com.bkahlert.kommons.text.splitPascalCase
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

private var indent: Int = 0
public fun <T> T.indenting(block: T.(String) -> Unit) {
    runWrapping({ indent++ }, { indent-- }) {
        val prefix = "    ".repeat(indent.coerceAtLeast(0))
        block(prefix)
    }
}


/**
 * The [KClass.simpleName] of this class written as single words
 * with lower-case letters and `<object>` if a name is missing.
 */
public val KClass<*>.lowerSentenceCaseName: String
    get() = simpleName?.splitPascalCase()?.joinToString(" ") ?: error("<object>")

/**
 * Returns a string representing `this` class and all specified [properties]
 * in the format `ClassName(name1=value1, name2=value2, …)`.
 */
public fun <T : Any> T.asString(vararg properties: KProperty<*>): String =
    asString {
        properties.forEach { property ->
            put(property.name, when (property) {
                is KProperty0<*> -> kotlin.runCatching { property.get() }.recover { "<$it>" }.getOrThrow()
                is KProperty1<*, *> -> kotlin.runCatching {
                    @Suppress("UNCHECKED_CAST") val typedProperty = property as KProperty1<T, *>
                    typedProperty.get(this@asString)
                }.recover { "<$it>" }.getOrThrow()
                else -> "<unexpected property type ${toSimpleClassName()}>"
            }
            )
        }
    }

private val brackets get() = Semantics.BlockDelimiters.BLOCK.map { it.formattedAs.meta }
private fun StringBuilder.open() = append(brackets.first)
private fun StringBuilder.close() = append(brackets.second)

/**
 * Returns a string representing `this` class and all specified [Map.entries]
 * built by the given [init] in the format `ClassName(name1=value1, name2=value2, …)`.
 */
public fun Any.asString(
    className: String = toSimpleClassName(),
    maxLineLength: Int = 160,
    init: MutableMap<Any?, Any?>.() -> Unit,
): String = buildString {
    append(className)
    val content = StringBuilder()
    val flatContent = mutableListOf<String>()
    indenting { indentString ->
        indenting { propIndent ->
            buildMap(init).forEach { (key, value) ->
                val keyString = key.let { it as? KProperty<*> }?.name ?: key.toString()
                val valueString = value.toString()
                content.appendMultiline(propIndent, keyString, valueString)
                flatContent.add("$keyString = $valueString")
            }
        }
//        if (flatContent.none { it.isMultiline } && flatContent.sumOf { it.length } <= maxLineLength) {
        if (flatContent.none { it.isMultiline }) {
            content.clear()
            if (flatContent.isNotEmpty()) {
                append(" ")
                open()
                append(" ")
                flatContent.joinTo(this@buildString, FieldDelimiters.FIELD.spaced)
                append(" ")
                close()
            }
        } else {
            flatContent.clear()
            append(" ")
            open()
            append(content)
            append(LF)
            append(indentString)
            close()
        }
    }
}

private fun StringBuilder.appendMultiline(propIndent: String, keyString: String, valueString: String) {
    val keyIndentString = "$propIndent$keyString = "
    val keyIndentStringMultiline = " ".repeat(keyIndentString.ansiRemoved.length)
    append(LF)
    append(keyIndentString)
    valueString.lines().forEachIndexed { index, line ->
        if (index == 0) {
            append(line)
        } else {
            append(LF)
            append(keyIndentStringMultiline)
            append(line)
        }
    }
}
