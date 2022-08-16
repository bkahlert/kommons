package com.bkahlert.logging.autoconfigure

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.test.util.ApplicationContextTestUtils
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Configuration

class AutoConfigurationIntegrationTest {

    private var context: ConfigurableApplicationContext? = null

    @AfterEach
    fun tearDown() {
        ApplicationContextTestUtils.closeAll(context)
    }

    @Test
    fun should_start_up() {
        context = SpringApplicationBuilder(DefaultConfiguration::class.java).run()
    }

    @Configuration(proxyBeanMethods = false) @EnableAutoConfiguration
    private class DefaultConfiguration
}
