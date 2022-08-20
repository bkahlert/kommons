package com.bkahlert.kommons.logging

import com.bkahlert.kommons.firstEnumValueOfOrNull

/** A logging preset that specifies base settings for a log. */
public enum class LoggingPreset(
    /** The value that represents this preset. */
    public val value: String
) {

    /** Explicitly configures the log to use default logging settings. */
    DEFAULT(DEFAULT_PRESET_VALUE),

    /** Configures the log to use the default Spring Boot logging settings. */
    SPRING(SPRING_PRESET_VALUE),

    /**
     * Configures the log to only include:
     * - minutes to milliseconds,
     * - the log level
     * - the logger, and
     * - the log message
     */
    MINIMAL(MINIMAL_PRESET_VALUE),

    /** Configures the log to log using the JSON format. */
    JSON(JSON_PRESET_VALUE),

    /** Configures the log to not log at all. */
    OFF(OFF_PRESET_VALUE),
    ;

    public companion object {

        /**
         * Returns the [LoggingPreset] corresponding to the specified [name], or [LoggingPreset.DEFAULT] otherwise.
         *
         * The [name] is compared ignoring the case.
         */
        public fun valueOfOrDefault(name: String?): LoggingPreset =
            firstEnumValueOfOrNull<LoggingPreset> { it.name.equals(name, ignoreCase = true) } ?: DEFAULT
    }
}

/** The value that represents [LoggingPreset.DEFAULT]. */
public const val DEFAULT_PRESET_VALUE: String = "default"

/** The value that represents [LoggingPreset.SPRING]. */
public const val SPRING_PRESET_VALUE: String = "spring"

/** The value that represents [LoggingPreset.MINIMAL]. */
public const val MINIMAL_PRESET_VALUE: String = "minimal"

/** The value that represents [LoggingPreset.JSON]. */
public const val JSON_PRESET_VALUE: String = "json"

/** The value that represents [LoggingPreset.OFF]. */
public const val OFF_PRESET_VALUE: String = "off"
