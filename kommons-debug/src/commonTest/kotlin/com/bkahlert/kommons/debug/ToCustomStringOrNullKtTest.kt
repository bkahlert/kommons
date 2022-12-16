package com.bkahlert.kommons.debug

import com.bkahlert.kommons.Platform
import com.bkahlert.kommons.Platform.Browser
import com.bkahlert.kommons.Platform.JVM
import com.bkahlert.kommons.Platform.NodeJS
import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.fail

class ToCustomStringOrNullTest {

    @Test fun test_null() = testAll {
        null.toCustomStringOrNull() shouldBe null
    }

    @Test fun test_string() = testAll {
        "string".toCustomStringOrNull() shouldBe "string"
    }

    @Test fun test_lambda() = testAll {
        {}.toCustomStringOrNull() shouldMatchGlob when (Platform.Current) {
            Browser, NodeJS -> "function ToCustomStringOrNullTest\$test_lambda\$lambda*() {\n" +
                "    return Unit_getInstance();\n" +
                "  }"

            JVM -> """
                () -> kotlin.Unit
            """.trimIndent()

            else -> fail("untested platform")
        }
    }

    @Test fun test_object_with_default_toString() = testAll {
        ClassWithDefaultToString().toCustomStringOrNull() shouldBe null
    }

    @Test fun test_object_with_custom_toString() = testAll {
        ClassWithCustomToString().toCustomStringOrNull() shouldBe "custom toString"
    }
}
