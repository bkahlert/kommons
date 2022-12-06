package com.bkahlert.kommons.debug

import com.bkahlert.kommons.debug.Compression.Always
import com.bkahlert.kommons.debug.Typing.Untyped
import com.bkahlert.kommons.quoted
import kotlin.reflect.KProperty

/**
 * Returns a string representing this object
 * and the properties specified by [include] (default: all)
 * with properties excluded as specified by [excludeNullValues]
 * and [exclude].
 */
public fun <T : Any> T.asString(
    vararg include: KProperty<*>,
    excludeNullValues: Boolean = true,
    exclude: Collection<KProperty<*>> = emptyList(),
): String {
    val receiver = this
    return asString(excludeNullValues, exclude.map { it.name }) {
        if (include.isEmpty()) putAll(receiver.properties)
        else include.forEach { prop ->
            prop.getOrNull(receiver)?.also { put(prop.name, it) }
        }
    }
}

/**
 * Returns a string representing this object
 * and the properties specified by [include] (default: all)
 * with properties excluded as specified by [excludeNullValues]
 * and [exclude].
 */
public fun <T : Any> T.asString(
    excludeNullValues: Boolean = true,
    exclude: Collection<String> = emptyList(),
    include: MutableMap<Any, Any?>.() -> Unit,
): String {
    val properties = buildMap(include).mapKeys { (key, _) ->
        when (key) {
            is CharSequence -> key.quoted.removeSurrounding("\"")
            is KProperty<*> -> key.name
            else -> key.render { compression = Always }
        }
    }
    val renderedType = renderType()
    val rendered = properties.render {
        typing = Untyped
        filterProperties { receiver, prop ->
            (!excludeNullValues || receiver != null) && !exclude.contains(prop)
        }
    }
    return buildString {
        append(renderedType)
        append(" ")
        append(rendered.removePrefix(renderedType))
    }
}
