package com.bkahlert.logging.support

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.classic.util.ContextInitializer
import ch.qos.logback.core.Context
import ch.qos.logback.core.joran.spi.JoranException
import ch.qos.logback.core.status.ErrorStatus
import ch.qos.logback.core.status.StatusManager
import ch.qos.logback.core.status.StatusUtil
import ch.qos.logback.core.util.OptionHelper.getSystemProperty
import ch.qos.logback.core.util.StatusPrinter
import org.slf4j.LoggerFactory
import org.slf4j.impl.StaticLoggerBinder
import org.springframework.util.ReflectionUtils
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.reflect.Method
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

/**
 * Bunch of useful utility methods to easily interact with Logback
 *
 * @author Björn Kahlert
 */
object LogbackUtil {
    val loggerContext: LoggerContext
        get() {

            val iLoggerFactory = LoggerFactory.getILoggerFactory()
            return iLoggerFactory as LoggerContext
        }

    fun getLogbackLogger(kClass: KClass<*>): Logger =
        getLogbackLogger(kClass.java)

    fun getLogbackLogger(clazz: Class<*>): Logger =
        LoggerFactory.getLogger(clazz) as Logger

    fun getLogbackLogger(logger: org.slf4j.Logger): Logger =
        logger as Logger

    fun changeLogLevel(kClass: KClass<*>, level: Level) {
        changeLogLevel(kClass.java, level)
    }

    fun changeLogLevel(clazz: Class<*>, level: Level) {
        getLogbackLogger(clazz).level = level
    }

    fun changeLogLevel(logger: org.slf4j.Logger, level: Level) {
        getLogbackLogger(logger).level = level
    }

    fun addLoggingEvent(clazz: Class<*>, level: Level, message: String?, vararg params: Any?) {
        val logger: Logger = getLogbackLogger(clazz)
        logger.callAppenders(LoggingEvent(clazz.name, logger, level, message, null, params))
    }

    fun addLoggingEvent(clazz: Class<*>, level: Level, message: String?, throwable: Throwable?, vararg params: Any?) {
        val logger: Logger = getLogbackLogger(clazz)
        logger.callAppenders(LoggingEvent(clazz.name, logger, level, message, throwable, params))
    }

    /**
     * Resets Logback and auto-configures is as if it was the first start.
     */
    fun reset() {
        val loggerContext: LoggerContext = loggerContext
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
    fun fullyReset() {
        val resetLogbackMethod: Method = StaticLoggerBinder::class.java.getDeclaredMethod("reset")
        ReflectionUtils.invokeMethod(resetLogbackMethod, StaticLoggerBinder::class.java)
    }

    val isLogbackDebugMode: Boolean
        get() = java.lang.Boolean.parseBoolean(getSystemProperty("logback.debug"))

    fun loadConfigurationStream(configAsStream: InputStream) {
        val loggerContext: LoggerContext = loggerContext
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

    fun loadConfiguration(configAsString: String) {
        loadConfigurationStream(ByteArrayInputStream(configAsString.toByteArray(StandardCharsets.UTF_8)))
    }

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
    fun printInCaseOfErrors(context: Context, threshold: Long) {
        requireNotNull(context) { "Context argument cannot be null" }
        val sm: StatusManager = context.statusManager
        val statusUtil = StatusUtil(context)
        if (statusUtil.getHighestLevel(threshold) >= ErrorStatus.ERROR) {
            StatusPrinter.print(sm, threshold)
        }
    }
}
