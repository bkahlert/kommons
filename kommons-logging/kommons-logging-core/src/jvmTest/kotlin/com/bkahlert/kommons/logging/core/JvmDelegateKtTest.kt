package com.bkahlert.kommons.logging.core

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import mu.KLogger
import mu.KotlinLogging
import org.junit.jupiter.api.Test

class JvmDelegateKtTest {

    @Test fun logger_type() {
        val logger: KLogger by KotlinLogging
        logger.shouldBeInstanceOf<KLogger>()
    }

    @Test fun class_with_derived_logger_field() {
        ClassWithDerivedLoggerField().logger.name shouldBe "$Package.ClassWithDerivedLoggerField"
    }

    @Test fun class_with_companion_with_derived_logger_field() {
        ClassWithCompanionWithDerivedLoggerField.logger.name shouldBe "$Package.ClassWithCompanionWithDerivedLoggerField"
    }

    @Test fun class_with_named_companion_with_derived_logger_field() {
        ClassWithNamedCompanionWithDerivedLoggerField.logger.name shouldBe "$Package.ClassWithNamedCompanionWithDerivedLoggerField"
    }

    @Test fun singleton_with_derived_logger_field() {
        SingletonWithDerivedLoggerField.logger.name shouldBe "$Package.SingletonWithDerivedLoggerField"
    }

    @Test fun file_class_with_derived_logger_field() {
        logger.name shouldBe "$Package.FixturesKt"
    }

    @Test fun locally_derived_logger_field() {
        val logger by KotlinLogging
        logger.name shouldBe "$Package.JvmDelegateKtTest"
    }

    companion object {
        private const val Package: String = "com.bkahlert.kommons.logging.core"
    }
}
