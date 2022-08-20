package com.bkahlert.kommons.logging.spring

import com.bkahlert.kommons.logging.LoggingPreset
import com.bkahlert.kommons.logging.LoggingPreset.JSON
import com.bkahlert.kommons.logging.LoggingPreset.MINIMAL
import com.bkahlert.kommons.test.spring.withAutoConfiguration
import com.bkahlert.kommons.test.spring.withPropertyValues
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.context.runner.ApplicationContextRunner

@Isolated
class LoggingPropertiesTest {

    @Test fun without_autoconfiguration() {
        ApplicationContextRunner()
            .run {
                it.containsBean(CONFIG_BEAN_NAME) shouldBe false
            }
    }

    @Test fun with_autoconfiguration() {
        ApplicationContextRunner()
            .withAutoConfiguration<LoggingAutoConfiguration>()
            .run {
                it.containsBean(CONFIG_BEAN_NAME) shouldBe true
            }
    }

    @Test fun default_properties() {
        ApplicationContextRunner()
            .withAutoConfiguration<LoggingAutoConfiguration>()
            .run {
                it.getBean<LoggingProperties>().preset.console shouldBe LoggingPreset.DEFAULT
                it.getBean<LoggingProperties>().preset.file shouldBe LoggingPreset.DEFAULT
            }
    }

    @Test fun custom_properties() {
        ApplicationContextRunner()
            .withAutoConfiguration<LoggingAutoConfiguration>()
            .withPropertyValues {
                consoleLogPreset = JSON
                fileLogPreset = MINIMAL
            }.run { ctx ->
                ctx.getBean<LoggingProperties>() should {
                    it.preset.console shouldBe JSON
                    it.preset.file shouldBe MINIMAL
                }
            }
    }

    companion object {
        /** `logging-com.bkahlert.kommons.autoconfigure.logging.LoggingProperties` */
        val CONFIG_BEAN_NAME: String = LoggingProperties.PREFIX + "-" + LoggingProperties::class.java.name
    }
}
