package com.bkahlert.kommons.debug

import com.bkahlert.kommons.Parser.ParsingException
import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

class JsStackTraceTest {

    @Test fun find_by_last_known_call_null() = testAll {
        foo { bar { StackTrace.get().findByLastKnownCallsOrNull("bar") } } should { it?.demangledFunction shouldBe "foo" }
        foo { bar { StackTrace.get().findByLastKnownCallsOrNull(::bar) } } should { it?.demangledFunction shouldBe "foo" }

        foo { bar { StackTrace.get().findByLastKnownCallsOrNull("foo") } } should { it?.demangledFunction shouldBe "find_by_last_known_call_null" }
        foo { bar { StackTrace.get().findByLastKnownCallsOrNull(::foo) } } should { it?.demangledFunction shouldBe "find_by_last_known_call_null" }

        foo { bar { StackTrace.get().findByLastKnownCallsOrNull("toString") } }.shouldBeNull()
        foo { bar { StackTrace.get().findByLastKnownCallsOrNull(String::toString) } }.shouldBeNull()
    }

    @Test fun parse() = testAll {
        JsStackTraceElement.parse("AnyReceiver.anyFun_hash_k\$ ($file:5:20)") shouldBe stackTraceElementWithReceiverAndFunction
        JsStackTraceElement.parse("AnyReceiver.<anonymous> ($file:5:20)") shouldBe stackTraceElementWithReceiverAndAnonymousFunction
        JsStackTraceElement.parse("anyFun ($file:5:20)") shouldBe stackTraceElementWithFunction
        JsStackTraceElement.parse("$file:5:20") shouldBe stackTraceElement
        shouldThrow<ParsingException> { JsStackTraceElement.parse("").shouldBeNull() }
            .message shouldBe "Failed to parse \"\" into an instance of JsStackTraceElement"
    }

    @Test fun get_firefox_stack_trace() = testAll {
        StackTrace.get { firefoxStackTrace.lineSequence() }.shouldContainExactly(
            JsStackTraceElement(null, "trace", "webpack-internal:///./kotlin/hello.js", 1949, 35),
            JsStackTraceElement(null, "trace\$default", "webpack-internal:///./kotlin/hello.js", 2149, 12),
            JsStackTraceElement(null, "main", "webpack-internal:///./kotlin/hello.js", 2167, 18),
            JsStackTraceElement(null, null, "webpack-internal:///./kotlin/hello.js", 76997, 3),
            JsStackTraceElement(null, null, "webpack-internal:///./kotlin/hello.js", 5, 35),
            JsStackTraceElement(null, null, "webpack-internal:///./kotlin/hello.js", 8, 2),
            JsStackTraceElement(null, "./kotlin/hello.js", "http://localhost:8080/hello.js", 2928, 1),
            JsStackTraceElement(null, "__webpack_require__", "http://localhost:8080/hello.js", 3167, 33),
            JsStackTraceElement(null, null, "http://localhost:8080/hello.js", 4235, 56),
            JsStackTraceElement(null, null, "http://localhost:8080/hello.js", 4238, 12),
            JsStackTraceElement(null, "webpackUniversalModuleDefinition", "http://localhost:8080/hello.js", 17, 19),
            JsStackTraceElement(null, null, "http://localhost:8080/hello.js", 18, 3),
        )
    }

    @Test fun get_chrome_stack_trace() = testAll {
        StackTrace.get { chromeStackTrace.lineSequence() }.shouldContainExactly(
            JsStackTraceElement(null, "trace", "webpack-internal:///./kotlin/hello.js", 1949, 35),
            JsStackTraceElement(null, "trace\$default", "webpack-internal:///./kotlin/hello.js", 2149, 12),
            JsStackTraceElement(null, "main", "webpack-internal:///./kotlin/hello.js", 2167, 5),
            JsStackTraceElement("Object", "eval", "webpack-internal:///./kotlin/hello.js", 76997, 3),
            JsStackTraceElement(null, "eval", "webpack-internal:///./kotlin/hello.js", 5, 35),
            JsStackTraceElement(null, "eval", "webpack-internal:///./kotlin/hello.js", 8, 2),
            JsStackTraceElement("Object", "./kotlin/hello.js", "http://localhost:8080/hello.js", 2928, 1),
            JsStackTraceElement(null, "__webpack_require__", "http://localhost:8080/hello.js", 3167, 33),
            JsStackTraceElement(null, null, "http://localhost:8080/hello.js", 4235, 37),
            JsStackTraceElement(null, null, "http://localhost:8080/hello.js", 4238, 12),
        )
    }

    @Suppress("SpellCheckingInspection")
    @Test fun get_headless_chrome_stack_trace() = testAll {
        StackTrace.get { headlessChromeStackTrace.lineSequence() }.shouldContainExactly(
            JsStackTraceElement(null, "trace", "http://localhost:9876/absolute/karma/commons.js?16bb", 40617, 35),
            JsStackTraceElement(null, "trace\$default", "http://localhost:9876/absolute/karma/commons.js?16bb", 40817, 12),
            JsStackTraceElement("JsStackTraceTest", "parse_webpack_single2_g9jcxn_k\$", "http://localhost:9876/absolute/karma/commons.js?16bb", 32513, 25),
            JsStackTraceElement(null, null, "http://localhost:9876/absolute/karma/commons.js?16bb", 32684, 11),
            JsStackTraceElement("Context", "<anonymous>", "http://localhost:9876/absolute/karma/commons.js?16bb", 89557, 1320),
            JsStackTraceElement(null, "callFn", "http://localhost:9876/absolute/mocha.js?c5922bb0b011ecf8d1db8ab79e46bd46dbdf2833", 22834, 22),
            JsStackTraceElement("Runnable", "run", "http://localhost:9876/absolute/mocha.js?c5922bb0b011ecf8d1db8ab79e46bd46dbdf2833", 22820, 6),
            JsStackTraceElement("Runner", "runTest", "http://localhost:9876/absolute/mocha.js?c5922bb0b011ecf8d1db8ab79e46bd46dbdf2833", 24469, 11),
            JsStackTraceElement(null, null, "http://localhost:9876/absolute/mocha.js?c5922bb0b011ecf8d1db8ab79e46bd46dbdf2833", 24599, 13),
            JsStackTraceElement(null, "next", "http://localhost:9876/absolute/mocha.js?c5922bb0b011ecf8d1db8ab79e46bd46dbdf2833", 24376, 15),
        )
    }

