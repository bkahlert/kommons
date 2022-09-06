package com.bkahlert.kommons.logging.spring

import com.bkahlert.kommons.logging.LoggingPreset.MINIMAL
import com.bkahlert.kommons.logging.LoggingPreset.SPRING
import com.bkahlert.kommons.test.spring.withPropertyValues
import com.bkahlert.kommons.test.spring.withUserConfiguration
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.context.runner.ApplicationContextRunner

@Isolated
class LoggingPropertiesTest {

    @Test fun undefined_properties() {
        ApplicationContextRunner()
            .withUserConfiguration<LoggingConfiguration>()
            .run {
                it.getBean<LoggingProperties>().preset.console shouldBe null
                it.getBean<LoggingProperties>().preset.file shouldBe null
            }
    }

    @Test fun application_properties() {
        ApplicationContextRunner()
            .withUserConfiguration<LoggingConfiguration>()
            .withPropertyValues {
                consoleLogPreset = MINIMAL
                fileLogPreset = SPRING
            }.run {
                it.getBean<LoggingProperties>().preset.console shouldBe MINIMAL
                it.getBean<LoggingProperties>().preset.file shouldBe SPRING
            }
    }
}
