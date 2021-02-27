package koodies.builder.context

import koodies.Deferred
import koodies.test.test
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectCatching
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isNull

@Execution(SAME_THREAD)
class CapturingContextTest {

    inner class TestContext(override val captures: CapturesMap) : CapturingContext() {
        val initializedWithNonNullable: (String) -> Unit by function<String>() default "initial"
        val initializedWithNullable: (String?) -> Unit by function<String?>() default "nullable"
        val initializedWithNull: (String?) -> Unit by function<String?>() default null
        val uninitialized: (String?) -> Unit by function<String?>()
    }

    @Test
    @Suppress("ReplaceNotNullAssertionWithElvisReturn", "UNREACHABLE_CODE")
    fun `non-nullable callable should not accept null`() {
        val context = TestContext(CapturesMap())
        expectCatching { context.initializedWithNonNullable(null!!) }.isFailure().isA<NullPointerException>()
    }

    @TestFactory
    fun `with no invocations`() = test(CapturesMap().also { TestContext(it) }) {
        expect("all initialized delegates have initial invocation") { mappings }.that { hasSize(3) }
        expect { mappings[TestContext::initializedWithNonNullable.name] }.that { isA<Deferred<out String>>().evaluatesTo("initial") }
        expect { mappings[TestContext::initializedWithNullable.name] }.that { isA<Deferred<out String?>>().evaluatesTo("nullable") }
        expect { mappings[TestContext::initializedWithNull.name] }.that { isA<Deferred<out String?>>().evaluatesTo(null) }
        expect { mappings[TestContext::uninitialized.name] }.that { isNull() }
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
        expect("all delegates have stored invocations") { mappings }.that { hasSize(4) }
        expect { mappings[TestContext::initializedWithNonNullable.name] }.that { isA<Deferred<out String>>().evaluatesTo("value") }
        expect { mappings[TestContext::initializedWithNullable.name] }.that { isA<Deferred<out String?>>().evaluatesTo("value") }
        expect { mappings[TestContext::initializedWithNull.name] }.that { isA<Deferred<out String?>>().evaluatesTo("value") }
        expect { mappings[TestContext::uninitialized.name] }.that { isA<Deferred<out String?>>().evaluatesTo("value") }
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
        expect("all delegates have stored invocation") { mappings }.that { hasSize(4) }
        expect { mappings[TestContext::initializedWithNonNullable.name] }.that { isA<Deferred<out String>>().evaluatesTo("new value") }
        expect { mappings[TestContext::initializedWithNullable.name] }.that { isA<Deferred<out String?>>().evaluatesTo("new value") }
        expect { mappings[TestContext::initializedWithNull.name] }.that { isA<Deferred<out String?>>().evaluatesTo("new value") }
        expect { mappings[TestContext::uninitialized.name] }.that { isA<Deferred<out String?>>().evaluatesTo("new value") }
    }
}
