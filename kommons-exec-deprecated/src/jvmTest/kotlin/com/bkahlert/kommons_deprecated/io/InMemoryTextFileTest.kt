package com.bkahlert.kommons_deprecated.io

import com.bkahlert.kommons.text.LineSeparators
import com.bkahlert.kommons_deprecated.test.expecting
import org.junit.jupiter.api.Test
import strikt.assertions.isEqualTo

class InMemoryTextFileTest {

    private val textFile = InMemoryTextFile("text-fixture", "line 1\nline 2${LineSeparators.LF}")

    @Test
    fun `should have name`() {
        expecting { textFile.name } that { isEqualTo("text-fixture") }
    }

    @Test
    fun `should have data`() {
        expecting { textFile.data } that { isEqualTo("line 1\nline 2${LineSeparators.LF}".toByteArray()) }
    }

    @Test
    fun `should have text`() {
        expecting { textFile.text } that { isEqualTo("line 1\nline 2${LineSeparators.LF}") }
    }
}