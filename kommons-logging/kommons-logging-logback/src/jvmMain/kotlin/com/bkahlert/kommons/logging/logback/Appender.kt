package com.bkahlert.kommons.logging.logback

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.util.ContextInitializer
import ch.qos.logback.classic.util.StatusViaSLF4JLoggerFactory
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.OutputStreamAppender
import ch.qos.logback.core.joran.spi.JoranException
import ch.qos.logback.core.rolling.RollingFileAppender
import com.bkahlert.kommons.logging.LoggingSystemProperties
import org.slf4j.LoggerFactory
import java.text.MessageFormat
import java.util.Objects
import java.util.function.Consumer

/**
 * Appenders that can be configured via [LoggingSystemProperties].
 */
public enum class Appender(
    /** The name of the system property that contains the configuration preset for this appender. */
    public val systemPropertyName: String,
) {
    Console(LoggingSystemProperties.CONSOLE_LOG_PRESET),
    File(LoggingSystemProperties.FILE_LOG_PRESET),
    ;

    /**
     * The [Encoder] used to serialize the log messages.
     *
     * **Important:** This method is only public to allow for test writers to easily change the logger for a selection of tests.<br></br>
     * The better solution would be so have a `common-logging-test` lib that provides the `LogbackConfigurationExtension`.
     *
     *
     * If you just want to change the configuration, you can either configure your `application.yml`, set system variables or
     * set up a `logback.xml` yourself. Consult `README.md` for more details.
     *
     * @param value the encoder to use
     */
    public var preset: AppenderPreset
        get() {
            val value: String = Logback.context.getProperty(name)
            log("Read property {0}: {1}", name, value)
            return AppenderPreset.ofOrDefault(value)
        }
        internal set(value) {
            Objects.requireNonNull(value)
            log("Setting property {0} to {1}", name, value)
            reload(Consumer<LoggerContext> { loggerContext: LoggerContext -> loggerContext.putProperty(name, value.name.lowercase()) })
        }

    override fun toString(): String {
        return MessageFormat.format("<property name=\"{0}\" value=\"{1}\"/>", name, preset)
    }

    public companion object {

        /**
         * Returns the root logger, which passes its configuration to all child
         * loggers if not overwritten.
         *
         * @return root logger
         */
        public val logbackRootLogger: Logger
            get() = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger

        /**
         * Returns the location of the currently used log file.
         *
         * @return log file log operations using [.FILE_LOG_PRESET] are written to
         */
        public val activeLogFileName: String?
            get() = fileAppender?.let {
                it.flush()
                (it as? RollingFileAppender)?.rollingPolicy?.activeFileName
            } ?: Logback.context.getProperty(org.springframework.boot.logging.LoggingSystemProperties.LOG_FILE)

        /**
         * Returns the actual [ch.qos.logback.core.FileAppender] the embodies [.FILE_LOG_PRESET].
         *
         * @return the actual [ch.qos.logback.core.FileAppender].
         */
        private val fileAppender: FileAppender<*>?
            get() = logbackRootLogger.getAppender("FILE") as? RollingFileAppender

        /**
         * Flushes eventually buffered content (as it is the case with [System.out] to avoid loss of data in case of reconfiguration.
         *
         * @param this@flush the appender to flush
         * @param <T>      type of the appender
         *
         * @return the flushed appender
        </T> */
        private fun OutputStreamAppender<*>.flush() {
            if (!isImmediateFlush) {
                isImmediateFlush = true
                isImmediateFlush = false
            }
        }

        /**
         * Reloads the configuration with the opportunity to apply changes
         * to the [LoggerContext].
         *
         * @param consumer will be called right before the passed logger context gets reloaded
         */
        private fun reload(consumer: Consumer<LoggerContext>) {
            val loggerContext: LoggerContext = Logback.context
            val backup: Map<String, String?> = loggerContext.getCopyOfPropertyMap()
            loggerContext.reset()
            val contextInitializer = ContextInitializer(loggerContext)
            backup.forEach(loggerContext::putProperty)
            consumer.accept(loggerContext)
            try {
                contextInitializer.autoConfig()
            } catch (e: JoranException) {
                throw LogbackConfigurationReloadException(e)
            }
        }

        /**
         * Logs using Logback's status logger. This logger doesn't not "pollute" the actual can
         * and can only be seen in debug mode.
         *
         * @param format [MessageFormat] template, that uses indices in contrast to SLF4J (`{0}...{n} instead of {}`)
         * @param args   the variables to fill the placeholders with
         */
        public fun log(format: String?, vararg args: Any?) {
            StatusViaSLF4JLoggerFactory.addInfo(MessageFormat.format(format, *args), Appender::class.java)
        }
    }
}
