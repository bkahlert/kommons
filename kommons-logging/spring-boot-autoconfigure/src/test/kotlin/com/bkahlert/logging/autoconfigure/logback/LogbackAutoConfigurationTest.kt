package com.bkahlert.logging.autoconfigure.logback

import com.bkahlert.logging.autoconfigure.Properties
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.util.function.Consumer
import java.util.stream.Stream

@ExtendWith(SmartOutputCaptureExtension::class)
class LogbackAutoConfigurationTest {
    var propertiesBuilder: PropertiesBuilder? = null

    @Nullable
    var context: ConfigurableApplicationContext? = null
    @AfterEach fun tearDown() {
        ApplicationContextTestUtils.closeAll(context)
    }

    @BeforeEach fun setUp() {
        propertiesBuilder = Properties.builder().anyPort().consoleEncoder(Encoder.plain).fileEncoder(Encoder.json)
    }

    @Test fun should_be_disabled_on_missing_auto_configuration(?) {
        val contextRunner: ApplicationContextRunner = ApplicationContextRunner()
            .withUserConfiguration(RequestLoggingAutoConfiguration::class.java)
        contextRunner.run(ContextConsumer<AssertableApplicationContext?> { ctx: AssertableApplicationContext? ->
            expectThat(ctx.containsBean(
                CONFIG_BEAN_NAME)).isFalse()
        })
    }

