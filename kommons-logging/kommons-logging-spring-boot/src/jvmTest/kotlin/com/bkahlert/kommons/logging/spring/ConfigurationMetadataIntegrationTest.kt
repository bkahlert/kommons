package com.bkahlert.kommons.logging.spring

import com.bkahlert.kommons.Program
import com.bkahlert.kommons.logging.LoggingPreset
import com.bkahlert.kommons.logging.spring.LoggingProperties.PresetProperties
import com.bkahlert.kommons.test.Slow
import com.bkahlert.kommons.test.testAll
import io.kotest.inspectors.forAny
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContainInOrder
import org.junit.jupiter.api.Test
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata
import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller
import org.springframework.util.ClassUtils
import kotlin.reflect.KClass

class ConfigurationMetadataIntegrationTest {

    @Slow
    @Test fun logging_preset_console() = testAll {
        configuredProperties.forAny {
            it.name shouldBe LoggingProperties.CONSOLE_LOG_PRESET_PROPERTY
            it.kClass shouldBe LoggingPreset::class
            it.description.shouldContainInOrder("Preset", "CONSOLE log")
            it.sourceKClass shouldBe PresetProperties::class
        }
    }

    @Slow
    @Test fun logging_preset_file() = testAll {
        configuredProperties.forAny {
            it.name shouldBe LoggingProperties.FILE_LOG_PRESET_PROPERTY
            it.kClass shouldBe LoggingPreset::class
            it.description.shouldContainInOrder("Preset", "FILE log")
            it.sourceKClass shouldBe PresetProperties::class
        }
    }

    companion object {
        private val METADATA_PATHS = listOf(
            "META-INF/spring-configuration-metadata.json",
            "META-INF/additional-spring-configuration-metadata.json",
        )

        /** The items of the generated and the additional metadata, which IDEs both read. */
        val configuredProperties: List<ItemMetadata>
            get() = METADATA_PATHS.flatMap { path ->
                checkNotNull(Program.contextClassLoader.getResourceAsStream(path)) { "$path not found on the test classpath" }
                    .use { JsonMarshaller().read(it).items }
            }
    }
}

val ItemMetadata.kClass: KClass<*>?
    get() = type?.let { ClassUtils.resolveClassName(it, null).kotlin }

val ItemMetadata.sourceKClass: KClass<*>?
    get() = sourceType?.let { ClassUtils.resolveClassName(it, null).kotlin }
