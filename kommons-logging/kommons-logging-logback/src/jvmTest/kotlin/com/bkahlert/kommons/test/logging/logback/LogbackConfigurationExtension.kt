package com.bkahlert.kommons.test.logging.logback

import ch.qos.logback.core.util.OptionHelper.getSystemProperty
import com.bkahlert.kommons.logging.logback.Appender
import com.bkahlert.kommons.logging.logback.AppenderPreset
import com.bkahlert.kommons.logging.logback.Logback
import com.bkahlert.kommons.logging.logback.Logback.loadConfiguration
import com.bkahlert.kommons.quoted
import com.bkahlert.kommons.test.logging.logback.AppenderTestPreset.None
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
import java.lang.Boolean.parseBoolean
import java.lang.reflect.Parameter
import java.nio.file.Path
import java.nio.file.Paths
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
        Paths.get(checkNotNull(Logback.activeLogFileName) { "Logback log file could not be located." })

    override fun beforeEach(context: ExtensionContext) {
        refresh(context)
    }

    override fun afterEach(context: ExtensionContext?) {
        Logback.reset()
    }

    companion object {

        /** Refreshes the [Logback] configuration to reflect the annotated [LogbackConfiguration]. */
        fun refresh(extensionContext: ExtensionContext) {
            Logback.reset()
            val logbackConfiguration: LogbackConfiguration? =
                AnnotationSupport.findAnnotation(extensionContext.element, LogbackConfiguration::class.java).orElse(null)
            val properties = buildList {
                logbackConfiguration?.console?.value?.also { add(Appender.Console.systemPropertyName to it) }
                logbackConfiguration?.file?.value?.also { add(Appender.File.systemPropertyName to it) }
                if (extensionContext.containsValidLogFileParameter()) {
                    add(LoggingSystemProperties.LOG_FILE to createTempFile("kommons-test-", ".log").pathString)
                }
            }
            loadConfiguration(renderXml(properties, debug = logbackConfiguration?.debug ?: parseBoolean(getSystemProperty("logback.debug"))))
        }

        private fun renderXml(properties: List<Pair<String, String?>>, debug: Boolean): String = buildString {
            appendLine("<configuration debug=\"$debug\">")
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
    val console: AppenderTestPreset = None,
    val file: AppenderTestPreset = None,
    val debug: Boolean = false,
)

/**
 * Test variant of [AppenderPreset] that can leave a property unchanged using [None].
 */
enum class AppenderTestPreset(
    val preset: AppenderPreset?,
) {
    /** Sets the preset setting for an appender to [AppenderPreset.Default]. */
    Default(AppenderPreset.Default),

    /** Sets the preset setting for an appender to [AppenderPreset.Spring]. */
    Spring(AppenderPreset.Spring),

    /** Sets the preset setting for an appender to [AppenderPreset.Minimal]. */
    Minimal(AppenderPreset.Minimal),

    /** Sets the preset setting for an appender to [AppenderPreset.Json]. */
    Json(AppenderPreset.Json),

    /** Sets the preset setting for an appender to [AppenderPreset.Off]. */
    Off(AppenderPreset.Off),

    /** Doesn't set the preset setting for an appender. */
    None(null),
    ;

    val value: String? get() = preset?.value
}

@Retention(AnnotationRetention.RUNTIME)
@Target(VALUE_PARAMETER)
@ExtendWith(LogbackConfigurationExtension::class)
@MustBeDocumented
annotation class LogFile
