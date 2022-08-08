package com.bkahlert.kommons.text

import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons.text.LineSeparators.isSingleLine
import com.bkahlert.kommons.text.LineSeparators.lines
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import strikt.api.Assertion.Builder

class LineSeparatorExtensionsKtTest {

    @Test fun last_line_regex() = LineSeparators.Common.testAll { sep ->
        LAST_LINE_REGEX.matchEntire("") shouldBe null
        LAST_LINE_REGEX.matchEntire("line")?.groupValues.shouldContainExactly("line")
        LAST_LINE_REGEX.matchEntire("line${sep}") shouldBe null
        LAST_LINE_REGEX.matchEntire("line${sep}line") shouldBe null
    }

    @Test fun intermediary_line_pattern() = LineSeparators.Common.testAll { sep ->
        INTERMEDIARY_LINE_PATTERN.matchEntire("") shouldBe null
        INTERMEDIARY_LINE_PATTERN.matchEntire(sep)?.groupValues?.shouldContainExactly(sep, sep)
        INTERMEDIARY_LINE_PATTERN.matchEntire("line") shouldBe null
        INTERMEDIARY_LINE_PATTERN.matchEntire("line${sep}")?.groupValues?.get(1) shouldBe sep
        INTERMEDIARY_LINE_PATTERN.matchEntire("line${sep}line") shouldBe null
    }

    @Test fun line_pattern() = LineSeparators.Common.testAll { sep ->
        LINE_PATTERN.matchEntire("") shouldBe null
        LINE_PATTERN.matchEntire(sep)?.groupValues?.shouldContainExactly(sep, sep)
        LINE_PATTERN.matchEntire("line")?.groupValues?.get(0) shouldBe "line"
        LINE_PATTERN.matchEntire("line${sep}")?.groupValues?.get(1) shouldBe sep
        LINE_PATTERN.matchEntire("line${sep}line") shouldBe null
    }
}

fun <T : CharSequence> Builder<T>.isSingleLine() =
    assert("is single line") {
        if (it.isSingleLine()) pass()
        else fail("has ${it.lines().size} lines")
    }

fun <T : CharSequence> Builder<T>.lines(
    keepDelimiters: Boolean = false,
): Builder<List<String>> = get("lines %s") { lines(keepDelimiters = keepDelimiters).toList() }
