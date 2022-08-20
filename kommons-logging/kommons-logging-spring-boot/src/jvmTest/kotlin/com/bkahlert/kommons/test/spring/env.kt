package com.bkahlert.kommons.test.spring

import com.bkahlert.kommons.io.toPath
import com.bkahlert.kommons.logging.LoggingPreset
import com.bkahlert.kommons.logging.spring.Banners.SPRING_MAIN_BANNER_MODE
import com.bkahlert.kommons.logging.spring.LoggingProperties
import com.bkahlert.kommons.logging.spring.SpringCloudDetection
import com.bkahlert.kommons.text.takeUnlessEmpty
import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.logging.LogFile
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.test.util.ApplicationContextTestUtils
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.Environment
import org.springframework.core.env.EnvironmentCapable
import org.springframework.core.env.PropertyResolver
import java.nio.file.Path
import kotlin.io.path.pathString

/** The log file used this Spring [ApplicationContext]. */
val EnvironmentCapable.logFile: Path?
    get() = environment.logFile

/** The log file used this Spring [Environment]. */
val PropertyResolver.logFile: Path?
    get() = LogFile.get(this)?.toString()?.toPath()

/** Scope to build Spring properties. */
class SpringPropertiesScope(
    private val map: MutableMap<String, Any?>,
) : MutableMap<String, Any?> by map {

    var port: Int?
        get() = map["server.port"]?.toString()?.toInt()
        set(value) {
            map["server.port"] = value.toString()
        }

    var actuatorEndpoints: List<String>
        get() = map["management.endpoints.web.exposure.include"]?.toString()?.split(",")?.map { it.trim() } ?: emptyList()
        set(value) {
            map["management.endpoints.web.exposure.include"] = "*" // TODO value.joinToString(",")
        }

    var springCloudBootstrap: Boolean?
        get() = map[SpringCloudDetection.SPRING_CLOUD_BOOTSTRAP_ENABLED]?.toString()?.toBoolean()
        set(value) {
            map[SpringCloudDetection.SPRING_CLOUD_BOOTSTRAP_ENABLED] = value.toString()
        }

    /** The [LogFile.FILE_NAME_PROPERTY], that is, the directory holding the log. */
    var logFile: Path?
        get() = map[LogFile.FILE_NAME_PROPERTY]?.toString()?.toPath()
        set(value) {
            map[LogFile.FILE_NAME_PROPERTY] = value?.pathString
        }

    /** The [LogFile.FILE_PATH_PROPERTY], that is, the absolute path or the path relative to [LogFile.FILE_PATH_PROPERTY] holding the log.  */
    var logPath: Path?
        get() = map[LogFile.FILE_PATH_PROPERTY]?.toString()?.toPath()
        set(value) {
            map[LogFile.FILE_PATH_PROPERTY] = value?.pathString
        }

    var bannerMode: Banner.Mode?
        get() = map[SPRING_MAIN_BANNER_MODE]?.toString()?.takeUnlessEmpty()?.let { Banner.Mode.valueOf(it) }
        set(value) {
            map[SPRING_MAIN_BANNER_MODE] = value?.name
        }

    var consoleLogPreset: LoggingPreset?
        get() = map[LoggingProperties.CONSOLE_LOG_PRESET_PROPERTY]?.toString()?.let { LoggingPreset.valueOfOrDefault(it) }
        set(preset) {
            map[LoggingProperties.CONSOLE_LOG_PRESET_PROPERTY] = preset?.value
        }

    var fileLogPreset: LoggingPreset?
        get() = map[LoggingProperties.FILE_LOG_PRESET_PROPERTY]?.toString()?.let { LoggingPreset.valueOfOrDefault(it) }
        set(preset) {
            map[LoggingProperties.FILE_LOG_PRESET_PROPERTY] = preset?.value
        }
}

/** Builds a map of Spring properties using the specified [block]. */
fun buildSpringProperties(block: SpringPropertiesScope.() -> Unit): Map<String, Any?> =
    mutableMapOf<String, Any?>().also { SpringPropertiesScope(it).block() }

/** Adds the properties built using the specified [block] to the environment. */
fun SpringApplicationBuilder.properties(block: SpringPropertiesScope.() -> Unit): SpringApplicationBuilder =
    properties(buildSpringProperties(block))

/**
 * Create an application context (and its parent if specified) with the specified command line [args],
 * passes it to the specified [block] and closes on upon completion.
 */
fun SpringApplicationBuilder.run(vararg args: String, block: (ConfigurableApplicationContext) -> Unit) {
    val context = build().run(*args)
    val result = kotlin.runCatching { block(context) }
    ApplicationContextTestUtils.closeAll(context)
    return result.getOrThrow()
}

/** Adds the properties built using the specified [block] to the environment. */
fun ApplicationContextRunner.withPropertyValues(block: SpringPropertiesScope.() -> Unit): ApplicationContextRunner =
    withPropertyValues(*buildSpringProperties(block)
        .entries
        .map { (key, value) -> "$key=$value" }
        .toTypedArray())

/** Registers the specified configuration [T] as an [AutoConfiguration] with the [ApplicationContext]. */
inline fun <reified T> ApplicationContextRunner.withAutoConfiguration(): ApplicationContextRunner =
    withConfiguration(AutoConfigurations.of(T::class.java))


/** Registers the specified user configuration [T] as a [AutoConfiguration] with the [ApplicationContext]. */
inline fun <reified T> ApplicationContextRunner.withUserConfiguration(): ApplicationContextRunner =
    withUserConfiguration(T::class.java)
