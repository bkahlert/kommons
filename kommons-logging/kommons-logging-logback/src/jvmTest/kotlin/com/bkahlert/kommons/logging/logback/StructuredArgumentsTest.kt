package com.bkahlert.kommons.logging.logback

import com.bkahlert.kommons.logging.JSON_PRESET_VALUE
import com.bkahlert.kommons.logging.LoggingSystemProperties
import com.bkahlert.kommons.test.junit.SystemProperty
import com.bkahlert.kommons.test.logging.lastLog
import com.bkahlert.kommons.test.spring.Captured
import com.bkahlert.kommons.test.testAll
import com.fasterxml.jackson.core.JsonGenerator
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.inspectors.forAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import net.logstash.logback.argument.StructuredArgument
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.test.system.CapturedOutput

@Isolated
@SystemProperty(LoggingSystemProperties.CONSOLE_LOG_PRESET, JSON_PRESET_VALUE)
class StructuredArgumentsTest {

    @BeforeAll
    fun setUp() {
        Logback.reset()
    }

    private val logger: Logger by lazy { LoggerFactory.getLogger("TestLogger") }

    private fun CapturedOutput.forAll(
        vararg invocations: Logger.() -> Unit,
        assertion: (String) -> Unit,
    ) {
        invocations.map {
            logger.it()
            lastLog.log
        }.forAll { log ->
            log.should(assertion)
        }
    }

    @Test fun @receiver:Captured CapturedOutput.key_value() = testAll {
        forAll(
            { info("{}", StructuredArguments.keyValue("key", ClassWithCustomToString()) { it.x() }) },
            { info("{}", StructuredArguments.kv("key", ClassWithCustomToString()) { it.x() }) },
        ) {
            it.shouldContainJsonKeyValue("message", "key=x-value")
            it.shouldContainJsonKeyValue("key.foo", "x-null")
            it.shouldContainJsonKeyValue("key.bar", "baz")
        }

        forAll(
            { info("{}", StructuredArguments.keyValue("key", ClassWithCustomToString())) },
            { info("{}", StructuredArguments.kv("key", ClassWithCustomToString())) },
        ) {
            it.shouldContainJsonKeyValue("message", "key=value")
            it.shouldContainJsonKeyNullValue("key.foo")
            it.shouldContainJsonKeyValue("key.bar", "baz")
        }

        forAll(
            { info("{}", StructuredArguments.keyValue(ClassWithCustomToString())) },
            { info("{}", StructuredArguments.kv(ClassWithCustomToString())) },
        ) {
            it.shouldContainJsonKeyValue("message", "class-with-custom-to-string=value")
            it.shouldContainJsonKeyNullValue("class-with-custom-to-string.foo")
            it.shouldContainJsonKeyValue("class-with-custom-to-string.bar", "baz")
        }
    }

    @Test fun @receiver:Captured CapturedOutput.value() = testAll {
        forAll(
            { info("{}", StructuredArguments.value("key", ClassWithCustomToString()) { it.x() }) },
            { info("{}", StructuredArguments.v("key", ClassWithCustomToString()) { it.x() }) },
        ) {
            it.shouldContainJsonKeyValue("message", "x-value")
            it.shouldContainJsonKeyValue("key.foo", "x-null")
            it.shouldContainJsonKeyValue("key.bar", "baz")
        }

        forAll(
            { info("{}", StructuredArguments.value(ClassWithCustomToString())) },
            { info("{}", StructuredArguments.v(ClassWithCustomToString())) },
        ) {
            it.shouldContainJsonKeyValue("message", "value")
            it.shouldContainJsonKeyNullValue("class-with-custom-to-string.foo")
            it.shouldContainJsonKeyValue("class-with-custom-to-string.bar", "baz")
        }
    }

    @Test fun @receiver:Captured CapturedOutput.entries() = testAll {
        forAll(
            { info("{}", StructuredArguments.entries(mapOf("foo" to null, "bar" to "baz")) { "x-${it.value}" }) },
            { info("{}", StructuredArguments.entries(*mapOf("foo" to null, "bar" to "baz").entries.toTypedArray()) { "x-${it.value}" }) },
            { info("{}", StructuredArguments.entries(*mapOf("foo" to null, "bar" to "baz").entries.toTypedArray()) { "x-${it.value}" }) },
            { info("{}", StructuredArguments.entries("foo" to null, "bar" to "baz") { "x-${it.value}" }) },
            { info("{}", StructuredArguments.e(mapOf("foo" to null, "bar" to "baz")) { "x-${it.value}" }) },
            { info("{}", StructuredArguments.e(*mapOf("foo" to null, "bar" to "baz").entries.toTypedArray()) { "x-${it.value}" }) },
            { info("{}", StructuredArguments.e("foo" to null, "bar" to "baz") { "x-${it.value}" }) },
        ) {
            it.shouldContainJsonKeyValue("message", """{foo=x-null, bar=x-baz}""")
            it.shouldContainJsonKeyValue("foo", "x-null")
            it.shouldContainJsonKeyValue("bar", "x-baz")
        }

        forAll(
            { info("{}", StructuredArguments.entries(mapOf("foo" to null, "bar" to "baz"))) },
            { info("{}", StructuredArguments.entries(*mapOf("foo" to null, "bar" to "baz").entries.toTypedArray())) },
            { info("{}", StructuredArguments.entries("foo" to null, "bar" to "baz")) },
            { info("{}", StructuredArguments.e(mapOf("foo" to null, "bar" to "baz"))) },
            { info("{}", StructuredArguments.e(*mapOf("foo" to null, "bar" to "baz").entries.toTypedArray())) },
            { info("{}", StructuredArguments.e("foo" to null, "bar" to "baz")) },
        ) {
            it.shouldContainJsonKeyValue("message", """{foo=null, bar=baz}""")
            it.shouldContainJsonKeyNullValue("foo")
            it.shouldContainJsonKeyValue("bar", "baz")
        }
    }

