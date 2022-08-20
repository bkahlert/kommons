package com.bkahlert.kommons.logging.spring

import com.bkahlert.kommons.logging.LoggingPreset
import com.bkahlert.kommons.logging.logback.Logback
import com.bkahlert.kommons.test.logging.PrintedLog
import com.bkahlert.kommons.test.logging.allLogs
import com.bkahlert.kommons.test.logging.shouldContain
import com.bkahlert.kommons.test.spring.Captured
import com.bkahlert.kommons.test.spring.logFile
import com.bkahlert.kommons.test.spring.properties
import com.bkahlert.kommons.test.spring.run
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forNone
import io.kotest.inspectors.forOne
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Isolated
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.ArgumentMatchers
import org.slf4j.MDC
import org.springframework.boot.Banner.Mode
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultMatcher
import java.nio.file.Path
import java.util.function.Consumer

@Isolated
class BannerIntegrationTest {

    @BeforeEach
    fun setUp() {
        Logback.reset()
        MDC.put("foo", "bar")
        MDC.put("baz", null)
    }

    private val springApplicationBuilder = SpringApplicationBuilder(TestConfig::class.java).properties {
        port = 0
        consoleLogPreset = LoggingPreset.OFF
        fileLogPreset = LoggingPreset.OFF
    }

    @Nested inner class SpringBootOnly {

        @BeforeEach fun setUp() {
            springApplicationBuilder.properties { springCloudBootstrap = false }
        }

        @Test fun no_spring_cloud() {
            springApplicationBuilder.run {
                SpringCloudDetection.isSpringCloud(it.shouldNotBeNull()) shouldBe false
            }
        }

        @Nested inner class UsingOffBannerMode {

            @BeforeEach fun setUp() {
                springApplicationBuilder.properties { bannerMode = Mode.OFF }
            }

            @EnumSource(VocalConsolePreset::class)
            @ParameterizedTest fun console(preset: VocalConsolePreset, @Captured output: CapturedOutput) {
                springApplicationBuilder.consoleLogPreset(preset).run {
                    output.allLogs.shouldContainNoBanner()
                }
            }

            @EnumSource(VocalConsolePreset::class)
            @ParameterizedTest fun file(preset: VocalConsolePreset, @TempDir tempDir: Path) {
                springApplicationBuilder.fileLogPreset(preset, tempDir).run {
                    it.logFile.shouldNotBeNull().allLogs.shouldContainNoBanner()
                }
            }
        }

        @Nested inner class UsingConsoleBannerMode {
            @BeforeEach fun setUp() {
                springApplicationBuilder.properties { bannerMode = Mode.CONSOLE }
            }

            @EnumSource(VocalConsolePreset::class)
            @ParameterizedTest fun console(preset: VocalConsolePreset, @Captured output: CapturedOutput) {
                springApplicationBuilder.consoleLogPreset(preset).run {
                    output.allLogs.shouldContainExactlyOneRegularBanner()
                }
            }

            @EnumSource(VocalConsolePreset::class)
            @ParameterizedTest fun file(preset: VocalConsolePreset, @TempDir tempDir: Path) {
                springApplicationBuilder.fileLogPreset(preset, tempDir).run {
                    it.logFile.shouldNotBeNull().allLogs.shouldContainNoBanner()
                }
            }
        }

        @Nested inner class UsingLogBannerMode {
            @BeforeEach fun setUp() {
                springApplicationBuilder.properties { bannerMode = Mode.LOG }
            }

            @EnumSource(VocalConsolePreset::class)
            @ParameterizedTest fun console(preset: VocalConsolePreset, @Captured output: CapturedOutput) {
                springApplicationBuilder.consoleLogPreset(preset).run {
                    output.allLogs.shouldContainExactlyOneRegularBanner()
                }
            }

            @EnumSource(VocalConsolePreset::class)
            @ParameterizedTest fun file(preset: VocalConsolePreset, @TempDir tempDir: Path) {
                springApplicationBuilder.fileLogPreset(preset, tempDir).run {
                    it.logFile.shouldNotBeNull().allLogs.shouldContainExactlyOneRegularBanner()
                }
            }
        }
    }

    @Nested inner class SpringCloud {

        @BeforeEach fun setUp() {
            springApplicationBuilder.properties { springCloudBootstrap = true }
        }

        @Test fun is_spring_cloud() {
            springApplicationBuilder.run {
                SpringCloudDetection.isSpringCloud(it.shouldNotBeNull()) shouldBe true
            }
        }

        @Nested inner class UsingOffBannerMode {

            @BeforeEach fun setUp() {
                springApplicationBuilder.properties { bannerMode = Mode.OFF }
            }

            @EnumSource(VocalConsolePreset::class)
            @ParameterizedTest fun console(preset: VocalConsolePreset, @Captured output: CapturedOutput) {
                springApplicationBuilder.consoleLogPreset(preset).run {
                    output.allLogs.shouldContainNoBanner()
                }
            }

            @EnumSource(VocalConsolePreset::class)
            @ParameterizedTest fun file(preset: VocalConsolePreset, @TempDir tempDir: Path) {
                springApplicationBuilder.fileLogPreset(preset, tempDir).run {
                    it.logFile.shouldNotBeNull().allLogs.shouldContainNoBanner()
                }
            }
        }

        @Nested inner class UsingConsoleBannerMode {
            @BeforeEach fun setUp() {
                springApplicationBuilder.properties { bannerMode = Mode.CONSOLE }
            }

            @EnumSource(VocalConsolePreset::class)
            @ParameterizedTest fun console(preset: VocalConsolePreset, @Captured output: CapturedOutput) {
                springApplicationBuilder.consoleLogPreset(preset).run {
                    output.allLogs.shouldContainExactlyOneCloudBanner()
                }
            }

            @EnumSource(VocalConsolePreset::class)
            @ParameterizedTest fun file(preset: VocalConsolePreset, @TempDir tempDir: Path) {
                springApplicationBuilder.fileLogPreset(preset, tempDir).run {
                    it.logFile.shouldNotBeNull().allLogs.shouldContainNoBanner()
                }
            }
        }

        @Nested inner class UsingLogBannerMode {
            @BeforeEach fun setUp() {
                springApplicationBuilder.properties { bannerMode = Mode.LOG }
            }

            @EnumSource(VocalConsolePreset::class)
            @ParameterizedTest fun console(preset: VocalConsolePreset, @Captured output: CapturedOutput) {
                springApplicationBuilder.consoleLogPreset(preset).run {
                    output.allLogs.shouldContainExactlyOneCloudBanner()
                }
            }

            @EnumSource(VocalConsolePreset::class)
            @ParameterizedTest fun file(preset: VocalConsolePreset, @TempDir tempDir: Path) {
                springApplicationBuilder.fileLogPreset(preset, tempDir).run {
                    it.logFile.shouldNotBeNull().allLogs.shouldContainExactlyOneCloudBanner()
                }
            }
        }
    }
}


