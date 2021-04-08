package koodies

import koodies.builder.Init
import koodies.builder.buildMap
import koodies.builder.context.MapBuildingContext
import koodies.collections.map
import koodies.text.LineSeparators.isMultiline
import koodies.text.Semantics
import koodies.text.Semantics.Symbols
import koodies.text.Semantics.formattedAs
import koodies.text.spaced
import koodies.text.splitPascalCase
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

private var indent: Int = 0
public fun <T> T.indenting(block: T.(String) -> Unit) {
    runWrapping({ indent++ }, { indent-- }) {
        val prefix = "    ".repeat(indent)
        block(prefix)
    }
}

/**
 * Returns a string representing `this` class and all specified [properties]
 * in the format `ClassName(name1=value1, name2=value2, ...)`.
 */
public fun <T : Any> T.asString(vararg properties: KProperty<*>): String =
    StringBuilder(this::class.simpleName ?: "<object>").apply {
        properties.joinTo(this, prefix = "(", postfix = ")") { property ->
            val value: Any? = when (property) {
                is KProperty0<*> -> kotlin.runCatching { property.get() }.recover { "<$it>" }.getOrThrow()
                is KProperty1<*, *> -> kotlin.runCatching {
                    @Suppress("UNCHECKED_CAST") val typedProperty = property as KProperty1<T, *>
                    typedProperty.get(this@asString)
                }.recover { "<$it>" }.getOrThrow()
                else -> "<unexpected property type ${property::class.simpleName}>"
            }

            "${property.name}=$value"
        }
    }.toString()

private fun Any?.isOtherThanOrErrorMessage(alreadySeen: Any?): Any? =
    this?.let {
        if (this === alreadySeen) listOf(Symbols.Error, "LOOP DETECTED FOR".formattedAs.error, this::class.simpleName.formattedAs.input, "(", hashCode(), ")")
            .joinToString(" ")
        else this.also { println("this is not null") }
    } ?: this

private val brackets = Semantics.Markers.block.map { it.formattedAs.meta }
private fun StringBuilder.open() = append(brackets.first)
private fun StringBuilder.close() = append(brackets.second)

/**
 * Returns a string representing `this` class and all specified [properties]
 * in the format `ClassName(name1=value1, name2=value2, ...)`.
 */
public fun Any.asString(
    className: String = this::class.simpleName ?: "<object>",
    init: Init<MapBuildingContext<Any?, Any?>>,
): String =
    StringBuilder(className).apply {
        append(" ")
        open()
        val content = StringBuilder()
        val flatContent = mutableListOf<String>()
        indenting { indent ->
            indenting { propIndent ->
                buildMap(init).forEach { (key, value) ->
                    val keyString = key.let { it as? KProperty<*> }?.name ?: key.toString()
                    val valueString = value.toString()
                    content.append("\n$propIndent$keyString = $valueString")
                    flatContent.add("$keyString = $valueString")
                }
            }
            if (flatContent.none { it.isMultiline }) {
                content.clear()
                append(" ")
                append(flatContent.joinToString(Symbols.Delimiter.spaced))
                append(" ")
            } else {
                flatContent.clear()
                append(content)
                append("\n$indent")
            }
            close()
        }
    }.toString()

/**
 * The [KClass.simpleName] of this class written as single words
 * with lower-case letters and `<object>` if a name is missing.
 */
public val KClass<*>.lowerSentenceCaseName: String
    get() = simpleName?.splitPascalCase()?.joinToString(" ") ?: error("<object>")
