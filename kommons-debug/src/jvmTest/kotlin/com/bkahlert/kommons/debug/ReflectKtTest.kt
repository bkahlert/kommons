package com.bkahlert.kommons.debug

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.reflect.jvm.javaField

class ReflectKtTest {

    @Test fun accessible() {
        val instance = BaseClass()
        val privateProperty = checkNotNull(instance.kProperties0().single { it.name.startsWith("private") }.javaField)

        privateProperty.accessible shouldBe false
        shouldThrow<IllegalAccessException> { privateProperty.get(instance) }

        privateProperty.accessible = true

        privateProperty.accessible shouldBe true
        privateProperty.get(instance) shouldBe "private-base-property"
    }
}
