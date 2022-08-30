package com.bkahlert.kommons.test.logging.logback

import com.bkahlert.kommons.logging.LoggingPreset
import com.bkahlert.kommons.logging.LoggingSystemProperties.CONSOLE_LOG_PRESET
import com.bkahlert.kommons.logging.LoggingSystemProperties.FILE_LOG_PRESET
import com.bkahlert.kommons.logging.logback.Logback
import com.bkahlert.kommons.logging.logback.Logback.loadConfiguration
import com.bkahlert.kommons.quoted
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.Resources
import org.junit.platform.commons.support.AnnotationSupport
import org.springframework.boot.logging.LoggingSystemProperties
import java.lang.reflect.Parameter
import java.nio.file.Path
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER
import kotlin.io.path.createTempFile
import kotlin.io.path.pathString

/**
 * A JUnit extension to configure Logback for the scope of a test container or test.
 * @see LogbackConfiguration
 */
class LogbackConfigurationExtension : ParameterResolver, BeforeEachCallback, AfterEachCallback {

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
        parameterContext.parameter.isValidLogFileParameter()

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any =
        checkNotNull(Logback.activeLogFile) { "Logback log file could not be located." }

    override fun beforeEach(context: ExtensionContext) {
        refresh(context)
    }

    override fun afterEach(context: ExtensionContext?) {
        Logback.reset()
    }

    companion object {

        /** Refreshes the [Logback] configuration to reflect the annotated [LogbackConfiguration]. */
        fun refresh(extensionContext: ExtensionContext) {
            val logbackConfiguration: LogbackConfiguration? =
                AnnotationSupport.findAnnotation(extensionContext.element, LogbackConfiguration::class.java).orElse(null)
            val properties: List<Pair<String, String>> = buildList {
                logbackConfiguration?.console?.also { add(CONSOLE_LOG_PRESET to it.value) }
                logbackConfiguration?.file?.also { add(FILE_LOG_PRESET to it.value) }
                if (extensionContext.containsValidLogFileParameter()) {
                    add(LoggingSystemProperties.LOG_FILE to createTempFile("kommons-test-", ".log").pathString)
                }
            }
            loadConfiguration(renderXml(properties, debug = logbackConfiguration?.debug ?: System.getProperty("logback.debug").toBoolean()))
        }

        private fun renderXml(properties: List<Pair<String, String?>>, debug: Boolean): String = buildString {
            appendLine("<configuration debug=${debug.quoted} watch=\"false\">")
            properties.forEach { (key, value) ->
                appendLine("    <property scope=\"context\" name=${key.quoted} value=${value.quoted}/>")
            }
            appendLine("    <include resource=\"com/bkahlert/kommons/logging/logback/base.xml\"/>")
            appendLine("</configuration>")
        }

        private fun ExtensionContext.containsValidLogFileParameter(): Boolean =
            testMethod.map { method -> method.parameters.any { it.isValidLogFileParameter() } }.orElse(false)

        private fun Parameter.isValidLogFileParameter(): Boolean =
            this.type == Path::class.java && this.isAnnotationPresent(LogFile::class.java)
    }
}

/**
 * Sets a [Logback] configuration for the scope
 * of the annotated test class or method.
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES, mode = READ_WRITE)
@Retention(AnnotationRetention.RUNTIME)
@Target(FUNCTION)
@ExtendWith(LogbackConfigurationExtension::class)
@MustBeDocumented
annotation class LogbackConfiguration(
    val console: LoggingPreset = LoggingPreset.SPRING,
    val file: LoggingPreset = LoggingPreset.OFF,
    val debug: Boolean = false,
)

/**
 * Annotation that can be used to retrieve the active log file
 * by annotating a test parameter of type [Path].
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(VALUE_PARAMETER)
@ExtendWith(LogbackConfigurationExtension::class)
@MustBeDocumented
annotation class LogFile
