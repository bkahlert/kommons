package com.bkahlert.kommons.logging.autoconfigure

import com.bkahlert.kommons.autoconfigure.SpringCloudDetection
import com.bkahlert.kommons.autoconfigure.logback.Banners.SPRING_MAIN_BANNER_MODE
import com.bkahlert.kommons.logging.autoconfigure.logback.LoggingProperties
import com.bkahlert.kommons.logging.logback.AppenderPreset
import org.springframework.boot.Banner
import org.springframework.boot.logging.LogFile

class Properties(
    private val properties: MutableMap<String, Any?> = mutableMapOf(),
) : Map<String, Any?> by properties {
    fun anyPort(): Properties = apply { properties["server.port"] = 0 }
    fun includeActuatorEndpoint(id: String?): Properties = apply { properties["management.endpoints.web.exposure.include"] = id }
    fun springCloudBooststrap(enabled: Boolean): Properties = apply { properties[SpringCloudDetection.SPRING_CLOUD_BOOTSTRAP_ENABLED] = enabled }
    fun loggingFilePath(path: String): Properties = apply { if (path.isNotEmpty()) properties[LogFile.FILE_PATH_PROPERTY] = path }
    fun loggingFileName(name: String): Properties = apply { if (name.isNotEmpty()) properties[LogFile.FILE_NAME_PROPERTY] = name }
    fun bannerMode(bannerMode: Banner.Mode): Properties = apply { properties[SPRING_MAIN_BANNER_MODE] = bannerMode }

    fun sharedEncoder(encoder: AppenderPreset): Properties =
        consoleEncoder(encoder).fileEncoder(encoder)

    fun consoleEncoder(encoder: AppenderPreset): Properties =
        apply { properties[LoggingProperties.CONSOLE_LOG_PRESET] = encoder.name }

    fun fileEncoder(encoder: AppenderPreset): Properties =
        apply { properties[LoggingProperties.FILE_LOG_PRESET] = encoder.name }
}