/** Appender presets that have an effect. */
enum class VocalConsolePreset(val preset: LoggingPreset) {
    Spring(LoggingPreset.SPRING),
    Minimal(LoggingPreset.MINIMAL),
    Json(LoggingPreset.JSON),
}

fun SpringApplicationBuilder.consoleLogPreset(preset: VocalConsolePreset) =
    properties { consoleLogPreset = preset.preset }

fun SpringApplicationBuilder.fileLogPreset(preset: VocalConsolePreset, logDir: Path) =
    properties { fileLogPreset = preset.preset; logPath = logDir }

private const val SPRING_BOOT = ":: Spring Boot ::"
private const val SPRING_CLOUD = ":: Spring Cloud ::"

private val jsonPresetRegex = Regex(
    """
    ^\{"@timestamp":.*
""".trimIndent()
)
private val springPresetRegex = Regex(
    """
    ^\d{4}-\d{2}-\d{2}.*
""".trimIndent()
)
private val minimalPresetRegex = Regex(
    """
    ^\d{2}:\d{2}.\d{3}.*
""".trimIndent()
)


fun PrintedLog.shouldContainNoBanner() = should {
    it.forNone { it shouldContain SPRING_BOOT }
    it.forNone { it shouldContain SPRING_CLOUD }
}

fun PrintedLog.shouldContainExactlyOneRegularBanner() = should {
    it.forOne { it shouldContain SPRING_BOOT }
    it.forNone { it shouldContain SPRING_CLOUD }
}

fun PrintedLog.shouldContainExactlyOneCloudBanner() = should {
    it.forOne { it shouldContain SPRING_BOOT }
    it.forOne { it shouldContain SPRING_CLOUD }
}

fun PrintedLog.shouldContainOnlyJsonPresetEntries() = should {
    it.forAll { it shouldMatch jsonPresetRegex }
    it.forNone { it shouldMatch springPresetRegex }
    it.forNone { it shouldMatch minimalPresetRegex }
}

fun PrintedLog.shouldContainOnlySpringPresetEntries() = should {
    it.forNone { it shouldMatch jsonPresetRegex }
    it.forAll { it shouldMatch springPresetRegex }
    it.forNone { it shouldMatch minimalPresetRegex }
}

fun PrintedLog.shouldContainOnlyMinimalPresetEntries() = should {
    it.forNone { it shouldMatch jsonPresetRegex }
    it.forNone { it shouldMatch springPresetRegex }
    it.forAll { it shouldMatch minimalPresetRegex }
}

@Deprecated("inline")
fun containsOnlyJsonLogs(log: PrintedLog) = log.shouldContainOnlyJsonPresetEntries()

@Deprecated("inline")
fun containsOnlyPlainLogs(log: PrintedLog) = log.shouldContainOnlySpringPresetEntries()

@Deprecated("inline")
fun containsOnlyClassicLogs(log: PrintedLog) = log.shouldContainOnlyMinimalPresetEntries()

/**
 * Helper method to be used in conjunction with [org.mockito.Mockito.verify].<br></br>
 * This way you can use AssertJ for call verification completely omitting [org.mockito.ArgumentCaptor]s.
 *
 *
 * Before:
 * <pre>`ArgumentCaptor<List < CreditCardAccount>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
 * verify(mainCreditCardAccountFilter).apply(listArgumentCaptor.capture());
 * assertThat(listArgumentCaptor.getValue()).containsExactlyElementsOf(creditCardAccounts);
`</pre> *
 *
 *
 * After:
 * <pre>`verify(mainCreditCardAccountFilter).apply(should(cards -> assertThat(cards)
 * .containsExactlyElementsOf(creditCardAccounts)));
`</pre> *
 *
 * @param assertion
 * @param <T>
 *
 * @return
</T> */
fun <T> should(assertion: Consumer<T>): T {
    return ArgumentMatchers.argThat { argument: T ->
        assertion.accept(argument)
        true
    }
}

/**
 * Help method that can be used to check [MvcResult]s using AssertJ.
 *
 *
 * Example:
 * <pre>`mvc.perform(get("http://whatever.wherever")
 * .accept(MediaType.APPLICATION_JSON))
 * .andDo(print())
 * .andExpect(status().isOk())
 * .andExpect(result(r ->  assertThat(r)...);
`</pre> *
 *
 * @param assertion
 *
 * @return
 */
fun result(assertion: Consumer<MvcResult?>): ResultMatcher {
    return ResultMatcher { t: MvcResult? -> assertion.accept(t) }
}
