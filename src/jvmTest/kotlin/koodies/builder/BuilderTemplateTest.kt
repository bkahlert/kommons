package koodies.builder

import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.test.test
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class BuilderTemplateTest {

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

    data class CustomObject(
        val byBuilderShorthand: Pair<List<String>, List<String>?>, // TODO default?
        val byBuilder: Pair<List<String>, List<String>>,
        val byNullableBuilder: Pair<List<String>, List<String>?>,
        val byBuilderSimple: Pair<String, String>,
        val byNullableBuilderSimple: Pair<String, String?>,

        val byCapture5: Pair<String, String>,
        val byNullableCapture5: Pair<String, String?>,
        val byCapture4: Pair<String, String>,
        val byNullableCapture4: Pair<String, String?>,
        val byCapture3: Pair<String, String>,
        val byNullableCapture3: Pair<String, String?>,
        val byCapture2: Pair<String, String>,
        val byNullableCapture2: Pair<String, String?>,
        val byCapture1: Pair<String, String>,
        val byNullableCapture1: Pair<String, String?>,
        val byCapture0: Pair<String, String>,
        val byNullableCapture0: Pair<String, String?>,

        val byCapture: Pair<String, String>,
        val byNullableCapture: Pair<String, String?>,

        val bySetter: Pair<String, String>,
        val byNullableSetter: Pair<String, String?>,
    )

    private class CustomBuilder : BuilderTemplate<CustomBuilder.CustomContext, CustomObject>() {
        inner class CustomContext(override val captures: CapturesMap) : CapturingContext() {

            val byBuilderShorthand by ListBuilder<String>()
            val byBuilder by builder(emptyList(), ListBuilder<String>())
            val byNullableBuilder by builder(ListBuilder<String>())
            val byBuilderSimple by builder("initial")
            val byNullableBuilderSimple by builder<String>()

            val byCapture5 by capture("initial-5", BuilderTemplateTest()::f5)
            val byNullableCapture5 by capture(BuilderTemplateTest()::f5)
            val byCapture4 by capture("initial-4", BuilderTemplateTest()::f4)
            val byNullableCapture4 by capture(BuilderTemplateTest()::f4)
            val byCapture3 by capture("initial-3", BuilderTemplateTest()::f3)
            val byNullableCapture3 by capture(BuilderTemplateTest()::f3)
            val byCapture2 by capture("initial-2", BuilderTemplateTest()::f2)
            val byNullableCapture2 by capture(BuilderTemplateTest()::f2)
            val byCapture1 by capture("initial-1", BuilderTemplateTest()::f1)
            val byNullableCapture1 by capture(BuilderTemplateTest()::f1)
            val byCapture0 by capture("initial-0", BuilderTemplateTest()::f0)
            val byNullableCapture0 by capture(BuilderTemplateTest()::f0)

            val byCapture by capture("initial")
            val byNullableCapture by capture<String>()

            var bySetter by setter("initial")
            var byNullableSetter by setter<String>()
        }

        override fun BuildContext.build(): CustomObject = withContext(::CustomContext) {
            CustomObject(
                ::byBuilderShorthand.evalOrDefault(listOf("default")) to ::byBuilderShorthand.evalOrNull(),
                ::byBuilder.evalOrDefault(listOf("default")) to ::byBuilder.eval(),
                ::byNullableBuilder.evalOrDefault(listOf("default")) to ::byNullableBuilder.evalOrNull(),
                ::byBuilderSimple.evalOrDefault("default") to ::byBuilderSimple.eval(),
                ::byNullableBuilderSimple.evalOrDefault("default") to ::byNullableBuilderSimple.evalOrNull(),

                ::byCapture5.evalOrDefault("default-5") to ::byCapture5.eval(),
                ::byNullableCapture5.evalOrDefault("default-5") to ::byNullableCapture5.evalOrNull(),
                ::byCapture4.evalOrDefault("default-4") to ::byCapture4.eval(),
                ::byNullableCapture4.evalOrDefault("default-4") to ::byNullableCapture4.evalOrNull(),
                ::byCapture3.evalOrDefault("default-3") to ::byCapture3.eval(),
                ::byNullableCapture3.evalOrDefault("default-3") to ::byNullableCapture3.evalOrNull(),
                ::byCapture2.evalOrDefault("default-2") to ::byCapture2.eval(),
                ::byNullableCapture2.evalOrDefault("default-2") to ::byNullableCapture2.evalOrNull(),
                ::byCapture1.evalOrDefault("default-1") to ::byCapture1.eval(),
                ::byNullableCapture1.evalOrDefault("default-1") to ::byNullableCapture1.evalOrNull(),
                ::byCapture0.evalOrDefault("default-0") to ::byCapture0.eval(),
                ::byNullableCapture0.evalOrDefault("default-0") to ::byNullableCapture0.evalOrNull(),

                ::byCapture.evalOrDefault("default") to ::byCapture.eval(),
                ::byNullableCapture.evalOrDefault("default") to ::byNullableCapture.evalOrNull<String>(),

                ::bySetter.evalOrDefault("default") to ::bySetter.eval(),
                ::byNullableSetter.evalOrDefault("default") to ::byNullableSetter.evalOrNull<String>(),
            )
        }
    }

    @Test
    fun `should build with initial value`() {

        data class CustomObject2(
            val value: String,
            val nullableValue: String?,
        )

        class CustomBuilder2 : BuilderTemplate<CustomBuilder2.CustomContext2, CustomObject2>() {
            inner class CustomContext2(override val captures: CapturesMap) : CapturingContext() {
                val initializedCapturingCallable: (String) -> Unit by capture("initial")
                val nullableInitializedCapturingCallable: (String?) -> Unit by capture(null)
            }

            override fun BuildContext.build(): CustomObject2 = withContext(::CustomContext2) {
                CustomObject2(
                    ::initializedCapturingCallable.eval(),
                    ::nullableInitializedCapturingCallable.evalOrNull(),
                )
            }
        }

        val customBuilder = CustomBuilder2()
        val customObject = customBuilder { }
        expectThat(customObject).isEqualTo(CustomObject2("initial", null))
    }

    @TestFactory
    fun should() = test(CustomBuilder()) {

        test("build") {
            expect {
                CustomBuilder().build {
                    byBuilderShorthand { +"value" }
                    byBuilder { +"value" }
                    byNullableBuilder { +"value" }
                    byBuilderSimple { "value" }
                    byNullableBuilderSimple { "value" }

                    byCapture5(1, 2, 3, 4, 5)
                    byNullableCapture5(1, 2, 3, 4, 5)
                    byCapture4(1, 2, 3, 4)
                    byNullableCapture4(1, 2, 3, 4)
                    byCapture3(1, 2, 3)
                    byNullableCapture3(1, 2, 3)
                    byCapture2(1, 2)
                    byNullableCapture2(1, 2)
                    byCapture1(1)
                    byNullableCapture1(1)
                    byCapture0()
                    byNullableCapture0()

                    byCapture("value")
                    byNullableCapture("value")

                    bySetter = "value"
                    byNullableSetter = "value"
                }
            }.that {
                isEqualTo(CustomObject(
                    byBuilderShorthand = listOf("value") to listOf("value"),
                    byBuilder = listOf("value") to listOf("value"),
                    byNullableBuilder = listOf("value") to listOf("value"),
                    byBuilderSimple = "value" to "value",
                    byNullableBuilderSimple = "value" to "value",

                    byCapture5 = "value-13" to "value-13",
                    byNullableCapture5 = "value-13" to "value-13",
                    byCapture4 = "value-8" to "value-8",
                    byNullableCapture4 = "value-8" to "value-8",
                    byCapture3 = "value-5" to "value-5",
                    byNullableCapture3 = "value-5" to "value-5",
                    byCapture2 = "value-3" to "value-3",
                    byNullableCapture2 = "value-3" to "value-3",
                    byCapture1 = "value-1" to "value-1",
                    byNullableCapture1 = "value-1" to "value-1",
                    byCapture0 = "value-0" to "value-0",
                    byNullableCapture0 = "value-0" to "value-0",

                    byCapture = "value" to "value",
                    byNullableCapture = "value" to "value",

                    bySetter = "value" to "value",
                    byNullableSetter = "value" to "value",
                ))
            }
        }

        test("build without invocations") {
            expect { CustomBuilder().build { } }.that {
                isEqualTo(CustomObject(
                    byBuilderShorthand = listOf("default") to null,
                    byBuilder = listOf("default") to listOf("initial"),
                    byNullableBuilder = listOf("default") to null,
                    byBuilderSimple = "default" to "initial",
                    byNullableBuilderSimple = "default" to null,

                    byCapture5 = "default-5" to "initial-5",
                    byNullableCapture5 = "default-5" to null,
                    byCapture4 = "default-4" to "initial-4",
                    byNullableCapture4 = "default-4" to null,
                    byCapture3 = "default-3" to "initial-3",
                    byNullableCapture3 = "default-3" to null,
                    byCapture2 = "default-2" to "initial-2",
                    byNullableCapture2 = "default-2" to null,
                    byCapture1 = "default-1" to "initial-1",
                    byNullableCapture1 = "default-1" to null,
                    byCapture0 = "default-0" to "initial-0",
                    byNullableCapture0 = "default-0" to null,

                    byCapture = "default" to "initial",
                    byNullableCapture = "default" to null,

                    bySetter = "default" to "initial",
                    byNullableSetter = "default" to null,
                ))
            }
        }

        test("not share state between builds") {
            expect {
                CustomBuilder().run {
                    build {
                        byBuilderShorthand { +"value" }
                        byBuilder { +"value" }
                        byNullableBuilder { +"value" }
                        byBuilderSimple { "value" }
                        byNullableBuilderSimple { "value" }

                        byCapture5(1, 2, 3, 4, 5)
                        byNullableCapture5(1, 2, 3, 4, 5)
                        byCapture4(1, 2, 3, 4)
                        byNullableCapture4(1, 2, 3, 4)
                        byCapture3(1, 2, 3)
                        byNullableCapture3(1, 2, 3)
                        byCapture2(1, 2)
                        byNullableCapture2(1, 2)
                        byCapture1(1)
                        byNullableCapture1(1)
                        byCapture0()
                        byNullableCapture0()

                        byCapture("value")
                        byNullableCapture("value")

                        bySetter = "value"
                        byNullableSetter = "value"
                    }

                    build { }
                }
            }.that {
                isEqualTo(CustomObject(
                    byBuilderShorthand = listOf("default") to null,
                    byBuilder = listOf("default") to listOf("initial"),
                    byNullableBuilder = listOf("default") to null,
                    byBuilderSimple = "default" to "initial",
                    byNullableBuilderSimple = "default" to null,

                    byCapture5 = "default-5" to "initial-5",
                    byNullableCapture5 = "default-5" to null,
                    byCapture4 = "default-4" to "initial-4",
                    byNullableCapture4 = "default-4" to null,
                    byCapture3 = "default-3" to "initial-3",
                    byNullableCapture3 = "default-3" to null,
                    byCapture2 = "default-2" to "initial-2",
                    byNullableCapture2 = "default-2" to null,
                    byCapture1 = "default-1" to "initial-1",
                    byNullableCapture1 = "default-1" to null,
                    byCapture0 = "default-0" to "initial-0",
                    byNullableCapture0 = "default-0" to null,

                    byCapture = "default" to "initial",
                    byNullableCapture = "default" to null,

                    bySetter = "default" to "initial",
                    byNullableSetter = "default" to null,
                ))
            }
        }
    }
}
