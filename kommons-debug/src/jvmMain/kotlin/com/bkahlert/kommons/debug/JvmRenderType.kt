package com.bkahlert.kommons.debug

import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.jvm.reflect

/**
 * Renders the type of this function to the specified [out].
 * @see renderFunctionType
 */
public actual fun Function<*>.renderFunctionTypeTo(out: StringBuilder, simplified: Boolean) {
    when (val kFunction = if (this is KFunction<*>) this else kotlin.runCatching { reflect() }.getOrNull()) {
        is KFunction<*> -> {
            val parametersByKind = kFunction.parameters.groupBy { it.kind }
            parametersByKind.getOrDefault(KParameter.Kind.INSTANCE, emptyList()).forEach {
                it.type.renderKTypeTo(out, simplified)
                out.append(".")
            }
            parametersByKind.getOrDefault(KParameter.Kind.EXTENSION_RECEIVER, emptyList()).forEach {
                it.type.renderKTypeTo(out, simplified)
                out.append(".")
            }
            kFunction.name.takeUnless { it == "<anonymous>" }?.also { out.append(it) }
            out.append("(")
            parametersByKind.getOrDefault(KParameter.Kind.VALUE, emptyList()).forEachIndexed { index: Int, param: KParameter ->
                if (index > 0) out.append(", ")
                param.type.renderKTypeTo(out, simplified)
            }
            out.append(")")
            out.append(" -> ")
            kFunction.returnType.renderKTypeTo(out, simplified)
        }
        else -> out.append("Function")
    }
}

private fun KType.renderKTypeTo(out: StringBuilder, simplified: Boolean) {
    when (val classifier = classifier) {
        null -> out.append("<unknown classifier ${this::class}>")
        else -> classifier.renderKClassifierTo(out, simplified)
    }
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
            kClass.enclosingKClass?.also { enclosingKClass ->
                out.append(enclosingKClass.simplifiedName)
                out.append('.')
            }
            out.append(kClass.simplifiedName)
        }
        else -> {
            out.append(kClass.qualifiedName?.takeUnless { objectRegex.containsMatchIn(it) } ?: "<object>")
        }
    }
}

private val KClass<*>.enclosingKClass: KClass<*>?
    get() = java.enclosingClass?.takeUnless { it.name.endsWith("Kt") }?.kotlin

private val objectRegex = "\\$\\d+$".toRegex()
