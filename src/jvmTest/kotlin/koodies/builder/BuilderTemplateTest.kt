package koodies.builder

import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

@Execution(SAME_THREAD)
class BuilderTemplateTest {

    class Owner {
        fun f5(p1: Int, p2: Int, p3: Int, p4: Int, p5: Int): String =
            "value-${p1 + p2 + p3 + p4 + p5}"

        fun f4(p1: Int, p2: Int, p3: Int, p4: Int): String =
            "value-${p1 + p2 + p3 + p4}"

        fun f3(p1: Int, p2: Int, p3: Int): String =
            "value-${p1 + p2 + p3}"

        fun f2(p1: Int, p2: Int): String =
            "value-${p1 + p2}"

        fun f1(p1: Int): String =
            "value-${p1}"

        fun f0(): String =
            "value-0"
    }

    data class CustomObject(
        val byBuilder: Pair<List<String>, List<String>>,
        val byNullableBuilder: Pair<List<String>, List<String>?>,

        val byCapture5: Pair<String, String>,
        val byCapture4: Pair<String, String>,
        val byCapture3: Pair<String, String>,
        val byCapture2: Pair<String, String>,
        val byCapture1: Pair<String, String>,
        val byCapture0: Pair<String, String>,
        val byNullableCapture5: Pair<String, String?>,
        val byNullableCapture4: Pair<String, String?>,
        val byNullableCapture3: Pair<String, String?>,
        val byNullableCapture2: Pair<String, String?>,
        val byNullableCapture1: Pair<String, String?>,
        val byNullableCapture0: Pair<String, String?>,

        val byCapture: Pair<String, String>,
        val byNullableCapture: Pair<String, String?>,

        val bySetter: Pair<String, String>,
        val byNullableSetter: Pair<String, String?>,
    )

    private class CustomBuilder : BuilderTemplate<CustomBuilder.CustomContext, CustomObject>() {
        inner class CustomContext(override val captures: CapturesMap) : CapturingContext() {

            val byBuilder by ListBuilder<String>() default emptyList()
            val byNullableBuilder by ListBuilder<String>()

            val byCapture5 by Owner()::f5 default "initial-5"
            val byCapture4 by Owner()::f4 default "initial-4"
            val byCapture3 by Owner()::f3 default "initial-3"
            val byCapture2 by Owner()::f2 default "initial-2"
            val byCapture1 by Owner()::f1 default "initial-1"
            val byCapture0 by Owner()::f0 default "initial-0"
            val byNullableCapture5 by Owner()::f5
            val byNullableCapture4 by Owner()::f4
            val byNullableCapture3 by Owner()::f3
            val byNullableCapture2 by Owner()::f2
            val byNullableCapture1 by Owner()::f1
            val byNullableCapture0 by Owner()::f0

            val byCapture by function<String>() default "initial"
            val byNullableCapture by function<String>()

            var bySetter by setter<String>() default "initial"
            var byNullableSetter by setter<String>()
        }

        override fun BuildContext.build() = ::CustomContext {
            CustomObject(
                ::byBuilder.evalOrDefault(listOf("default")) to ::byBuilder.eval(),
                ::byNullableBuilder.evalOrDefault(listOf("default")) to ::byNullableBuilder.evalOrNull(),

                ::byCapture5.evalOrDefault("default-5") to ::byCapture5.eval(),
                ::byCapture4.evalOrDefault("default-4") to ::byCapture4.eval(),
                ::byCapture3.evalOrDefault("default-3") to ::byCapture3.eval(),
                ::byCapture2.evalOrDefault("default-2") to ::byCapture2.eval(),
                ::byCapture1.evalOrDefault("default-1") to ::byCapture1.eval(),
                ::byCapture0.evalOrDefault("default-0") to ::byCapture0.eval(),
                ::byNullableCapture5.evalOrDefault("default-5") to ::byNullableCapture5.evalOrNull(),
                ::byNullableCapture4.evalOrDefault("default-4") to ::byNullableCapture4.evalOrNull(),
                ::byNullableCapture3.evalOrDefault("default-3") to ::byNullableCapture3.evalOrNull(),
                ::byNullableCapture2.evalOrDefault("default-2") to ::byNullableCapture2.evalOrNull(),
                ::byNullableCapture1.evalOrDefault("default-1") to ::byNullableCapture1.evalOrNull(),
                ::byNullableCapture0.evalOrDefault("default-0") to ::byNullableCapture0.evalOrNull(),

                ::byCapture.evalOrDefault("default") to ::byCapture.eval(),
                ::byNullableCapture.evalOrDefault("default") to ::byNullableCapture.evalOrNull<String>(),

                ::bySetter.evalOrDefault("default") to ::bySetter.eval(),
                ::byNullableSetter.evalOrDefault("default") to ::byNullableSetter.evalOrNull<String>(),
            )
        }
    }

