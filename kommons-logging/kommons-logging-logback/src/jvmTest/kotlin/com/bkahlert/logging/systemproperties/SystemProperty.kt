package de.dkb.api.systemproperties

import org.junit.jupiter.api.extension.ExtendWith
import java.lang.annotation.Repeatable

/**
 * Use this annotation to set a proper system property for the scope
 * of the annotated test class or method.
 *
 * Its functionality is implemented by [SystemPropertyExtension] which is globally
 * registered using service locator `META-INF/services/org.junit.jupiter.api.extension.Extension`.
 *
 * *JUnit explicitly requires [Repeatable] (in contrast to [kotlin.annotation.Repeatable]).*
 */
@Suppress("DEPRECATED_JAVA_ANNOTATION")
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER
)
@Repeatable(SystemProperties::class)
@ExtendWith(SystemPropertyExtension::class)
annotation class SystemProperty(val name: String, val value: String)
