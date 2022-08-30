package com.bkahlert.kommons.logging.logback

import ch.qos.logback.core.Context
import ch.qos.logback.core.PropertyDefinerBase
import ch.qos.logback.core.spi.ContextAware
import ch.qos.logback.core.spi.PropertyDefiner
import com.bkahlert.kommons.Program
import com.bkahlert.kommons.logging.LoggingPreset
import com.bkahlert.kommons.logging.LoggingSystemProperties

/** Provides the config path for the CONSOLE log. */
public class ConsoleAppenderConfigPropertyDefiner : ContextAware, PropertyDefiner by AppenderConfigPropertyDefiner(
    name = "console",
    fallbackPreset = LoggingPreset.SPRING,
    valueProvider = { it?.getProperty(LoggingSystemProperties.CONSOLE_LOG_PRESET) },
)

/** Provides the config path for the FILE log. */
public class FileAppenderConfigPropertyDefiner : ContextAware, PropertyDefiner by AppenderConfigPropertyDefiner(
    name = "file",
    fallbackPreset = LoggingPreset.OFF,
    valueProvider = { it?.getProperty(LoggingSystemProperties.FILE_LOG_PRESET) },
)

private class AppenderConfigPropertyDefiner(
    val name: String,
    val fallbackPreset: LoggingPreset,
    val valueProvider: (Context?) -> String?,
) : PropertyDefinerBase() {
    private val preset: LoggingPreset
        get() {
            val value = valueProvider(context)

            StatusLogger.info<AppenderConfigPropertyDefiner>("Resolving preset for value: $value")
            val preset = LoggingPreset.valueOfOrNull(value) ?: fallbackPreset
            StatusLogger.info<AppenderConfigPropertyDefiner>("Using preset $preset for $name log.")

            return preset
        }

    private val config: String
        get() {
            val baseDirectory = javaClass.`package`.name.replace('.', '/')
            return "$baseDirectory/appenders/$name-${preset.value}-appender.xml"
        }

    override fun getPropertyValue(): String = config.also {
        StatusLogger.info<AppenderConfigPropertyDefiner>("Using config $it for $name log.")
        checkNotNull(Program.contextClassLoader.getResource(it)) { "Failed to get resource $it" }
    }
}
