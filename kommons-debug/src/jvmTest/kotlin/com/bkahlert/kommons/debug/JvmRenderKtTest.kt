package com.bkahlert.kommons.debug

import com.bkahlert.kommons.debug.CustomToString.Ignore
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.collections.Map.Entry

class JvmRenderTest {

    @Test fun render_inaccessible() = testAll {

        val evilMap: Map<Any, Any?> = object : AbstractMap<Any, Any?>() {
            override fun equals(other: Any?): Boolean = false
            override fun hashCode(): Int = 1
            override val entries: Set<Entry<Any, Any?>>
                get() = error("no entries")

            override fun toString(): String {
                return "string representation"
            }
        }
        val veryEvilMap: Map<Any, Any?> = object : AbstractMap<Any, Any?>() {
            override fun equals(other: Any?): Boolean = false
            override fun hashCode(): Int = 1
            override val entries: Set<Entry<Any, Any?>>
                get() = error("no entries")

            override fun toString(): String {
                return StackTrace.get().findOrNull { it.methodName == "toCustomStringOrNull" }?.toString()
                    ?: error("no string")
            }
        }

        evilMap.render { customToString = Ignore } shouldBe "<string representation>"
        veryEvilMap.render { customToString = Ignore } shouldBe "<unsupported:java.lang.IllegalStateException: no string>"
    }
}
