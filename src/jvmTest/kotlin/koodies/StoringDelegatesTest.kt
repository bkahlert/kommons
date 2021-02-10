package koodies

import koodies.StoringDelegates.storing
import koodies.StoringDelegates.storingFunction
import koodies.number.isOdd
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion
import strikt.api.expect
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible

@Execution(SAME_THREAD)
class StoringDelegatesTest {

    class SomeClass {
        var stringStoringProperty by storing("initial")
        var pickyStringStoringProperty by storing("initial") { property, history, newValue -> newValue.length.isOdd }

        val stringStoringFunction by storingFunction("initial")
        val pickyStringStoringFunction by storingFunction("initial") { property, history, newValue -> newValue.length.isOdd }
    }


    @Nested
    inner class StoringProperties {

        @Suppress("UNCHECKED_CAST")
        private val <T : KProperty0<String>> Assertion.Builder<T>.history: Assertion.Builder<StoredValueHistory<String>>
            get() = get("history") {
                isAccessible = true
                val delegate = getDelegate()
                check(delegate is ValueHistoryStoring<*>)
                delegate.history as StoredValueHistory<String>
            }

        @Test
        fun `should have initial value`() {
            val someObject = SomeClass()
            expect {
                that(someObject.stringStoringProperty).isEqualTo("initial")
                that(someObject::stringStoringProperty).history.containsExactly("initial")
            }
        }

        @Test
        fun `should store a new value`() {
            val someObject = SomeClass()
            someObject.stringStoringProperty = "new value"
            expect {
                that(someObject.stringStoringProperty).isEqualTo("new value")
                that(someObject::stringStoringProperty).history.containsExactly("initial", "new value")
            }
        }

        @Test
        fun `should discard a new value`() {
            val someObject = SomeClass()
            someObject.pickyStringStoringProperty = "even"
            expect {
                that(someObject.pickyStringStoringProperty).isEqualTo("initial")
                that(someObject::pickyStringStoringProperty).history.containsExactly("initial")
            }
        }
    }

    @Nested
    inner class StoringFunctions {

        @Suppress("UNCHECKED_CAST")
        private val <T : KProperty0<Function1<String, Unit>>> Assertion.Builder<T>.history: Assertion.Builder<StoredValueHistory<String>>
            get() = get("history") {
                isAccessible = true
                val delegate = getDelegate()
                check(delegate is ValueHistoryStoring<*>)
                delegate.history as StoredValueHistory<String>
            }

        @Test
        fun `should have initial value`() {
            val someObject = SomeClass()
            expect {
                that(someObject::stringStoringFunction).history.containsExactly("initial")
            }
        }

        @Test
        fun `should store a new value`() {
            val someObject = SomeClass()
            someObject.stringStoringFunction("new value")
            expect {
                that(someObject::stringStoringFunction).history.containsExactly("initial", "new value")
            }
        }

        @Test
        fun `should discard a new value`() {
            val someObject = SomeClass()
            someObject.pickyStringStoringFunction("even")
            expect {
                that(someObject::pickyStringStoringFunction).history.containsExactly("initial")
            }
        }
    }
}