    @Test fun @receiver:Captured CapturedOutput.fields() = testAll {
        forAll(
            { info("{}", StructuredArguments.fields(ClassWithCustomToString())) },
            { info("{}", StructuredArguments.f(ClassWithCustomToString())) },
        ) {
            it.shouldContainJsonKeyValue("message", """value""")
            it.shouldContainJsonKeyNullValue("foo")
            it.shouldContainJsonKeyValue("bar", "baz")
        }
    }


    @Test fun @receiver:Captured CapturedOutput.array() = testAll {
        forAll(
            { info("{}", StructuredArguments.array(ClassWithCustomToString(), ClassWithCustomToString("alt"), key = "key") { "x-${it.foo}" }) },
            { info("{}", StructuredArguments.a(ClassWithCustomToString(), ClassWithCustomToString("alt"), key = "key") { "x-${it.foo}" }) },
        ) {
            it.shouldContainJsonKeyValue("message", """key=[x-null, x-alt]""")
            it.shouldContainJsonKeyValue("key", listOf("x-null", "x-alt"))
        }

        forAll(
            { info("{}", StructuredArguments.array(ClassWithCustomToString(), ClassWithCustomToString("alt"))) },
            { info("{}", StructuredArguments.a(ClassWithCustomToString(), ClassWithCustomToString("alt"))) },
        ) {
            it.shouldContainJsonKeyValue("message", """class-with-custom-to-strings=[value, value]""")
            it.shouldContainJsonKeyValue("class-with-custom-to-strings", listOf(mapOf("foo" to null, "bar" to "baz"), mapOf("foo" to "alt", "bar" to "baz")))
        }
    }

    @Test fun @receiver:Captured CapturedOutput.objects() = testAll {
        forAll(
            { info("{}", StructuredArguments.objects("key", listOf(ClassWithCustomToString(), ClassWithCustomToString("alt"))) { "x-${it.foo}" }) },
            { info("{}", StructuredArguments.o("key", listOf(ClassWithCustomToString(), ClassWithCustomToString("alt"))) { "x-${it.foo}" }) },
        ) {
            it.shouldContainJsonKeyValue("message", """key=[x-null, x-alt]""")
            it.shouldContainJsonKeyValue("key", listOf("x-null", "x-alt"))
        }

        forAll(
            { info("{}", StructuredArguments.objects(listOf(ClassWithCustomToString(), ClassWithCustomToString("alt")))) },
            { info("{}", StructuredArguments.o(listOf(ClassWithCustomToString(), ClassWithCustomToString("alt")))) },
        ) {
            it.shouldContainJsonKeyValue("message", """class-with-custom-to-strings=[value, value]""")
            it.shouldContainJsonKeyValue("class-with-custom-to-strings", listOf(mapOf("foo" to null, "bar" to "baz"), mapOf("foo" to "alt", "bar" to "baz")))
        }
    }

    @Test fun @receiver:Captured CapturedOutput.raw() = testAll {
        forAll(
            { info("{}", StructuredArguments.raw("key", """{ "raw": "value" }""")) },
            { info("{}", StructuredArguments.r("key", """{ "raw": "value" }""")) },
        ) {
            it.shouldContainJsonKeyValue("message", """key={ "raw": "value" }""")
            it.shouldContainJsonKeyValue("key.raw", "value")
        }

        forAll(
            { info("{}", StructuredArguments.raw("key", "null")) },
            { info("{}", StructuredArguments.r("key", "null")) },
        ) {
            it.shouldContainJsonKeyValue("message", """key=null""")
            it.shouldContainJsonKeyNullValue("key")
        }
    }

    @Test fun @receiver:Captured CapturedOutput.defer() = testAll {
        var instantiations = 0
        val deferredStructuredArgument = StructuredArguments.defer {
            instantiations++
            var i = 0
            object : StructuredArgument {
                override fun writeTo(generator: JsonGenerator) {
                    generator.writeNumberField("key", i++)
                }

                override fun toString(): String {
                    return i.toString()
                }
            }
        }
        instantiations shouldBe 0

        logger.info("{}", deferredStructuredArgument)
        instantiations shouldBe 1
        lastLog.log should {
            it.shouldContainJsonKeyValue("message", "0")
            it.shouldContainJsonKeyValue("key", 0)
        }
        logger.info("{}", deferredStructuredArgument)
        instantiations shouldBe 1
        lastLog.log should {
            it.shouldContainJsonKeyValue("message", "1")
            it.shouldContainJsonKeyValue("key", 1)
        }
    }

    @Test fun @receiver:Captured CapturedOutput.to_string() = testAll {
        forAll(
            { info("{}", StructuredArguments.toString(ClassWithCustomToString())) },
        ) {
            it.shouldContainJsonKeyValue("message", """value""")
        }

        forAll(
            { info("{}", StructuredArguments.toString(arrayOf(ClassWithCustomToString(), ClassWithCustomToString("alt")))) },
        ) {
            it.shouldContainJsonKeyValue("message", """[value, value]""")
        }
    }
}


fun String?.shouldContainJsonKeyNullValue(path: String) {
    this.shouldContainJsonKeyValue<Any?>(path, null)
}

internal class ClassWithCustomToString(
    val foo: Any? = null,
    private val toString: String = "value",
) {
    val bar: String = "baz"

    fun x() = ClassWithCustomToString(
        foo = "x-$foo",
        toString = "x-$toString",
    )

    override fun toString(): String = toString
}
