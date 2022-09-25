package com.bkahlert.kommons_deprecated.io

import com.bkahlert.kommons_deprecated.test.expecting
import org.junit.jupiter.api.Test
import strikt.assertions.isEqualTo

class InMemoryBinaryFileTest {

    private val binaryFile = InMemoryBinaryFile("binary-fixture", "binary".toByteArray())

    @Test
    fun `should have name`() {
        expecting { binaryFile.name } that { isEqualTo("binary-fixture") }
    }

    @Test
    fun `should have data`() {
        expecting { binaryFile.data } that { isEqualTo("binary".toByteArray()) }
    }
}
