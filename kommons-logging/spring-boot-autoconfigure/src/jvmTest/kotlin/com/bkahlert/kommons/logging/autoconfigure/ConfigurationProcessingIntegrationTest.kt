package com.bkahlert.kommons.autoconfigure

import com.bkahlert.kommons.logging.autoconfigure.logback.LoggingProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata
import org.springframework.util.ClassUtils
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isGreaterThan
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import strikt.assertions.length
import java.util.stream.Stream
import kotlin.streams.asSequence

class ConfigurationProcessingIntegrationTest {

    @Test
    fun should_test_all_configured_properties() {
        expectThat(configuredProperties().map { it.name }.asSequence().toList())
            .containsExactlyInAnyOrder(testedProperties().asSequence().toList())
    }

    @Test
    fun should_configure_all_tested_properties() {
        expectThat(testedProperties().asSequence().toList())
            .containsExactlyInAnyOrder(configuredProperties().map { it.name }.asSequence().toList())
    }

    @ParameterizedTest
    @MethodSource("configuredProperties")
    fun should_contain_fully_documented_properties(itemMetadata: ItemMetadata) {
        expectThat(itemMetadata) {
            get { name }.isNotNull()
            get { ClassUtils.isPresent(type, null) }.isTrue()
            get { description }.length.isGreaterThan(10)
            get { ClassUtils.isPresent(sourceType, null) }.isTrue()
            get { defaultValue }.isNotNull()
        }
    }

    private fun getTestDescription(property: String?, field: String?): String? {
        return "expect $property to have $field configured"
    }

    companion object {
        @JvmStatic
        fun testedProperties(): Stream<String> =
            Stream.of(LoggingProperties.propertyNames).flatMap { it.stream() }

        @JvmStatic
        fun configuredProperties(): Stream<ItemMetadata> =
            MetadataStoreReader.metadataStore.readMetadata()
                .items
                .stream()
                .filter { !it.type.endsWith("Properties") }
    }
}
