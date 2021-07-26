package koodies.test

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.platform.commons.support.AnnotationSupport
import java.lang.annotation.Repeatable

/**
 * Allows to annotate [SystemProperty] multiple times.
 */
@ResourceLock(SystemPropertyExtension.RESOURCE, mode = READ_WRITE)
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


/**
 * Use this annotation to set a proper system property for the scope
 * of the annotated test class or method.
 *
 * Its functionality is implemented by [SystemPropertyExtension] which is globally
 * registered using service locator `META-INF/services/org.junit.jupiter.api.extension.Extension`.
 *
 * *JUnit explicitly requires [Repeatable] (in contrast to [kotlin.annotation.Repeatable]).*
 */
@ResourceLock(SystemPropertyExtension.RESOURCE, mode = READ_WRITE)
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


/**
 * This extension allows to set/override system properties for the scope of a single test.
 *
 * The system properties can be specified using the [SystemProperty] annotation — one per property.
 *
 * The previous state will be restored after the test finished.
 */
class SystemPropertyExtension : BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    // sets system properties on class level
    override fun beforeAll(context: ExtensionContext) {
        context.backupAndApplyAllSystemProperties()
    }

    // restores system properties set on class level
    override fun afterAll(context: ExtensionContext) {
        context.restoreAllSystemProperties()
    }

    // restores system properties set on method level
    override fun beforeEach(context: ExtensionContext) {
        context.backupAndApplyAllSystemProperties()
    }

    // restores system properties set on method level
    override fun afterEach(context: ExtensionContext) {
        context.restoreAllSystemProperties()
    }

    private fun ExtensionContext.backupAndApplyAllSystemProperties() {
        AnnotationSupport.findRepeatableAnnotations(element, SystemProperty::class.java).forEach { property ->
            val oldValue = System.setProperty(property.name, property.value)
            getStore().put(property, oldValue)
        }
    }

    private fun ExtensionContext.restoreAllSystemProperties() {
        AnnotationSupport.findRepeatableAnnotations(element, SystemProperty::class.java)
            .forEach { property ->
                val backupValue = getStore().get(property, String::class.java)
                if (backupValue != null) System.setProperty(property.name, backupValue)
                else System.clearProperty(property.name)
            }
    }

    private fun ExtensionContext.getStore(): ExtensionContext.Store = getStore(ExtensionContext.Namespace.create(element))

    companion object {
        const val RESOURCE = "system-properties"
    }
}
