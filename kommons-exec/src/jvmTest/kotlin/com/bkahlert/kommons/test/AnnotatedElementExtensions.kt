package com.bkahlert.kommons.test

import com.bkahlert.kommons.orNull
import com.bkahlert.kommons.runtime.ancestors
import com.bkahlert.kommons.runtime.ancestorsx
import com.bkahlert.kommons.test.junit.ancestors
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.support.AnnotationSupport
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import kotlin.reflect.KClass

/**
 * Filters this list and leaves all elements which are annotated with [A] **and** satisfy [annotationPredicate].
 *
 * The elements that are not annotated with [A] are returned depending on [notAnnotatedElementsPredicate].
 */
inline fun <reified A : Annotation> Iterable<AnnotatedElement>.withAnnotationAndDefault(
    notAnnotatedElementsPredicate: (AnnotatedElement) -> Boolean,
    ancestorsIgnored: Boolean = true,
    crossinline annotationPredicate: (A) -> Boolean,
): Set<AnnotatedElement> = filter { element ->
    val elements = if (ancestorsIgnored) listOf(element) else when (element) {
        is Method -> element.ancestorsx
        is Class<*> -> element.ancestors
        else -> throw IllegalStateException("Unexpected type $element")
    }
    val annotationType = A::class.java
    val map = elements.mapNotNull {
        val findAnnotation = AnnotationSupport.findAnnotation(it, annotationType)
        findAnnotation.orElseGet { null }
    }
    val annotation: A? = map.firstOrNull()
    annotation?.let { annotationPredicate(it) } ?: notAnnotatedElementsPredicate(element)
}.toSet()

/**
 * Filters this list and leaves only elements annotated with [A] or [A] matching [annotationPredicate].
 */
inline fun <reified A : Annotation> Iterable<AnnotatedElement>.withAnnotation(
    ancestorsIgnored: Boolean = true,
    crossinline annotationPredicate: (A) -> Boolean = { true },
): Set<AnnotatedElement> =
    withAnnotationAndDefault(notAnnotatedElementsPredicate = { false }, ancestorsIgnored = ancestorsIgnored, annotationPredicate = annotationPredicate)


/**
 * Checks if at least one of [this] elements annotations or meta annotations is of the provided type.
 */
inline fun <reified A : Annotation> AnnotatedElement?.isA(annotationPredicate: (A) -> Boolean = { true }): Boolean {
    val annotation: A? = AnnotationSupport.findAnnotation(this, A::class.java).orElseGet { null }
    return annotation?.let { annotationPredicate(it) } ?: false
}

/**
 * Checks if at least one of [this] elements annotations or meta annotations is of the provided type.
 */
inline fun <A : Annotation> AnnotatedElement?.isA(annotationClass: KClass<A>, annotationPredicate: (A) -> Boolean = { true }): Boolean {
    val annotation: A? = AnnotationSupport.findAnnotation(this, annotationClass.java).orElseGet { null }
    return annotation?.let { annotationPredicate(it) } ?: false
}


/**
 * Checks if current context is annotated with [A].
 */
@Suppress("unused")
inline fun <reified A : Annotation> ExtensionContext.isAnnotated(
    crossinline annotationPredicate: (A) -> Boolean = { true },
): Boolean = element.orNull().isA(annotationPredicate) || ancestors.any { it.element.orNull().isA(annotationPredicate) }

/**
 * Checks if current context is annotated with [A].
 */
@Suppress("unused")
inline fun <A : Annotation> ExtensionContext.isAnnotated(
    annotationClass: KClass<A>,
    crossinline annotationPredicate: (A) -> Boolean = { true },
): Boolean = element.orNull().isA(annotationClass, annotationPredicate)

inline fun <reified A : Annotation, reified T> ExtensionContext.withAnnotation(crossinline annotationPredicate: A.() -> T): T? =
    AnnotationSupport.findAnnotation(element, A::class.java).orElseGet { null }?.run(annotationPredicate)