    @Test
    fun `should build`() {
        val built = CustomBuilder().build {
            byBuilder { +"value" }
            byNullableBuilder { +"value" }

            byCapture5(1, 2, 3, 4, 5)
            byCapture4(1, 2, 3, 4)
            byCapture3(1, 2, 3)
            byCapture2(1, 2)
            byCapture1(1)
            byCapture0()
            byNullableCapture5(1, 2, 3, 4, 5)
            byNullableCapture4(1, 2, 3, 4)
            byNullableCapture3(1, 2, 3)
            byNullableCapture2(1, 2)
            byNullableCapture1(1)
            byNullableCapture0()

            byCapture("value")
            byNullableCapture("value")

            bySetter = "value"
            byNullableSetter = "value"
        }

        expectThat(built).isEqualTo(CustomObject(
            byBuilder = listOf("value") to listOf("value"),
            byNullableBuilder = listOf("value") to listOf("value"),

            byCapture5 = "value-15" to "value-15",
            byCapture4 = "value-10" to "value-10",
            byCapture3 = "value-6" to "value-6",
            byCapture2 = "value-3" to "value-3",
            byCapture1 = "value-1" to "value-1",
            byCapture0 = "value-0" to "value-0",
            byNullableCapture5 = "value-15" to "value-15",
            byNullableCapture4 = "value-10" to "value-10",
            byNullableCapture3 = "value-6" to "value-6",
            byNullableCapture2 = "value-3" to "value-3",
            byNullableCapture1 = "value-1" to "value-1",
            byNullableCapture0 = "value-0" to "value-0",

            byCapture = "value" to "value",
            byNullableCapture = "value" to "value",

            bySetter = "value" to "value",
            byNullableSetter = "value" to "value",
        ))
    }

    @Test
    fun `should build without invocations`() {
        val built = CustomBuilder().build { }

        expectThat(built).isEqualTo(CustomObject(
            byBuilder = emptyList<String>() to emptyList(),
            byNullableBuilder = listOf("default") to null,

            byCapture5 = "initial-5" to "initial-5",
            byCapture4 = "initial-4" to "initial-4",
            byCapture3 = "initial-3" to "initial-3",
            byCapture2 = "initial-2" to "initial-2",
            byCapture1 = "initial-1" to "initial-1",
            byCapture0 = "initial-0" to "initial-0",
            byNullableCapture5 = "default-5" to null,
            byNullableCapture4 = "default-4" to null,
            byNullableCapture3 = "default-3" to null,
            byNullableCapture2 = "default-2" to null,
            byNullableCapture1 = "default-1" to null,
            byNullableCapture0 = "default-0" to null,

            byCapture = "initial" to "initial",
            byNullableCapture = "default" to null,

            bySetter = "initial" to "initial",
            byNullableSetter = "default" to null,
        ))
    }

    @Test
    fun `should not share state between builds`() {
        val built = CustomBuilder().run {
            build {
                byBuilder { +"value" }
                byNullableBuilder { +"value" }

                byCapture5(1, 2, 3, 4, 5)
                byCapture4(1, 2, 3, 4)
                byCapture3(1, 2, 3)
                byCapture2(1, 2)
                byCapture1(1)
                byCapture0()
                byNullableCapture5(1, 2, 3, 4, 5)
                byNullableCapture4(1, 2, 3, 4)
                byNullableCapture3(1, 2, 3)
                byNullableCapture2(1, 2)
                byNullableCapture1(1)
                byNullableCapture0()

                byCapture("value")
                byNullableCapture("value")

                bySetter = "value"
                byNullableSetter = "value"
            }

            build { }
        }

        expectThat(built).isEqualTo(CustomObject(
            byBuilder = emptyList<String>() to emptyList(),
            byNullableBuilder = listOf("default") to null,

            byCapture5 = "initial-5" to "initial-5",
            byCapture4 = "initial-4" to "initial-4",
            byCapture3 = "initial-3" to "initial-3",
            byCapture2 = "initial-2" to "initial-2",
            byCapture1 = "initial-1" to "initial-1",
            byCapture0 = "initial-0" to "initial-0",
            byNullableCapture5 = "default-5" to null,
            byNullableCapture4 = "default-4" to null,
            byNullableCapture3 = "default-3" to null,
            byNullableCapture2 = "default-2" to null,
            byNullableCapture1 = "default-1" to null,
            byNullableCapture0 = "default-0" to null,

            byCapture = "initial" to "initial",
            byNullableCapture = "default" to null,

            bySetter = "initial" to "initial",
            byNullableSetter = "default" to null,
        ))
    }

    @Nested
    inner class ErrorsHandling {

        private inner class ThrowingBuilder(val exception: Throwable) : BuilderTemplate<Any, Any>() {
            override fun BuildContext.build(): Any = throw exception
        }

        @Test
        fun `should throw IllegalStateException on error`() {
            expectCatching { ThrowingBuilder(RuntimeException("test")).build { } }.isFailure().isA<IllegalStateException>()
        }

        @Test
        fun `should throw IllegalArgumentException on illegal input`() {
            expectCatching { ThrowingBuilder(IllegalArgumentException("input")).build { } }.isFailure().isA<IllegalArgumentException>()
        }
    }
}
