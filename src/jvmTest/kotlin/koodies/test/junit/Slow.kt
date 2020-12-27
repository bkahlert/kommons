package koodies.test.junit

import koodies.test.junit.Slow.Companion.NAME
import org.junit.jupiter.api.Tag

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
