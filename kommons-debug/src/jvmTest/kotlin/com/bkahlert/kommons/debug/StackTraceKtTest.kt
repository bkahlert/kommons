package com.bkahlert.kommons.debug

import com.bkahlert.kommons.test.testAll
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotStartWith
import io.kotest.matchers.string.shouldStartWith
import kotlin.test.Test

class StackTraceTest {

    @Test fun to_string() = testAll {
        var stackTrace = ""
        foo { bar { StackTrace.get().also { stackTrace = it.toString() }.firstOrNull() } }
        stackTrace.lines() should {
            it.size shouldBeGreaterThan 3
            it.first() shouldNotStartWith "    at "
            it.drop(1).shouldForAll { line -> line shouldStartWith "    at " }
        }
    }

    @Test fun get_first() = testAll {
        StackTrace.get().first() should {
            it.className shouldBe "com.bkahlert.kommons.debug.StackTraceTest"
            it.methodName shouldBe "get_first"
            it.fileName shouldBe "StackTraceKtTest.kt"
            it.lineNumber shouldBeGreaterThan 0
        }
    }

    @Test fun find_or_null() = testAll {
        foo { bar { StackTrace.get().findOrNull { it.methodName == "foo" } } } should { it?.methodName shouldStartWith "find_or_null" }
        foo { bar { StackTrace.get().findOrNull { false } } }.shouldBeNull()
    }

    @Test fun find_by_last_known_call_null() = testAll {
        val receiver = "com.bkahlert.kommons.debug.StackTraceKtTestKt"
        foo { bar { StackTrace.get().findByLastKnownCallsOrNull(receiver to "bar") } } should { it?.methodName shouldBe "foo" }
        foo { bar { StackTrace.get().findByLastKnownCallsOrNull("bar") } } should { it?.methodName shouldBe "foo" }
        foo { bar { StackTrace.get().findByLastKnownCallsOrNull(::bar) } } should { it?.methodName shouldBe "foo" }

        foo { bar { StackTrace.get().findByLastKnownCallsOrNull(receiver to "foo") } } should { it?.methodName shouldBe "find_by_last_known_call_null" }
        foo { bar { StackTrace.get().findByLastKnownCallsOrNull("foo") } } should { it?.methodName shouldBe "find_by_last_known_call_null" }
        foo { bar { StackTrace.get().findByLastKnownCallsOrNull(::foo) } } should { it?.methodName shouldBe "find_by_last_known_call_null" }

        foo { bar { StackTrace.get().findByLastKnownCallsOrNull(String::class, "toString") } }.shouldBeNull()
        foo { bar { StackTrace.get().findByLastKnownCallsOrNull("toString") } }.shouldBeNull()
        foo { bar { StackTrace.get().findByLastKnownCallsOrNull(String::toString) } }.shouldBeNull()
    }
}

internal fun foo(block: () -> StackTraceElement?) = block()
internal fun bar(block: () -> StackTraceElement?) = block()
