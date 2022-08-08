package com.bkahlert.kommons.debug

import com.bkahlert.kommons.text.EMPTY
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KType

/**
 * Renders the type of this function to the specified [out].
 * @see renderFunctionType
 */
public actual fun Function<*>.renderFunctionTypeTo(out: StringBuilder, simplified: Boolean) {
    out.append("Function")
}

/**
 * Renders this [KClassifier] to the specified [out].
 * @see renderKClassifier
 */
internal actual fun KClassifier.renderKClassifierTo(out: StringBuilder, simplified: Boolean) {

    val kClass = when (this) {
        is KClass<*> -> this
        is KType -> this.classifier as? KClass<out Any>
        else -> null
    }

    when {
        kClass == null -> {
            out.append("<unknown classifier ${this::class}>")
        }

        simplified -> {
            out.append(kClass.simplifiedName)
        }

        else -> {
            out.append(kClass.js.name.let {
                if (objectRegex.containsMatchIn(it)) "<object>"
                else if (implRegex.containsMatchIn(it)) it.replace(implRegex, String.EMPTY)
                else it
            })
        }
    }
}

private val objectRegex = "\\$\\d+$".toRegex()
private val implRegex = "_\\d+$".toRegex()
