package koodies.builder.context

import koodies.CallableProperty
import koodies.Deferred
import koodies.Exceptions
import koodies.builder.Builder
import koodies.builder.context.CapturesMapTest.BuilderClass.Context
import koodies.test.test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNull
import strikt.assertions.map

@Execution(SAME_THREAD)
class CapturesMapTest {

    private object BuilderClass : Builder<Context.() -> Unit, String> {
        fun interface Context {
            fun count()
        }

        override fun invoke(init: Context.() -> Unit): String {
            var counter = 0
            Context { counter++ }.apply(init)
            return counter.toString()
        }
    }


    @Nested
    inner class NoInvocations {

        private val capturesMap = CapturesMap()
        private val delegatedFunction by CallableProperty { _, property ->
            { init: Context.() -> Unit ->
                capturesMap.add(property, Deferred { BuilderClass(init) })
            }
        }

        @TestFactory
        fun `using member functions`() = test(capturesMap) {
            test("getAll") {
                expect { getAll() }.that { evaluateTo() }
                expect { getAll<String>(::delegatedFunction) }.that { evaluateTo() }
            }

            group("get") {
                test { expect { get<String>(::delegatedFunction) }.that { evaluatesTo(null) } }
                test { expect { get<String>(::delegatedFunction) { it as String } }.that { evaluatesTo(null) } }
                test { expect { get<String>("delegatedFunction") }.that { evaluatesTo(null) } }
                test { expect { get<String>("delegatedFunction") { it as String } }.that { evaluatesTo(null) } }
            }

            group("wrong type") {
                test { expect { get<Double>(::delegatedFunction) }.that { evaluatesTo(null) } }
                test { expect { get<Double>(::delegatedFunction) { it as Double } }.that { evaluatesTo(null) } }
                test { expect { get<Double>("delegatedFunction") }.that { evaluatesTo(null) } }
                test { expect { get<Double>("delegatedFunction") { it as Double } }.that { evaluatesTo(null) } }
            }

            group("getOrDefault") {
                test { expect { getOrDefault(::delegatedFunction, "default") }.that { evaluatesTo("default") } }
                test { expect { getOrDefault("delegatedFunction", "default") }.that { evaluatesTo("default") } }
            }

            group("getOrThrow") {
                group("default exception") {
                    test { expectThrowing { getOrThrow<String>(::delegatedFunction) }.that { isFailure().isA<NoSuchElementException>() } }
                    test { expectThrowing { getOrThrow<String>("delegatedFunction") }.that { isFailure().isA<NoSuchElementException>() } }
                }
                group("custom exception") {
                    test { expectThrowing { getOrThrow<String>(::delegatedFunction) { Exceptions.AE("test") } }.that { isFailure().isA<AssertionError>() } }
                    test { expectThrowing { getOrThrow<String>("delegatedFunction") { Exceptions.AE("test") } }.that { isFailure().isA<AssertionError>() } }
                }
            }
        }

        @TestFactory
        fun `using extension functions`() = test(capturesMap) {
            test { expect { ::delegatedFunction.evalAll<String>() }.isEmpty() }
            test { expect { ::delegatedFunction.evalOrNull<String>() }.isNull() }
            test { expect { ::delegatedFunction.evalOrDefault("default") }.isEqualTo("default") }
            test { expect { ::delegatedFunction.evalOrDefault { "default" } }.isEqualTo("default") }
            test { expectThrowing { ::delegatedFunction.eval<String>() }.isFailure().isA<NoSuchElementException>() }
        }
    }

    @Nested
    inner class MultipleInvocations {

        private val capturesMap = CapturesMap()
        private val delegatedFunction by CallableProperty { _, property ->
            { init: Context.() -> Unit ->
                capturesMap.add(property, Deferred { BuilderClass(init) })
            }
        }

        init {
            delegatedFunction { count();count() }
            delegatedFunction { count() }
            delegatedFunction { count();count();count() }
        }

        @TestFactory
        fun `using member functions`() = test(capturesMap) {
            test("getAll") {
                expect { getAll() }.that { evaluateTo("2", "1", "3") }
                expect { getAll<String>(::delegatedFunction) }.that { evaluateTo("2", "1", "3") }
            }

            group("get") {
                test { expect { get<String>(::delegatedFunction) }.that { evaluatesTo("3") } }
                test { expect { get<String>(::delegatedFunction) { it as String } }.that { evaluatesTo("3") } }
                test { expect { get<String>("delegatedFunction") }.that { evaluatesTo("3") } }
                test { expect { get<String>("delegatedFunction") { it as String } }.that { evaluatesTo("3") } }
            }

            group("wrong type") {
                test { expectThrowing { get<Double>(::delegatedFunction).evaluate() }.that { isFailure().isA<ClassCastException>() } }
                test { expectThrowing { get(::delegatedFunction) { it as Double }.evaluate() }.that { isFailure().isA<ClassCastException>() } }
                test { expectThrowing { get<Double>("delegatedFunction").evaluate() }.that { isFailure().isA<ClassCastException>() } }
                test { expectThrowing { get("delegatedFunction") { it as Double }.evaluate() }.that { isFailure().isA<ClassCastException>() } }
            }

            group("getOrDefault") {
                test { expect { getOrDefault(::delegatedFunction, "default") }.that { evaluatesTo("3") } }
                test { expect { getOrDefault("delegatedFunction", "default") }.that { evaluatesTo("3") } }
            }

            group("getOrThrow") {
                group("default exception") {
                    test { expect { getOrThrow<String>(::delegatedFunction) }.that { evaluatesTo("3") } }
                    test { expect { getOrThrow<String>("delegatedFunction") }.that { evaluatesTo("3") } }
                }
                group("custom exception") {
                    test { expect { getOrThrow<String>(::delegatedFunction) { Exceptions.AE("test") } }.that { evaluatesTo("3") } }
                    test { expect { getOrThrow<String>("delegatedFunction") { Exceptions.AE("test") } }.that { evaluatesTo("3") } }
                }
            }
        }

        @TestFactory
        fun `using extension functions`() = test(capturesMap) {
            test { expect { ::delegatedFunction.evalAll<String>() }.containsExactly("2", "1", "3") }
            test { expect { ::delegatedFunction.evalOrNull<String>() }.isEqualTo("3") }
            test { expect { ::delegatedFunction.evalOrDefault("default") }.isEqualTo("3") }
            test { expect { ::delegatedFunction.evalOrDefault { "default" } }.isEqualTo("3") }
            test { expect { ::delegatedFunction.eval<String>() }.isEqualTo("3") }
        }
    }
}

inline fun <reified T> Assertion.Builder<Deferred<out T>>.evaluatesTo(value: T) =
    get("evaluating") { evaluate() }.isEqualTo(value)

inline fun <reified T> Assertion.Builder<List<Deferred<out T>>>.evaluateTo(vararg values: T) =
    map { it.evaluate() }.containsExactly(values)
