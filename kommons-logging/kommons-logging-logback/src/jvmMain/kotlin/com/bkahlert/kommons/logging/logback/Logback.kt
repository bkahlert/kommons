package com.bkahlert.kommons.logging.logback

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.classic.util.ContextInitializer
import ch.qos.logback.core.Context
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.OutputStreamAppender
import ch.qos.logback.core.joran.spi.JoranException
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.status.ErrorStatus
import ch.qos.logback.core.status.StatusManager
import ch.qos.logback.core.status.StatusUtil
import ch.qos.logback.core.util.StatusPrinter
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory
import org.slf4j.impl.StaticLoggerBinder
import org.springframework.boot.logging.LoggingSystemProperties
import org.springframework.util.ReflectionUtils
import java.io.InputStream
import java.lang.reflect.Method
import kotlin.reflect.KClass
import ch.qos.logback.classic.Logger as LogbackLogger
import org.slf4j.Logger as Slf4jLogger

/**
 * [Logback](https://logback.qos.ch/) convenience features.
 */
public object Logback {

    /** The [LoggerContext]. */
    public val context: LoggerContext
        get() = LoggerFactory.getILoggerFactory() as LoggerContext

    /** The properties of the [context]. */
    public val properties: Map<String, String?>
        get() = buildMap {
            putAll(context.copyOfPropertyMap)
            put(LoggingSystemProperties.LOG_FILE, Appender.activeLogFileName)
        }

    /** The root logger. */
    public val rootLogger: Logger
        get() = LoggerFactory.getLogger(ROOT_LOGGER_NAME) as Logger

    /** The filename of the active log file used by the [FileAppender]. */
    public val activeLogFileName: String?
        get() = fileAppender
            ?.let { (it as? RollingFileAppender) }
            ?.also { it.flush() }
            ?.rollingPolicy?.activeFileName
            ?: context.getProperty(LoggingSystemProperties.LOG_FILE)

    /** Returns the actual `FILE` [FileAppender]. */
    private val fileAppender: FileAppender<*>?
        get() = rootLogger.getAppender("FILE") as? RollingFileAppender

    private fun OutputStreamAppender<*>.flush() {
        if (!isImmediateFlush) {
            isImmediateFlush = true
            isImmediateFlush = false
        }
    }

    // TODO make extension functions

    public fun getLogger(name: String): LogbackLogger =
        context.getLogger(name)

    public fun getLogger(clazz: Class<*>): LogbackLogger =
        context.getLogger(clazz)

    public fun getLogger(kClass: KClass<*>): LogbackLogger =
        getLogger(kClass.java)

    public fun getLogger(logger: Slf4jLogger): LogbackLogger =
        logger as LogbackLogger

    public fun changeLogLevel(kClass: KClass<*>, level: Level) {
        changeLogLevel(kClass.java, level)
    }

    public fun changeLogLevel(clazz: Class<*>, level: Level) {
        getLogger(clazz).level = level
    }

    public fun changeLogLevel(logger: Slf4jLogger, level: Level) {
        getLogger(logger).level = level
    }

    public fun addLoggingEvent(clazz: Class<*>, level: Level, message: String?, vararg params: Any?) {
        val logger: LogbackLogger = getLogger(clazz)
        logger.callAppenders(LoggingEvent(clazz.name, logger, level, message, null, params))
    }

    public fun addLoggingEvent(clazz: Class<*>, level: Level, message: String?, throwable: Throwable?, vararg params: Any?) {
        val logger: LogbackLogger = getLogger(clazz)
        logger.callAppenders(LoggingEvent(clazz.name, logger, level, message, throwable, params))
    }

    /**
     * Resets Logback and auto-configures is as if it was the first start.
     */
    public fun reset() {
        val loggerContext: LoggerContext = context
        loggerContext.reset()
        val contextInitializer = ContextInitializer(loggerContext)
        contextInitializer.autoConfig()
    }

    /**
     * Resets Logback and auto-configures is as if it was the first start.
     *
     *
     * <**Warning: This operation creates a new logging context!**<br></br>
     * Already created loggers will be detached and use the old configuration.
     *
     *
     * If that's not the intention, use [.reset] instead.
     */
    public fun fullyReset() {
        val resetLogbackMethod: Method = StaticLoggerBinder::class.java.getDeclaredMethod("reset")
        ReflectionUtils.invokeMethod(resetLogbackMethod, StaticLoggerBinder::class.java)
    }

    public fun loadConfiguration(configAsStream: InputStream) {
        val loggerContext: LoggerContext = context
        try {
            val configurator = JoranConfigurator()
            configurator.setContext(loggerContext)
            loggerContext.reset()
            configurator.doConfigure(configAsStream)
        } catch (je: JoranException) {
            // StatusPrinter will handle this
        }
        printInCaseOfErrors(loggerContext, 0)
    }

    public fun loadConfiguration(configAsString: String): Unit =
        loadConfiguration(configAsString.byteInputStream())

    /**
     * Print the contents of the context status, but only if they contain
     * warnings or errors occurring later then the threshold.
     *
     *
     * Copied from [StatusPrinter.printInCaseOfErrorsOrWarnings] to allow for Error-only status logging
     * which allows for very strict tests.
     *
     * @param context
     */
    public fun printInCaseOfErrors(context: Context, threshold: Long) {
        requireNotNull(context) { "Context argument cannot be null" }
        val sm: StatusManager = context.statusManager
        val statusUtil = StatusUtil(context)
        if (statusUtil.getHighestLevel(threshold) >= ErrorStatus.ERROR) {
            StatusPrinter.print(sm, threshold)
        }
    }
}
