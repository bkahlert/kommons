package koodies.test

import koodies.test.output.OutputCaptureExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Isolated
import org.junit.jupiter.api.parallel.ResourceAccessMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.ResourceLocks
import org.junit.jupiter.api.parallel.Resources

/**
 * Declares a requirement on [System.out] and [System.err].
 * Using the annotations provides exclusive access to the named
 * resources but in contrast to [Isolated] system IO independent
 * tests can run in parallel.
 *
 * Adds [OutputCaptureExtension] automatically.
 */
@ResourceLocks(
    ResourceLock(Resources.SYSTEM_OUT),
    ResourceLock(Resources.SYSTEM_ERR),
)
@ExtendWith(OutputCaptureExtension::class)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class SystemIoExclusive

/**
 * Declares a requirement on [System.out] and [System.err].
 * Using the annotations provides exclusive access to the named
 * resources but in contrast to [Isolated] system IO independent
 * tests can run in parallel.
 *
 * Adds [OutputCaptureExtension] automatically.
 */
@ResourceLocks(
    ResourceLock(Resources.SYSTEM_OUT, mode = ResourceAccessMode.READ),
    ResourceLock(Resources.SYSTEM_ERR, mode = ResourceAccessMode.READ),
)
@ExtendWith(OutputCaptureExtension::class)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class SystemIoRead
