package com.bkahlert.kommons.logging

import com.bkahlert.kommons.debug.properties
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import mu.KLogger
import mu.KotlinLogging
import kotlin.test.Test

class JsDelegateKtTest {

    @Test fun logger_type() {
        val logger: KLogger by KotlinLogging
        logger.shouldBeInstanceOf<KLogger>()
    }

    @Test fun class_with_derived_logger_field() {
        ClassWithDerivedLoggerField().logger.name shouldBe "ClassWithDerivedLoggerField"
    }

    @Test fun class_with_companion_with_derived_logger_field() {
        ClassWithCompanionWithDerivedLoggerField.logger.name shouldBe "Companion"
    }

    @Test fun class_with_named_companion_with_derived_logger_field() {
        ClassWithNamedCompanionWithDerivedLoggerField.logger.name shouldBe "Named"
    }

    @Test fun singleton_with_derived_logger_field() {
        SingletonWithDerivedLoggerField.logger.name shouldBe "SingletonWithDerivedLoggerField"
    }

    @Test fun file_class_with_derived_logger_field() {
        logger.name shouldBe "init_properties_fixtures_kt"
    }

    @Test fun locally_derived_logger_field() {
        val logger by KotlinLogging
        // V8 (Node, Chrome) names the method frame `JsDelegateKtTest.locally_derived_logger_field`. Firefox cannot name
        // a Kotlin 2 method (`protoOf(C).m = function () {}`), so the enclosing class is unknown and the fallback applies.
        logger.name shouldBe if (isFirefox) "<global>" else "JsDelegateKtTest"
    }
}

private val isFirefox: Boolean = js("typeof navigator !== 'undefined' && /Firefox/.test(navigator.userAgent)") as Boolean

val KLogger.name: String
    get() = checkNotNull(properties["loggerName"] as? String) { "Failed to find logger name of $this" }
