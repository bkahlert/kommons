package koodies.test.junit

import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit.MINUTES

/**
 * JUnit 5 annotation to denote slow that, which may take up to 15 minutes.
 */
@Timeout(15, unit = MINUTES)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
annotation class FifteenMinutesTimeout
