package koodies.test.debug

import koodies.test.Debug
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import java.lang.annotation.Inherited
import java.util.concurrent.TimeUnit.MINUTES
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER

/**
 * Annotated [Test] methods are run [Isolated] and sibling tests and their descendants
 * are ignored.
 */
@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, ANNOTATION_CLASS, CLASS)
@Retention(RUNTIME)
@MustBeDocumented
@Inherited
@Timeout(10, unit = MINUTES)
@Isolated
@Execution(ExecutionMode.SAME_THREAD)
@Test
@Debug
annotation class InternalDebug(val includeInReport: Boolean = true)
