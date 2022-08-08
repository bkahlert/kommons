package com.bkahlert.kommons.debug

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class JvmStackTraceTest {

    @Test fun find_by_last_known_call_null() = testAll {
        val receiver = "com.bkahlert.kommons.debug.StackTraceKtTestKt"
        foo { bar { StackTrace.get().findByLastKnownCallsOrNull(receiver to "bar") } } should { it?.function shouldBe "foo" }
        foo { bar { StackTrace.get().findByLastKnownCallsOrNull("bar") } } should { it?.function shouldBe "foo" }
        foo { bar { StackTrace.get().findByLastKnownCallsOrNull(::bar) } } should { it?.function shouldBe "foo" }

        foo { bar { StackTrace.get().findByLastKnownCallsOrNull(receiver to "foo") } } should { it?.function shouldBe "find_by_last_known_call_null" }
        foo { bar { StackTrace.get().findByLastKnownCallsOrNull("foo") } } should { it?.function shouldBe "find_by_last_known_call_null" }
        foo { bar { StackTrace.get().findByLastKnownCallsOrNull(::foo) } } should { it?.function shouldBe "find_by_last_known_call_null" }

        foo { bar { StackTrace.get().findByLastKnownCallsOrNull(String::class, "toString") } }.shouldBeNull()
        foo { bar { StackTrace.get().findByLastKnownCallsOrNull("toString") } }.shouldBeNull()
        foo { bar { StackTrace.get().findByLastKnownCallsOrNull(String::toString) } }.shouldBeNull()
    }

    @Test fun equality() = testAll {
        stackTraceElementWithColumn shouldNotBe JvmStackTraceElement("any.package.AnyReceiver", "anyFun", file, 5, null)
        stackTraceElement shouldBe JvmStackTraceElement("any.package.AnyReceiver", "anyFun", file, 5, null)
    }

    @Test fun to_string() = testAll {
        stackTraceElementWithColumn.toString() shouldBe "any.package.AnyReceiver.anyFun($file:5:20)"
        stackTraceElementWithNegativeLine.toString() shouldBe "any.package.AnyReceiver.anyFun($file:-5)"
        stackTraceElement.toString() shouldBe "any.package.AnyReceiver.anyFun($file:5)"
    }
}

private const val file = "AnyReceiverImpl.kt"
internal val stackTraceElementWithColumn = JvmStackTraceElement("any.package.AnyReceiver", "anyFun", file, 5, 20)
internal val stackTraceElementWithNegativeLine = JvmStackTraceElement("any.package.AnyReceiver", "anyFun", file, -5, null)
internal val stackTraceElement = JvmStackTraceElement("any.package.AnyReceiver", "anyFun", file, 5, null)
