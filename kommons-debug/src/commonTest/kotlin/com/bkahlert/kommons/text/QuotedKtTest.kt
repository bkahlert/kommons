package com.bkahlert.kommons.text

import com.bkahlert.kommons.debug.ClassWithCustomToString
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class QuotedKtTest {

    @Test fun test_quotes() = testAll {
        "".quoted shouldBe "\"\""
        "foo".quoted shouldBe "\"foo\""
        "{ bar: \"baz\" }".quoted shouldBe "\"{ bar: \\\"baz\\\" }\""
        'a'.quoted shouldBe "\"a\""
        '"'.quoted shouldBe "\"\\\"\""
        ClassWithCustomToString().quoted shouldBe "\"custom toString\""
        @Suppress("CAST_NEVER_SUCCEEDS")
        (null as? ClassWithCustomToString).quoted shouldBe "null"
    }

    @Test fun test_escaped_backslash() = testAll {
        "\\".quoted shouldBe "\"\\\\\""
        '\\'.quoted shouldBe "\"\\\\\""
    }

    @Test fun test_escaped_line_feed() = testAll {
        "\n".quoted shouldBe "\"\\n\""
        '\n'.quoted shouldBe "\"\\n\""
    }

    @Test fun test_escaped_carriage_return() = testAll {
        "\r".quoted shouldBe "\"\\r\""
        '\r'.quoted shouldBe "\"\\r\""
    }

    @Test fun test_escaped_tab() = testAll {
        "\t".quoted shouldBe "\"\\t\""
        '\t'.quoted shouldBe "\"\\t\""
    }

    @Test fun test_escaped_double_quote() = testAll {
        "\"".quoted shouldBe "\"\\\"\""
        '"'.quoted shouldBe "\"\\\"\""
    }
}
