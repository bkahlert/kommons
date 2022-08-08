package com.bkahlert.kommons.test.junit

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Store
import org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.Resources
import org.junit.platform.commons.support.AnnotationSupport
import java.lang.annotation.Repeatable

/**
 * This extension allows to set/override system properties for the scope of a single test.
 *
 * The system properties can be specified using the [SystemProperty] annotation — one per property.
 *
 * The previous state will be restored after the test finished.
 */
public class SystemPropertyExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    override fun beforeAll(context: ExtensionContext) {
        context.backupAndApplySystemProperties()
    }

    override fun afterAll(context: ExtensionContext) {
        context.restoreBackedUpSystemProperties()
    }

    override fun beforeEach(context: ExtensionContext) {
        context.backupAndApplySystemProperties()
    }

    override fun afterEach(context: ExtensionContext) {
        context.restoreBackedUpSystemProperties()
    }

    private fun ExtensionContext.backupAndApplySystemProperties() {
        annotatedSystemProperties.forEach { property ->
            val oldValue: String? = System.setProperty(property.name, property.value)
            store.put(property, oldValue)
        }
    }

    private fun ExtensionContext.restoreBackedUpSystemProperties() {
        annotatedSystemProperties.forEach { property ->
            val backupValue: String? = store.getTyped(property)
            if (backupValue != null) System.setProperty(property.name, backupValue)
            else System.clearProperty(property.name)
        }
    }

    private val ExtensionContext.annotatedSystemProperties: List<SystemProperty>
        get() = AnnotationSupport.findRepeatableAnnotations(element, SystemProperty::class.java)


    private val ExtensionContext.store: Store get() = getStore<SystemPropertyExtension>()
}


/**
 * Use this annotation to set a proper system property for the scope
 * of the annotated test class or method.
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES, mode = READ_WRITE)
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER
)
@Suppress("DEPRECATED_JAVA_ANNOTATION") // JUnit explicitly requires Java @Repeatable
@Repeatable(SystemProperties::class)
@ExtendWith(SystemPropertyExtension::class)
public annotation class SystemProperty(
    /** Name of the system property to set during test execution. */
    val name: String,
    /** Value of the system property to set during test execution. */
    val value: String,
)


/**
 * Allows to annotate [SystemProperty] multiple times.
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES, mode = READ_WRITE)
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
public annotation class SystemProperties(
    /** System properties to set during test execution. */
    vararg val value: SystemProperty,
)