    @Test fun should_be_enabled_by_default(?) {
        val contextRunner: ApplicationContextRunner = ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LogbackAutoConfiguration::class.java))
        contextRunner.run(ContextConsumer<AssertableApplicationContext?> { ctx: AssertableApplicationContext? ->
            expectThat(ctx.containsBean(
                CONFIG_BEAN_NAME)).isTrue()
        })
    }

    @ParameterizedTest @ValueSource(strings = ["true", "false", "invalid"]) fun should_be_enabled_independent_of_request_logging(
        value: String?,
        ?,
    ) {
        val contextRunner: ApplicationContextRunner = ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LogbackAutoConfiguration::class.java))
            .withPropertyValues(RequestLoggingProperties.HTTP_ENABLED + "=" + value)
        contextRunner.run(ContextConsumer<AssertableApplicationContext?> { ctx: AssertableApplicationContext? ->
            expectThat(ctx.containsBean(
                CONFIG_BEAN_NAME)).isTrue()
        })
    }

    @Test fun should_have_default_configuration(?) {
        val contextRunner: ApplicationContextRunner = ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LogbackAutoConfiguration::class.java))
        contextRunner.run(ContextConsumer<AssertableApplicationContext?> { ctx: AssertableApplicationContext? ->
            val logbackProperties: LogbackProperties = ctx.getBean<LogbackProperties?>(LogbackProperties::class.java)
            expectThat<LogbackProperties?>(logbackProperties).isNotNull()
            expectThat(logbackProperties.appenders.console).isEqualTo(Encoder.preset.name)
            expectThat(logbackProperties.appenders.file).isEqualTo(Encoder.preset.name)
        })
    }

    @Test fun should_reflect_configuration(?) {
        val contextRunner: ApplicationContextRunner = ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LogbackAutoConfiguration::class.java))
            .withUserConfiguration(TestConfig::class.java)
            .withPropertyValues(LogbackProperties.CONSOLE + "=json")
            .withPropertyValues(LogbackProperties.FILE + "=classic")
        contextRunner.run(ContextConsumer<AssertableApplicationContext?> { ctx: AssertableApplicationContext? ->
            val logbackProperties: LogbackProperties = ctx.getBean<LogbackProperties?>(LogbackProperties::class.java)
            expectThat<LogbackProperties?>(logbackProperties).isNotNull()
            expectThat(logbackProperties.appenders.console).isEqualTo("json")
            expectThat(logbackProperties.appenders.file).isEqualTo("classic")
        })
    }

    @Test fun should_configure_logger(?, output: SmartCapturedOutput?) {
        context = SpringApplicationBuilder(TestConfig::class.java).properties(Properties.builder()
            .consoleEncoder(Encoder.json)
            .fileEncoder(Encoder.json)
            .build()).run()
        val log = LoggerFactory.getLogger(LogbackAutoConfigurationTest::class.java)
        log!!.warn("Warning")
        log.error("Error", RuntimeException("Exception"))
        Stream.of(output.assertThatMappedJSON(softly, -2), loggedSoFar().assertThatMappedJSON(softly, -2)).forEach(Consumer<T?> { map: T? ->
            map
                .containsKeys(JsonLoggingTest.REQUIRED_FIELDS)
                .hasEntry("level", "WARN")
                .hasEntry("message", "Warning")
        })
        Stream.of(output.assertThatMappedJSON(softly, -1), loggedSoFar().assertThatMappedJSON(softly, -1)).forEach(Consumer<T?> { map: T? ->
            map
                .containsKeys(JsonLoggingTest.REQUIRED_FIELDS)
                .hasEntry("level", "ERROR")
                .hasEntry("message", "Error")
                .hasEntrySatisfying("stack-trace") { stacktrace ->
                    expectThat(stacktrace.toString())
                        .startsWith("""java.lang.RuntimeException: Exception
	at ${LogbackAutoConfigurationTest::class.java.name}""")
                }
        }
        )
    }

    @Configuration(proxyBeanMethods = false) @Import(
        LogConfiguringEnvironmentPostProcessor::class) @EnableAutoConfiguration(exclude = [LogbackAutoConfiguration::class])
    internal class TestConfig

    @Nested
    internal inner class with_regular_bootstrapping {
        @BeforeEach fun setUp() {
            propertiesBuilder = propertiesBuilder.springCloudBooststrap(false)
        }

        @Test fun should_disable_spring_cloud_bootstrapping(?) {
            context = SpringApplicationBuilder(TestConfig::class.java).properties(Properties.builder().springCloudBooststrap(false).build()).run()
            expectThat(SpringCloudDetection.isSpringCloud(context.getEnvironment())).isFalse()
        }

        @Nested
        internal inner class using_json_encoder {
            @BeforeEach fun setUp() {
                propertiesBuilder = propertiesBuilder.sharedEncoder(Encoder.json)
            }

            @ParameterizedTest @EnumSource(Banner.Mode::class) fun should_log_regular_banner(
                bannerMode: Banner.Mode?,
                ?,
                output: SmartCapturedOutput?,
                logged: SmartCapturedLog,
            ) {
                context = SpringApplicationBuilder(TestConfig::class.java).properties(propertiesBuilder.bannerMode(bannerMode).build()).run()
                Assertions.assertCorrectBannerAppearance(softly, bannerMode, false).accept(output, logged)
            }
        }

        @Nested
        internal inner class using_plain_encoder {
            @BeforeEach fun setUp() {
                propertiesBuilder = propertiesBuilder.sharedEncoder(Encoder.plain)
            }

            @ParameterizedTest @EnumSource(Banner.Mode::class) fun should_log_regular_banner(
                bannerMode: Banner.Mode?,
                ?,
                output: SmartCapturedOutput?,
                logged: SmartCapturedLog,
            ) {
                context = SpringApplicationBuilder(TestConfig::class.java).properties(propertiesBuilder.bannerMode(bannerMode).build()).run()
                Assertions.assertCorrectBannerAppearance(softly, bannerMode, false).accept(output, logged)
            }
        }

        @Nested
        internal inner class using_minimal_encoder {
            @BeforeEach fun setUp() {
                propertiesBuilder = propertiesBuilder.sharedEncoder(Encoder.minimal)
            }

            @ParameterizedTest @EnumSource(Banner.Mode::class) fun should_log_regular_banner(
                bannerMode: Banner.Mode?,
                ?,
                output: SmartCapturedOutput?,
                logged: SmartCapturedLog,
            ) {
                context = SpringApplicationBuilder(TestConfig::class.java).properties(propertiesBuilder.bannerMode(bannerMode).build()).run()
                Assertions.assertCorrectBannerAppearance(softly, bannerMode, false).accept(output, logged)
            }
        }

        @Nested
        internal inner class using_classic_encoder {
            @BeforeEach fun setUp() {
                propertiesBuilder = propertiesBuilder.sharedEncoder(Encoder.classic)
            }

            @ParameterizedTest @EnumSource(Banner.Mode::class) fun should_log_regular_banner(
                bannerMode: Banner.Mode?,
                ?,
                output: SmartCapturedOutput?,
                logged: SmartCapturedLog,
            ) {
                context = SpringApplicationBuilder(TestConfig::class.java).properties(propertiesBuilder.bannerMode(bannerMode).build()).run()
                Assertions.assertCorrectBannerAppearance(softly, bannerMode, false).accept(output, logged)
            }
        }
    }

    @Nested
    internal inner class with_spring_cloud_bootstrapping {
        @BeforeEach fun setUp() {
            propertiesBuilder = propertiesBuilder.springCloudBooststrap(true)
        }

        @Test fun should_enable_spring_cloud_bootstrapping(?) {
            context = SpringApplicationBuilder(TestConfig::class.java).properties(Properties.builder().springCloudBooststrap(true).build()).run()
            expectThat(SpringCloudDetection.isSpringCloud(context.getEnvironment())).isTrue()
        }

        @Nested
        internal inner class using_JSON_encoder {
            @BeforeEach fun setUp() {
                propertiesBuilder = propertiesBuilder.sharedEncoder(Encoder.json)
            }

            @ParameterizedTest @EnumSource(Banner.Mode::class) fun should_log_cloud_banner(
                bannerMode: Banner.Mode?,
                ?,
                output: SmartCapturedOutput?,
                logged: SmartCapturedLog,
            ) {
                context = SpringApplicationBuilder(TestConfig::class.java).properties(propertiesBuilder.bannerMode(bannerMode).build()).run()
                Assertions.assertCorrectBannerAppearance(softly, bannerMode, true).accept(output, logged)
            }
        }

        @Nested
        internal inner class using_plain_encoder {
            @BeforeEach fun setUp() {
                propertiesBuilder = propertiesBuilder.sharedEncoder(Encoder.plain)
            }

            @ParameterizedTest @EnumSource(Banner.Mode::class) fun should_log_cloud_banner(
                bannerMode: Banner.Mode?,
                ?,
                output: SmartCapturedOutput?,
                logged: SmartCapturedLog,
            ) {
                context = SpringApplicationBuilder(TestConfig::class.java).properties(propertiesBuilder.bannerMode(bannerMode).build()).run()
                Assertions.assertCorrectBannerAppearance(softly, bannerMode, true).accept(output, logged)
            }
        }

        @Nested
        internal inner class using_minimal_encoder {
            @BeforeEach fun setUp() {
                propertiesBuilder = propertiesBuilder.sharedEncoder(Encoder.minimal)
            }

            @ParameterizedTest @EnumSource(Banner.Mode::class) fun should_log_cloud_banner(
                bannerMode: Banner.Mode?,
                ?,
                output: SmartCapturedOutput?,
                logged: SmartCapturedLog,
            ) {
                context = SpringApplicationBuilder(TestConfig::class.java).properties(propertiesBuilder.bannerMode(bannerMode).build()).run()
                Assertions.assertCorrectBannerAppearance(softly, bannerMode, true).accept(output, logged)
            }
        }

        @Nested
        internal inner class using_classic_encoder {
            @BeforeEach fun setUp() {
                propertiesBuilder = propertiesBuilder.sharedEncoder(Encoder.classic)
            }

            @ParameterizedTest @EnumSource(Banner.Mode::class) fun should_log_cloud_banner(
                bannerMode: Banner.Mode?,
                ?,
                output: SmartCapturedOutput?,
                logged: SmartCapturedLog,
            ) {
                context = SpringApplicationBuilder(TestConfig::class.java).properties(propertiesBuilder.bannerMode(bannerMode).build()).run()
                Assertions.assertCorrectBannerAppearance(softly, bannerMode, true).accept(output, logged)
            }
        }
    }

    companion object {
        val CONFIG_BEAN_NAME: String? = LogbackProperties.PREFIX + "-" + LogbackProperties::class.java.name
    }
}
