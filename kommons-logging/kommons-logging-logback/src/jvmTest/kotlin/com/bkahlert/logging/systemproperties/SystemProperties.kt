package de.dkb.api.systemproperties

import org.junit.jupiter.api.extension.ExtendWith

/**
 * Allows to annotate [SystemProperty] multiple times.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER
)
@ExtendWith(SystemPropertyExtension::class)
annotation class SystemProperties(vararg val value: SystemProperty)
