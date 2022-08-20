package com.bkahlert.kommons.logging.logback

import com.bkahlert.kommons.logging.LogPresets
import java.util.stream.Stream

/** Configuration presets for [Appender] instances. */
public enum class AppenderPreset(
    /** The value used to set an appender to this preset. */
    public val value: String,
) {

    /** Explicitly configures an [Appender] to use default logging settings. */
    Default(LogPresets.DEFAULT_PRESET),

    /** Configures an [Appender] to use the default Spring Boot logging settings. */
    Spring(LogPresets.SPRING_PRESET),

    /**
     * Configures an [Appender] to use only log:
     * - minutes to milliseconds,
     * - the log level
     * - the logger, and
     * - the log message
     */
    Minimal(LogPresets.MINIMAL_PRESET),

    /** Configures an [Appender] to log using the JSON format. */
    Json(LogPresets.JSON_PRESET),

    /** Configures an [Appender] to not log at all. */
    Off(LogPresets.OFF_PRESET),
    ;

    public companion object {
        @Deprecated("remove")
        public fun ofOrDefault(presetName: String?): AppenderPreset {
            if (presetName == null) {
                Appender.log("Encoder looked up with null: defaulting to 'preset'.")
                return Default
            }
            val encoder = Stream.of(*values()).filter { enc: AppenderPreset -> enc.value.equals(presetName, ignoreCase = true) }
                .findAny()
            if (!encoder.isPresent) {
                Appender.log("Encoder looked up with invalid {0}: defaulting to 'preset'.", presetName)
            }
            return encoder.orElse(Default)
        }
    }
}
