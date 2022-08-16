package com.bkahlert.logging.autoconfigure

import com.bkahlert.logging.autoconfigure.logback.Banners.SPRING_MAIN_BANNER_MODE
import com.bkahlert.logging.autoconfigure.logback.LogbackProperties
import com.bkahlert.logging.logback.LogbackConfiguration.Encoder
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
    fun sharedEncoder(encoder: Encoder): Properties = consoleEncoder(encoder).fileEncoder(encoder)
    fun consoleEncoder(encoder: Encoder): Properties = apply { properties[LogbackProperties.CONSOLE] = encoder.name }
    fun fileEncoder(encoder: Encoder): Properties = apply { properties[LogbackProperties.FILE] = encoder.name }
}
