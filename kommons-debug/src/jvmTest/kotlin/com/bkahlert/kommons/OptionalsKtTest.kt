package com.bkahlert.kommons

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import java.util.Optional
import kotlin.test.Test

class OptionalsTest {

    @Test fun or_null() = testAll {
        optionalPresentString.orNull() shouldBe "string"
        optionalNonPresentString.orNull() shouldBe null
        nullableOptionalPresentString.orNull() shouldBe "string"
        nullableOptionalNonPresentString.orNull() shouldBe null
        nullOptional.orNull() shouldBe null
    }
}

internal val optionalPresentString: Optional<String> = Optional.of("string")
internal val optionalNonPresentString: Optional<String> = Optional.ofNullable(null)
@Suppress("RedundantNullableReturnType") internal val nullableOptionalPresentString: Optional<String>? = optionalPresentString
@Suppress("RedundantNullableReturnType") internal val nullableOptionalNonPresentString: Optional<String>? = optionalNonPresentString
internal val nullOptional: Optional<String>? = null
