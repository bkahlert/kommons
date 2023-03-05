package com.bkahlert.kommons.logging.logback

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.util.ContextInitializer
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.OutputStreamAppender
import ch.qos.logback.core.joran.spi.JoranException
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.RollingPolicy
import ch.qos.logback.core.rolling.RollingPolicyBase
import ch.qos.logback.core.rolling.TriggeringPolicy
import ch.qos.logback.core.util.StatusPrinter
import com.bkahlert.kommons.io.toPath
import com.bkahlert.kommons.logging.LoggingSystemProperties
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.lang.reflect.Modifier
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.TimeoutException
import kotlin.io.path.pathString
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.springframework.boot.logging.LoggingSystemProperties as SpringLoggingSystemProperties

/** [Logback](https://logback.qos.ch/) utility functions. */
public object Logback {

    private val MaxInitializationWaitDuration = 2.seconds

    /** The [LoggerContext]. */
    public val context: LoggerContext
        get() {
            val start = Instant.now()
            while ((System.currentTimeMillis() - start.toEpochMilli()).milliseconds < MaxInitializationWaitDuration) {
                val factory = LoggerFactory.getILoggerFactory()
                if (factory is LoggerContext) return factory
                else Thread.sleep(100)
            }
            throw TimeoutException("${LoggerContext::class.qualifiedName} did not initialize within $MaxInitializationWaitDuration")
        }

    /** The properties of the [context]. */
    public val properties: Map<String, String?>
        get() = context.copyOfPropertyMap

    /** The root logger. */
    public val rootLogger: Logger
        get() = LoggerFactory.getLogger(ROOT_LOGGER_NAME) as Logger

    /** The active log filename used by the [FileAppender]. */
    public var activeLogFileName: String?
        get() = fileAppender
            ?.let { (it as? RollingFileAppender) }
            ?.apply { flush() }
            ?.rollingPolicy
            ?.activeFileName
        set(value) {
            if (activeLogFileName == value) return
            fileAppender
                ?.let { (it as? RollingFileAppender) }
                ?.apply {
                    stop()
                    val triggeringPolicyBackup: TriggeringPolicy<*>? = triggeringPolicy.also { it.stop() }
                    val rollingPolicyBackup: RollingPolicy? = rollingPolicy.also { it.stop() }
                    if (rollingPolicyBackup is RollingPolicyBase) {
                        val activeFileNameBackup: String? = rollingPolicyBackup.activeFileName
                        val fileNamePatternBackup: String? = rollingPolicyBackup.fileNamePattern
                        if (activeFileNameBackup != null && fileNamePatternBackup != null && value != null) {
                            rollingPolicyBackup.fileNamePattern = fileNamePatternBackup.replace(activeFileNameBackup, value)
                        }
                    }
                    triggeringPolicy = null
                    rollingPolicy = null
                    file = value
                    triggeringPolicy = triggeringPolicyBackup.also { it?.start() }
                    rollingPolicy = rollingPolicyBackup.also { it?.start() }
                    start()
                }
        }

    /** The active log file used by the [FileAppender]. */
    public var activeLogFile: Path?
        get() = activeLogFileName?.toPath()
        set(value) {
            activeLogFileName = value?.pathString
        }

    /** Returns the actual `FILE` [FileAppender]. */
    public val fileAppender: FileAppender<*>?
        get() = rootLogger.getAppender("FILE") as? RollingFileAppender

    private fun OutputStreamAppender<*>.flush() {
        if (!isImmediateFlush) {
            isImmediateFlush = true
            isImmediateFlush = false
        }
    }

    /** Clears all system properties that can be used to configure logging. */
    public fun clearSystemProperties() {
        arrayOf(
            SpringLoggingSystemProperties::class,
            LoggingSystemProperties::class
        ).flatMap {
            it.java.declaredFields.filter { field ->
                Modifier.isPublic(field.modifiers) && Modifier.isStatic(field.modifiers) && field.type == String::class.java
            }
        }.forEach {
            System.clearProperty(it.get(null) as String)
        }
    }

    /** Resets Logback's [LoggerContext] and autoconfigures it. */
    public fun reset() {
        val loggerContext: LoggerContext = context
        loggerContext.reset()
        val contextInitializer = ContextInitializer(loggerContext)
        contextInitializer.autoConfig()
    }

    /** Loads the specified input stream-based [configuration] using [JoranConfigurator]. */
    public fun loadConfiguration(configuration: InputStream) {
        val loggerContext: LoggerContext = context
        try {
            val configurator = JoranConfigurator()
            configurator.context = loggerContext
            loggerContext.reset()
            configurator.doConfigure(configuration)
        } catch (_: JoranException) {
            // StatusPrinter handles this
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext, 0)
    }

    /** Loads the specified string-based [configuration] using [JoranConfigurator]. */
    public fun loadConfiguration(configuration: String): Unit =
        loadConfiguration(configuration.byteInputStream())
}
