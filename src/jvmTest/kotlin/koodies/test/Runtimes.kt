package koodies.test

import koodies.test.Slow.Companion.NAME
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

/**
 * JUnit 5 annotation to denote slow that, which may take up to 2 minutes.
 */
@Timeout(2, unit = TimeUnit.MINUTES)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
annotation class TwoMinutesTimeout

/**
 * JUnit 5 annotation to denote slow that, which may take up to 5 minutes.
 */
@Timeout(5, unit = TimeUnit.MINUTES)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
annotation class FiveMinutesTimeout

/**
 * JUnit 5 annotation to denote slow that, which may take up to 2 minutes.
 */
@TwoMinutesTimeout
@Tag(NAME)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
annotation class Slow {
    companion object {
        const val NAME = "Slow"
    }
}

/**
 * JUnit 5 annotation to denote slow that, which may take up to 15 minutes.
 */
@Timeout(15, unit = TimeUnit.MINUTES)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
annotation class FifteenMinutesTimeout

/**
 * JUnit 5 annotation to denote slow that, which may take up to 30 minutes.
 */
@Timeout(30, unit = TimeUnit.MINUTES)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
annotation class ThirtyMinutesTimeout
