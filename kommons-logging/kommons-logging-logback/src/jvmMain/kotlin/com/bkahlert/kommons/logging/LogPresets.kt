package com.bkahlert.kommons.logging

/** The system property values used to configure log presets. */
public object LogPresets {

    /** Explicitly configures a log to use default logging settings. */
    public const val DEFAULT_PRESET: String = "default"

    /** Configures a log to use the default Spring Boot logging settings. */
    public const val SPRING_PRESET: String = "spring"

    /**
     * Configures a log to use only log:
     * - minutes to milliseconds,
     * - the log level
     * - the logger, and
     * - the log message
     */
    public const val MINIMAL_PRESET: String = "minimal"

    /** Configures a log to log using the JSON format. */
    public const val JSON_PRESET: String = "json"

    /** Configures a log to not log at all. */
    public const val OFF_PRESET: String = "off"
}
