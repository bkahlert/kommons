package koodies.test

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.support.AnnotationSupport
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import java.util.Optional

/**
 * Filters this list and leaves all elements which are annotated with [A] **and** satisfy [annotationFilter].
 *
 * The elements that are not annotated with [A] are returned depending on [notAnnotatedElementsFilter].
 */
inline fun <reified A : Annotation> Iterable<AnnotatedElement>.withAnnotationAndDefault(
    notAnnotatedElementsFilter: (AnnotatedElement) -> Boolean,
    crossinline annotationFilter: (A) -> Boolean,
): Set<AnnotatedElement> {
    return this.filter { element ->
        val annotation: A? = AnnotationSupport.findAnnotation(element, A::class.java).orElseGet { null }
        annotation?.let { annotationFilter(it) } ?: notAnnotatedElementsFilter(element)
    }.toSet()
}

/**
 * Filters this list and leaves only elements annotated with [A] or [A] matching [annotationFilter].
 */
inline fun <reified A : Annotation> Iterable<AnnotatedElement>.withAnnotation(crossinline annotationFilter: (A) -> Boolean = { true }): Set<AnnotatedElement> =
    withAnnotationAndDefault(notAnnotatedElementsFilter = { false }, annotationFilter = annotationFilter)

/**
 * Filters this list and leaves only elements not annotated with [A] or if annotated, a matching [annotationFilter].
 */
inline fun <reified A : Annotation> Iterable<AnnotatedElement>.withoutAnnotation(crossinline annotationFilter: (A) -> Boolean = { false }): Set<AnnotatedElement> =
    withAnnotationAndDefault(notAnnotatedElementsFilter = { true }, annotationFilter = annotationFilter)


/**
 * Checks if at least one of [this] elements annotations or meta annotations is of the provided type.
 */
inline fun <reified A : Annotation> AnnotatedElement?.isA(annotationFilter: (A) -> Boolean = { true }): Boolean {
    val annotation: A? = AnnotationSupport.findAnnotation(this, A::class.java).orElseGet { null }
    return annotation?.let { annotationFilter(it) } ?: false
}

/**
 * Checks if at least one of [this] elements annotations or meta annotations is of the provided type.
 */
inline fun <reified T : Annotation> Optional<AnnotatedElement>?.isA(): Boolean =
    AnnotationSupport.isAnnotated(this, T::class.java)


@JvmName("isAMethod")
inline fun <reified T : Annotation> Optional<out Method>?.isA(): Boolean =
    AnnotationSupport.isAnnotated(this!!, T::class.java)


@JvmName("isAMethod")
inline fun <reified T : Annotation> Method?.isA(): Boolean =
    AnnotationSupport.isAnnotated(this!!, T::class.java)

/**
 * Checks if current context is annotated with [A].
 */
@Suppress("unused")
inline fun <reified A : Annotation> ExtensionContext.isAnnotated(
    crossinline annotationFilter: (A) -> Boolean = { true },
): Boolean = element { isA(annotationFilter) }

