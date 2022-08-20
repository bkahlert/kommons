package com.bkahlert.kommons.logging.spring

import com.bkahlert.kommons.logging.LoggingPreset
import com.bkahlert.kommons.logging.logback.Logback
import com.bkahlert.kommons.logging.logback.StructuredArguments.e
import com.bkahlert.kommons.logging.spring.LoggingProperties.Companion
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * Logging properties.
 */
@Suppress("RedundantCompanionReference")
@ConstructorBinding
@ConfigurationProperties(prefix = Companion.PREFIX)
public data class LoggingProperties(
    public val preset: PresetProperties = PresetProperties(),
) : InitializingBean {

    private val logger = LoggerFactory.getLogger(LoggingProperties::class.java)

    public override fun afterPropertiesSet() {
        logger.info("Configured Logback: {}", e(Logback.properties))
    }

    /**
     * Preset properties.
     */
    public data class PresetProperties(
        /**
         * Preset to use for the CONSOLE log.
         */
        public val console: LoggingPreset = LoggingPreset.DEFAULT,

        /**
         * Preset to use for the FILE log.
         */
        public val file: LoggingPreset = LoggingPreset.DEFAULT,
    )

    public companion object {

        /**
         * Prefix for logging Spring properties.
         */
        public const val PREFIX: String = "logging"

        /**
         * Name of the Spring property that contains the preset of the CONSOLE log.
         */
        public const val CONSOLE_LOG_PRESET_PROPERTY: String = "$PREFIX.preset.console"

        /**
         * Name of the Spring property that contains the preset of the FILE log.
         */
        public const val FILE_LOG_PRESET_PROPERTY: String = "$PREFIX.preset.file"
    }
}
