package com.bkahlert.kommons.logging.autoconfigure.logback

import com.bkahlert.kommons.logging.LogPresets
import com.bkahlert.kommons.logging.autoconfigure.logback.LoggingProperties.Companion
import com.bkahlert.kommons.logging.logback.Logback
import com.bkahlert.kommons.logging.logback.StructuredArguments.e
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.context.properties.ConfigurationProperties

@Suppress("RedundantCompanionReference")
@ConfigurationProperties(
    prefix = Companion.PREFIX,
)
public data class LoggingProperties(
    public val console: LogProperties = LogProperties(),
    public val file: LogProperties = LogProperties(),
) : InitializingBean {
    private val logger = LoggerFactory.getLogger(javaClass)

    public override fun afterPropertiesSet() {
        logger.info("Logback successfully configured: {}", e(Logback.properties))
    }

    public data class LogProperties(
        /**
         * The configuration preset for this log.
         */
        val preset: String = LogPresets.DEFAULT_PRESET,
    )

    public companion object {
        public const val PREFIX: String = "logging"
        public const val CONSOLE_LOG_PRESET: String = "logging.console.preset"
        public const val FILE_LOG_PRESET: String = "logging.file.preset"
        public val propertyNames: List<String>
            get() = listOf(CONSOLE_LOG_PRESET, FILE_LOG_PRESET)
    }
}
