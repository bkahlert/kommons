package com.bkahlert.logging.support

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PropertiesLoaderUtils
import java.util.Properties

interface BuildInfoProperties {

    val properties: Properties

    fun getProperty(artifact: String): String? = properties.getProperty("build.$artifact")

    val artifact: String? get() = getProperty("artifact")
    val encodingReporting: String? get() = getProperty("encoding.reporting")
    val encodingSource: String? get() = getProperty("encoding.source")
    val group: String? get() = getProperty("group")
    val javaSource: String? get() = getProperty("java.source")
    val javaTarget: String? get() = getProperty("java.target")
    val name: String? get() = getProperty("name")
    val time: String? get() = getProperty("time")
    val version: String? get() = getProperty("version")

    companion object {
        fun load(path: String = DEFAULT_LOCATION): BuildInfoProperties? {
            val resource: Resource = ClassPathResource(path)
            val properties: Properties = PropertiesLoaderUtils.loadProperties(resource)
            return object : BuildInfoProperties {
                override val properties: Properties = properties
            }
        }

        const val DEFAULT_LOCATION = "META-INF/build-info.properties"
    }
}
