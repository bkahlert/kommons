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
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
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
            group("getAll") {
                expecting { getAll() } that { isEmpty() }
                expecting { getAll<String>(::delegatedFunction) } that { isEmpty() }
            }

            group("get") {
                expecting { get<String>(::delegatedFunction) } that { evaluatesTo(null) }
                expecting { get<String>(::delegatedFunction) { it as String } } that { evaluatesTo(null) }
                expecting { get<String>("delegatedFunction") } that { evaluatesTo(null) }
                expecting { get<String>("delegatedFunction") { it as String } } that { evaluatesTo(null) }
            }

            group("wrong type") {
                expecting { get<Double>(::delegatedFunction) } that { evaluatesTo(null) }
                expecting { get<Double>(::delegatedFunction) { it as Double } } that { evaluatesTo(null) }
                expecting { get<Double>("delegatedFunction") } that { evaluatesTo(null) }
                expecting { get<Double>("delegatedFunction") { it as Double } } that { evaluatesTo(null) }
            }

            group("getOrDefault") {
                expecting { getOrDefault(::delegatedFunction, "default") } that { evaluatesTo("default") }
                expecting { getOrDefault("delegatedFunction", "default") } that { evaluatesTo("default") }
            }

            group("getOrThrow") {
                group("default exception") {
                    expectThrows<NoSuchElementException> { getOrThrow<String>(::delegatedFunction).evaluate() }
                    expectThrows<NoSuchElementException> { getOrThrow<String>("delegatedFunction").evaluate() }
                }
                group("custom exception") {
                    expectThrows<AssertionError> { getOrThrow<String>(::delegatedFunction) { Exceptions.AE("test") }.evaluate() }
                    expectThrows<AssertionError> { getOrThrow<String>("delegatedFunction") { Exceptions.AE("test") }.evaluate() }
                }
            }
        }

        @TestFactory
        fun `using extension functions`() = test(capturesMap) {
            expecting { ::delegatedFunction.evalAll<String>() } that { isEmpty() }
            expecting { ::delegatedFunction.evalOrNull<String>() } that { isNull() }
            expecting { ::delegatedFunction.evalOrDefault("default") } that { isEqualTo("default") }
            expecting { ::delegatedFunction.evalOrDefault { "default" } } that { isEqualTo("default") }
            expectThrows<NoSuchElementException> { ::delegatedFunction.eval<String>() }
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
            group("getAll") {
                expecting { getAll() } that { evaluateTo("2", "1", "3") }
                expecting { getAll<String>(::delegatedFunction) } that { evaluateTo("2", "1", "3") }
            }

            group("get") {
                expecting { get<String>(::delegatedFunction) } that { evaluatesTo("3") }
                expecting { get<String>(::delegatedFunction) { it as String } } that { evaluatesTo("3") }
                expecting { get<String>("delegatedFunction") } that { evaluatesTo("3") }
                expecting { get<String>("delegatedFunction") { it as String } } that { evaluatesTo("3") }
            }

            group("wrong type") {
                expectThrows<ClassCastException> { get<Double>(::delegatedFunction).evaluate() }
                expectThrows<ClassCastException> { get(::delegatedFunction) { it as Double }.evaluate() }
                expectThrows<ClassCastException> { get<Double>("delegatedFunction").evaluate() }
                expectThrows<ClassCastException> { get("delegatedFunction") { it as Double }.evaluate() }
            }

            group("getOrDefault") {
                expecting { getOrDefault(::delegatedFunction, "default") } that { evaluatesTo("3") }
                expecting { getOrDefault("delegatedFunction", "default") } that { evaluatesTo("3") }
            }

            group("getOrThrow") {
                group("default exception") {
                    expecting { getOrThrow<String>(::delegatedFunction) } that { evaluatesTo("3") }
                    expecting { getOrThrow<String>("delegatedFunction") } that { evaluatesTo("3") }
                }
                group("custom exception") {
                    expecting { getOrThrow<String>(::delegatedFunction) { Exceptions.AE("test") } } that { evaluatesTo("3") }
                    expecting { getOrThrow<String>("delegatedFunction") { Exceptions.AE("test") } } that { evaluatesTo("3") }
                }
            }
        }

        @TestFactory
        fun `using extension functions`() = test(capturesMap) {
            expecting { ::delegatedFunction.evalAll<String>() } that { containsExactly("2", "1", "3") }
            expecting { ::delegatedFunction.evalOrNull<String>() } that { isEqualTo("3") }
            expecting { ::delegatedFunction.evalOrDefault("default") } that { isEqualTo("3") }
            expecting { ::delegatedFunction.evalOrDefault { "default" } } that { isEqualTo("3") }
            expecting { ::delegatedFunction.eval<String>() } that { isEqualTo("3") }
        }
    }
}

inline fun <reified T> Assertion.Builder<Deferred<out T>>.evaluatesTo(value: T) =
    get("evaluating") { evaluate() }.isEqualTo(value)

inline fun <reified T> Assertion.Builder<List<Deferred<out T>>>.evaluateTo(vararg values: T) =
    map { it.evaluate() }.containsExactly(*values)
