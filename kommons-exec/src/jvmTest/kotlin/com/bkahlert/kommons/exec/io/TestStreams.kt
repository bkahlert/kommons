package com.bkahlert.kommons.exec.io

import java.io.ByteArrayInputStream

internal interface TestStream {
    val closed: Boolean
}

internal class TestInputStream(
    input: String,
) : ByteArrayInputStream(input.toByteArray()), TestStream {
    override var closed = false
        private set

    override fun close() {
        closed = true
        super.close()
    }
}

internal class TestOutputStream : ByteArrayOutputStream(), TestStream {
    var flushed = false
        private set

    override fun flush() {
        flushed = true
        super.flush()
    }

    override var closed = false
        private set

    override fun close() {
        closed = true
        super.close()
    }
}

internal fun MutableList<TestStream>.testInputStream(input: String = "foo"): TestInputStream =
    TestInputStream(input).also { add(it) }


internal fun MutableList<TestStream>.testOutputStream(): TestOutputStream =
    TestOutputStream().also { add(it) }
