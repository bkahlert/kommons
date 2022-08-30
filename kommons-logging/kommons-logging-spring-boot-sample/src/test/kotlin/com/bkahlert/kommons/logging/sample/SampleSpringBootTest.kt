package com.bkahlert.kommons.logging.sample

import com.bkahlert.kommons.logging.core.SLF4J
import com.bkahlert.kommons.logging.logback.Logback
import io.kotest.inspectors.forAll
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.should
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Isolated
import org.springframework.boot.logging.LogFile
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.core.env.Environment
import java.nio.file.Paths
import java.util.UUID
import kotlin.io.path.readLines

@Isolated
@SpringBootTest(
    properties = [
        "logging.preset.console=minimal",
        "logging.preset.file=json",
    ]
)
@ExtendWith(OutputCaptureExtension::class)
class SampleSpringBootTest(val environment: Environment) {

    private val logger by SLF4J

    @Test fun `should log`(output: CapturedOutput) {
        val message = "Spring Boot test running... 🏃💨"
        logger.info(message)
        output.out shouldContain message
    }

    @Test fun `should log to log file`() {
        val message = UUID.randomUUID().toString()
        logger.info(message)
        listOf(
            Logback.activeLogFile,
            LogFile.get(environment)?.toString()?.let { Paths.get(it) },
        ).forAll {
            it.shouldNotBeNull() should {
                it.shouldExist()
                it.readLines().dropLastWhile { it.isEmpty() }.last() shouldContain message
            }
        }
    }
}
