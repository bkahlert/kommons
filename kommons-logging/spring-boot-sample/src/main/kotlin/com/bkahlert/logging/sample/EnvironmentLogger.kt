package com.bkahlert.kommons.sample

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
import net.logstash.logback.argument.StructuredArguments
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.PropertySource
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.stereotype.Component
import java.util.AbstractMap.SimpleImmutableEntry
import java.util.Collections
import java.util.stream.Collectors
import kotlin.collections.Map.Entry

@Component
class EnvironmentLogger(
    val environment: ConfigurableEnvironment,
) {
    private val logger = getLogger(javaClass)

    @Autowired
    @EventListener
    fun contextRefreshedEvent(event: ContextRefreshedEvent) {
        val environment = event.applicationContext.environment as? ConfigurableEnvironment ?: throw IllegalStateException()
        logEnvironment(environment)
    }

    companion object {
        private val logger = getLogger(EnvironmentLogger::class.java)

        fun logEnvironment(environment: ConfigurableEnvironment) {
            val allProperties = environment.propertySources.stream()
                .map { propertySource: PropertySource<*> -> toEntryStream(propertySource) }
                .collect(
                    Collectors.toMap(
                        { (key, _) -> key },
                        { (_, value) -> value },
                        { _: Any?, b: Any? -> b })
                )
            val json: String = Jackson2ObjectMapperBuilder.json().featuresToEnable(INDENT_OUTPUT).build<ObjectMapper>().writeValueAsString(allProperties)
            logger.debug("Configuration: {}", json, StructuredArguments.entries(allProperties))
        }

        private fun <T> entry(key: String, value: T): Entry<String, T> {
            return SimpleImmutableEntry(key, value)
        }

        private fun toEntryStream(propertySource: PropertySource<*>): Entry<String, Map<String, Any?>> {
            val properties: MutableMap<String, Any?> = LinkedHashMap()
            try {
                properties.putAll(mapOf(propertySource.source))
            } catch (e: RuntimeException) {
                properties.putAll(Collections.singletonMap("unknown-error", e.message))
            }
            return entry<Map<String, Any?>>(getName(propertySource), properties)
        }

        private fun getName(enumerablePropertySource: PropertySource<*>): String {
            return enumerablePropertySource.javaClass.simpleName + "[" + enumerablePropertySource.name + "]"
        }

        private fun mapOf(source: Any): Map<String, Any?> {
            return Jackson2ObjectMapperBuilder.json().build<ObjectMapper>()
                .convertValue(source, object : TypeReference<Map<String, Any?>>() {})
        }
    }
}
