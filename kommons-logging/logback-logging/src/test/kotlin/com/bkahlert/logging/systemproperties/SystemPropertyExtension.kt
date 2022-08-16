package de.dkb.api.systemproperties

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Store
import org.junit.platform.commons.support.AnnotationSupport.findRepeatableAnnotations
// TODO migrate to kommons-test
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
        findRepeatableAnnotations(element, SystemProperty::class.java).forEach { property ->
            val oldValue = System.setProperty(property.name, property.value)
            getStore().put(property, oldValue)
        }
    }

    private fun ExtensionContext.restoreAllSystemProperties() {
        findRepeatableAnnotations(element, SystemProperty::class.java)
            .forEach { property ->
                val backupValue = getStore().get(property, String::class.java)
                if (backupValue != null) System.setProperty(property.name, backupValue)
                else System.clearProperty(property.name)
            }
    }

    private fun ExtensionContext.getStore(): Store = getStore(ExtensionContext.Namespace.create(element))
}
