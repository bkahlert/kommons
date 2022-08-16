package com.bkahlert.logging.autoconfigure.logback

import com.bkahlert.logging.logback.LogbackConfiguration
import net.logstash.logback.argument.StructuredArguments.e
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.Arrays

/**
 * Properties that can be set to manipulate the default log settings.
 *
 *
 * The configuration encompasses the different appenders whose default
 * configuration can be overwritten.
 */
@ConfigurationProperties(prefix = LogbackProperties.PREFIX)
public data class LogbackProperties(
    public val appenders: AppenderProperties = AppenderProperties(),
) : InitializingBean {
    private val logger = LoggerFactory.getLogger(javaClass)

    public override fun afterPropertiesSet() {
        logger.info("Logback successfully configured: {}", e(LogbackConfiguration.details))
    }

    public data class AppenderProperties(
        /**
         * Appender that logs to the console
         */
        val console: String = "preset",

        /**
         * Appender that logs to a file.
         * Further configuration can be made using `logging` properties like changing the log file name with `logging.file.name=my.log`.
         */
        val file: String = "preset",
    )

    public companion object {
        public const val PREFIX: String = "kkb.logging"
        public const val CONSOLE: String = "kkb.logging.appenders.console"
        public const val FILE: String = "kkb.logging.appenders.file"
        public val propertyNames: List<String>
            get() = Arrays.asList(CONSOLE, FILE)
    }
}
