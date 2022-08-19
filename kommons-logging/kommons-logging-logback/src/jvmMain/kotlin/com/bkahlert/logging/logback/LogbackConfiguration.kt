package com.bkahlert.logging.logback

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.util.ContextInitializer
import ch.qos.logback.classic.util.StatusViaSLF4JLoggerFactory
import ch.qos.logback.core.OutputStreamAppender
import ch.qos.logback.core.joran.spi.JoranException
import ch.qos.logback.core.rolling.RollingFileAppender
import org.slf4j.LoggerFactory
import java.text.MessageFormat
import java.util.Objects
import java.util.function.Consumer
import java.util.stream.Stream

/**
 * Port of call for the managed Logback configuration.
 *
 *
 * Provides access to the properties themselves as well as to life-cycle
 * functionality - namely reloading.
 *
 * @author Björn Kahlert
 */
public enum class LogbackConfiguration {
    CONSOLE_APPENDER, FILE_APPENDER;

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
    public var encoder: Encoder
        get() {
            val value: String = loggerContext.getProperty(name)
            log("Read property {0}: {1}", name, value)
            return Encoder.of(value)
        }
        internal set(value) {
            Objects.requireNonNull(value)
            log("Setting property {0} to {1}", name, value)
            reload(Consumer<LoggerContext> { loggerContext: LoggerContext -> loggerContext.putProperty(name, value.name.lowercase()) })
        }

    override fun toString(): String {
        return MessageFormat.format("<property name=\"{0}\" value=\"{1}\"/>", name, encoder)
    }

    public enum class Encoder {
        /**
         * Classical KKB console log format
         */
        classic,

        /**
         * New KKB console log encoder. This new format provides a more Spring-like look,
         * is easier to parse and easier to read due to the use of brackets that group information more semantically.
         */
        plain,
        spring,

        /**
         * Minimal version of the new KKB console log encoder which only logs minutes to milliseconds,
         * the log level, logger and message.
         */
        minimal,

        /**
         * JSON format that is particularly suited to store machine-readable structured information
         */
        json,

        /**
         * No encoder at all is used which deactivates the corresponding appender.
         */
        none,

        /**
         * Console appender defaults to plain
         * File appender defaults to none
         */
        preset;

        public companion object {
            public fun of(lowerCaseName: String?): Encoder {
                if (lowerCaseName == null) {
                    log("Encoder looked up with null: defaulting to 'preset'.")
                    return preset
                }
                val encoder = Stream.of(*values()).filter { enc: Encoder -> enc.name.equals(lowerCaseName, ignoreCase = true) }
                    .findAny()
                if (!encoder.isPresent) {
                    log("Encoder looked up with invalid {0}: defaulting to 'preset'.", lowerCaseName)
                }
                return encoder.orElse(preset)
            }
        }
    }

    public companion object {
        public const val LOG_FILE_PROPERTY_NAME: String = "LOG_FILE"

        /**
         * Returns a detached copy of Logback's configuration properties.
         *
         * @return Logback's configuration properties
         */
        public val details: Map<String, String?>
            get() {
                val details: MutableMap<String, String?> = loggerContext.copyOfPropertyMap
                details[LOG_FILE_PROPERTY_NAME] = activeLogFileName
                return details
            }

        /**
         * Returns the currently active Logger context.
         *
         *
         * **Use with caution!**
         * Incorrectly configuring it, can trigger the creation of a new instance.
         * Consequently loggers already created stay bound to the old context and can
         * no longer be configured consistently.
         *
         * @return active logger context
         */
        public val loggerContext: LoggerContext
            get() = LoggerFactory.getILoggerFactory() as LoggerContext

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
         * @return log file log operations using [.FILE_APPENDER] are written to
         */
        public val activeLogFileName: String?
            get() = fileAppender?.let {
                it.flush()
                it.rollingPolicy?.activeFileName
            } ?: loggerContext.getProperty(LOG_FILE_PROPERTY_NAME)

        /**
         * Returns the actual [ch.qos.logback.core.FileAppender] the embodies [.FILE_APPENDER].
         *
         * @return the actual [ch.qos.logback.core.FileAppender].
         */
        private val fileAppender: RollingFileAppender<*>?
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
            val loggerContext: LoggerContext = loggerContext
            val backup: Map<String, String> = loggerContext.getCopyOfPropertyMap()
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
            StatusViaSLF4JLoggerFactory.addInfo(MessageFormat.format(format, *args), LogbackConfiguration::class.java)
        }
    }
}
