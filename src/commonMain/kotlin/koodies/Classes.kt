package koodies

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
public inline fun <reified T : Any> T.asString(vararg properties: KProperty<*>): String =
    StringBuilder(T::class.simpleName ?: "<object>").apply {
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

/**
 * Returns a string representing `this` class and all specified [properties]
 * in the format `ClassName(name1=value1, name2=value2, ...)`.
 */
public inline fun <reified T : Any> T.asString(crossinline transform: T.() -> List<Pair<Any?, Any?>>): String =
    StringBuilder(T::class.simpleName ?: "<object>").apply {
        append(" {")
        indenting { indent ->
            indenting { propIndent ->
                transform().forEach { (key, value) ->
                    append("\n$propIndent$key = $value")
                }
            }
            append("\n$indent}")
        }
    }.toString()
