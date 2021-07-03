package koodies.builder.context

import koodies.Deferred
import koodies.builder.SkippableBuilder
import koodies.test.test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectCatching
import strikt.assertions.containsExactly
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isFailure
import strikt.assertions.isNull
import strikt.assertions.map

class CapturingContextTest {

    open inner class TestContext(override val captures: CapturesMap) : CapturingContext() {
        val initializedWithNonNullable: (String) -> Unit by function<String>() default "initial"
        val initializedWithNullable: (String?) -> Unit by function<String?>() default "nullable"
        val initializedWithNull: (String?) -> Unit by function<String?>() default null
        val uninitialized: (String?) -> Unit by function()
    }

    @Test
    @Suppress("ReplaceNotNullAssertionWithElvisReturn", "UNREACHABLE_CODE")
    fun `non-nullable callable should not accept null`() {
        val context = TestContext(CapturesMap())
        expectCatching { context.initializedWithNonNullable(null!!) }.isFailure().isA<NullPointerException>()
    }

    @TestFactory
    fun `with no invocations`() = test(CapturesMap().also { TestContext(it) }) {
        expecting { mostRecent(TestContext::initializedWithNonNullable) } that { isA<Deferred<out String>>().evaluatesTo("initial") }
        expecting { mostRecent(TestContext::initializedWithNullable) } that { isA<Deferred<out String?>>().evaluatesTo("nullable") }
        expecting { mostRecent(TestContext::initializedWithNull) } that { isA<Deferred<out String?>>().evaluatesTo(null) }
        expecting { mostRecent(TestContext::uninitialized) } that { isNull() }
        expecting { getAll { true } } that { isEmpty() }
    }

    @TestFactory
    fun `with one invocation`() = test(CapturesMap().also {
        TestContext(it).apply {
            initializedWithNonNullable("value")
            initializedWithNullable("value")
            initializedWithNull("value")
            uninitialized("value")
        }
    }) {
        expecting { mostRecent(TestContext::initializedWithNonNullable) } that { isA<Deferred<out String>>().evaluatesTo("value") }
        expecting { mostRecent(TestContext::initializedWithNullable) } that { isA<Deferred<out String?>>().evaluatesTo("value") }
        expecting { mostRecent(TestContext::initializedWithNull) } that { isA<Deferred<out String?>>().evaluatesTo("value") }
        expecting { mostRecent(TestContext::uninitialized) } that { isA<Deferred<out String?>>().evaluatesTo("value") }
        expecting { getAll { true } } that {
            hasSize(4)
            map { it.evaluate() }.containsExactly("value", "value", "value", "value")
        }
    }

    @TestFactory
    fun `with multiple invocation`() = test(CapturesMap().also {
        TestContext(it).apply {
            initializedWithNonNullable("value")
            initializedWithNullable("value")
            initializedWithNull("value")
            uninitialized("value")
            initializedWithNonNullable("new value")
            initializedWithNullable("new value")
            initializedWithNull("new value")
            uninitialized("new value")
        }
    }) {
        expecting { mostRecent(TestContext::initializedWithNonNullable) } that { isA<Deferred<out String>>().evaluatesTo("new value") }
        expecting { mostRecent(TestContext::initializedWithNullable) } that { isA<Deferred<out String?>>().evaluatesTo("new value") }
        expecting { mostRecent(TestContext::initializedWithNull) } that { isA<Deferred<out String?>>().evaluatesTo("new value") }
        expecting { mostRecent(TestContext::uninitialized) } that { isA<Deferred<out String?>>().evaluatesTo("new value") }
        expecting { getAll { true } } that {
            hasSize(8)
            map { it.evaluate() }.containsExactly("value", "value", "value", "value", "new value", "new value", "new value", "new value")
        }
    }

