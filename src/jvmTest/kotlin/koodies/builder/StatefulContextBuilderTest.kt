package koodies.builder

import koodies.builder.context.StatefulContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import kotlin.properties.Delegates.observable

@Execution(SAME_THREAD)
class StatefulContextBuilderTest {

    private companion object {
        private val PRINT_STATE_CHANGES = true
    }

    private interface CustomContext {
        operator fun Int.plus(string: String) = this + string.length
        operator fun String.times(int: Int) = length + int
    }

    private class CustomStatefulContext : StatefulContext<CustomContext, Int> {
        override val state: Int get() = total
        private var total: Int by observable(0) { _, old, new ->
            if (PRINT_STATE_CHANGES) println("state changed from $old to $new")
        }

        override val context: CustomContext = object : CustomContext {
            override fun Int.plus(string: String): Int = (this + string.length).also { total += it }
            override fun String.times(int: Int): Int = int + this
        }
    }

    private class CustomStatefulContextBuilder(
        override val statefulContext: StatefulContext<CustomContext, Int> = CustomStatefulContext(),
    ) : StatefulContextBuilder<CustomContext, Int, Double> {
        override val transform: Int.() -> Double = { toDouble() }
    }

    private val builder = CustomStatefulContextBuilder()

    @Test
    fun `should build by changing the context state`() {
        expectThat(builder.build {
            10 + "123" * 10 + 10 + "a" + "𓌈🥸𓂈"
            "0" * (-130)
        }).isEqualTo(-42.0)
    }
}