    @Test fun equality() = testAll {
        stackTraceElementWithFunction shouldNotBe JsStackTraceElement(null, null, file, 5, 20)
        stackTraceElement shouldBe JsStackTraceElement(null, null, file, 5, 20)
    }

    @Test fun to_string() = testAll {
        stackTraceElementWithReceiverAndFunction.toString() shouldBe "AnyReceiver.anyFun_hash_k\$ ($file:5:20)"
        stackTraceElementWithReceiverAndAnonymousFunction.toString() shouldBe "AnyReceiver.<anonymous> ($file:5:20)"
        stackTraceElementWithFunction.toString() shouldBe "anyFun ($file:5:20)"
        stackTraceElement.toString() shouldBe "$file:5:20"
    }
}

private const val file = "http://localhost:9876/absolute/your/path/commons.js?09b8"
internal val stackTraceElementWithReceiverAndFunction = JsStackTraceElement("AnyReceiver", "anyFun_hash_k\$", file, 5, 20)
internal val stackTraceElementWithReceiverAndAnonymousFunction = JsStackTraceElement("AnyReceiver", "<anonymous>", file, 5, 20)
internal val stackTraceElementWithFunction = JsStackTraceElement(null, "anyFun", file, 5, 20)
internal val stackTraceElement = JsStackTraceElement(null, null, file, 5, 20)


internal val firefoxStackTrace = """
    captureStack@webpack-internal:///./kotlin/kotlin_kotlin.js:31066:26
    RuntimeException_init_${'$'}Create${'$'}@webpack-internal:///./kotlin/kotlin_kotlin.js:33739:17
    trace@webpack-internal:///./kotlin/hello.js:1949:35
    trace${'$'}default@webpack-internal:///./kotlin/hello.js:2149:12
    main@webpack-internal:///./kotlin/hello.js:2167:18
    @webpack-internal:///./kotlin/hello.js:76997:3
    @webpack-internal:///./kotlin/hello.js:5:35
    @webpack-internal:///./kotlin/hello.js:8:2
    ./kotlin/hello.js@http://localhost:8080/hello.js:2928:1
    __webpack_require__@http://localhost:8080/hello.js:3167:33
    @http://localhost:8080/hello.js:4235:56
    @http://localhost:8080/hello.js:4238:12
    webpackUniversalModuleDefinition@http://localhost:8080/hello.js:17:19
    @http://localhost:8080/hello.js:18:3
""".trimIndent()
internal val chromeStackTrace = """
    RuntimeException
        at trace (webpack-internal:///./kotlin/hello.js:1949:35)
        at trace${'$'}default (webpack-internal:///./kotlin/hello.js:2149:12)
        at main (webpack-internal:///./kotlin/hello.js:2167:5)
        at Object.eval (webpack-internal:///./kotlin/hello.js:76997:3)
        at eval (webpack-internal:///./kotlin/hello.js:5:35)
        at eval (webpack-internal:///./kotlin/hello.js:8:2)
        at Object../kotlin/hello.js (http://localhost:8080/hello.js:2928:1)
        at __webpack_require__ (http://localhost:8080/hello.js:3167:33)
        at http://localhost:8080/hello.js:4235:37
        at http://localhost:8080/hello.js:4238:12
""".trimIndent()

@Suppress("SpellCheckingInspection")
internal val headlessChromeStackTrace = """
    RuntimeException
        at trace (http://localhost:9876/absolute/karma/commons.js?16bb:40617:35)
        at trace${'$'}default (http://localhost:9876/absolute/karma/commons.js?16bb:40817:12)
        at JsStackTraceTest.parse_webpack_single2_g9jcxn_k${'$'} (http://localhost:9876/absolute/karma/commons.js?16bb:32513:25)
        at http://localhost:9876/absolute/karma/commons.js?16bb:32684:11
        at Context.<anonymous> (http://localhost:9876/absolute/karma/commons.js?16bb:89557:1320)
        at callFn (http://localhost:9876/absolute/mocha.js?c5922bb0b011ecf8d1db8ab79e46bd46dbdf2833:22834:22)
        at Runnable.run (http://localhost:9876/absolute/mocha.js?c5922bb0b011ecf8d1db8ab79e46bd46dbdf2833:22820:6)
        at Runner.runTest (http://localhost:9876/absolute/mocha.js?c5922bb0b011ecf8d1db8ab79e46bd46dbdf2833:24469:11)
        at http://localhost:9876/absolute/mocha.js?c5922bb0b011ecf8d1db8ab79e46bd46dbdf2833:24599:13
        at next (http://localhost:9876/absolute/mocha.js?c5922bb0b011ecf8d1db8ab79e46bd46dbdf2833:24376:15)
""".trimIndent()