    @Nested
    inner class WithDelegation {
        inner class DelegationTestContext(captures: CapturesMap) : TestContext(captures) {
            val delegatingToInitializedWithNonNullable: SkippableBuilder<() -> String, String, Unit> by builder<String>() then initializedWithNonNullable
            val delegatingToInitializedWithNullable: SkippableBuilder<() -> String?, String?, Unit> by builder<String?>() then initializedWithNullable
            val delegatingToInitializedWithNull: SkippableBuilder<() -> String?, String?, Unit> by builder<String?>() then initializedWithNull
            val delegatingToUninitialized: SkippableBuilder<() -> String, String, Unit> by builder<String>() then uninitialized
        }

        @Test
        @Suppress("ReplaceNotNullAssertionWithElvisReturn", "UNREACHABLE_CODE")
        fun `non-nullable callable should not accept null`() {
            val context = DelegationTestContext(CapturesMap())
            expectCatching { context.delegatingToInitializedWithNonNullable { null!! } }.isFailure().isA<NullPointerException>()
        }

        @TestFactory
        fun `with no invocations`() = test(CapturesMap().also { DelegationTestContext(it) }) {
            expecting { mostRecent(DelegationTestContext::initializedWithNonNullable) } that { isA<Deferred<out String>>().evaluatesTo("initial") }
            expecting { mostRecent(DelegationTestContext::initializedWithNullable) } that { isA<Deferred<out String?>>().evaluatesTo("nullable") }
            expecting { mostRecent(DelegationTestContext::initializedWithNull) } that { isA<Deferred<out String?>>().evaluatesTo(null) }
            expecting { mostRecent(DelegationTestContext::uninitialized) } that { isNull() }
            expecting { getAll { true } } that { isEmpty() }
        }

        @TestFactory
        fun `with one invocation`() = test(CapturesMap().also {
            DelegationTestContext(it).apply {
                delegatingToInitializedWithNonNullable { "value" }
                delegatingToInitializedWithNullable { "value" }
                delegatingToInitializedWithNull { "value" }
                delegatingToUninitialized { "value" }
            }
        }) {
            expecting { mostRecent(DelegationTestContext::initializedWithNonNullable) } that { isA<Deferred<out String>>().evaluatesTo("value") }
            expecting { mostRecent(DelegationTestContext::initializedWithNullable) } that { isA<Deferred<out String?>>().evaluatesTo("value") }
            expecting { mostRecent(DelegationTestContext::initializedWithNull) } that { isA<Deferred<out String?>>().evaluatesTo("value") }
            expecting { mostRecent(DelegationTestContext::uninitialized) } that { isA<Deferred<out String?>>().evaluatesTo("value") }
            expecting { getAll { true } } that {
                hasSize(4)
                map { it.evaluate() }.containsExactly("value", "value", "value", "value")
            }
        }

        @TestFactory
        fun `with multiple invocation`() = test(CapturesMap().also {
            DelegationTestContext(it).apply {
                delegatingToInitializedWithNonNullable { "value" }
                delegatingToInitializedWithNullable { "value" }
                delegatingToInitializedWithNull { "value" }
                delegatingToUninitialized { "value" }
                delegatingToInitializedWithNonNullable { "new value" }
                delegatingToInitializedWithNullable { "new value" }
                delegatingToInitializedWithNull { "new value" }
                delegatingToUninitialized { "new value" }
            }
        }) {
            expecting { mostRecent(DelegationTestContext::initializedWithNonNullable) } that { isA<Deferred<out String>>().evaluatesTo("new value") }
            expecting { mostRecent(DelegationTestContext::initializedWithNullable) } that { isA<Deferred<out String?>>().evaluatesTo("new value") }
            expecting { mostRecent(DelegationTestContext::initializedWithNull) } that { isA<Deferred<out String?>>().evaluatesTo("new value") }
            expecting { mostRecent(DelegationTestContext::uninitialized) } that { isA<Deferred<out String?>>().evaluatesTo("new value") }
            expecting { getAll { true } } that {
                hasSize(8)
                map { it.evaluate() }.containsExactly("value", "value", "value", "value", "new value", "new value", "new value", "new value")
            }
        }
    }
}
