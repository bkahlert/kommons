package koodies.builder

import koodies.builder.SkippableBuilderTest.AllVariantsBuilder.Lists
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.callable
import koodies.test.tests
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class SkippableBuilderTest {

    companion object {
        private val abList = listOf("a", "b")
    }

    class AllVariantsBuilder : BuilderTemplate<AllVariantsBuilder.Context, Lists>() {
        inner class Context(override val captures: CapturesMap) : CapturingContext() {
            val explicitBuilderInstance = ListBuilder<String>()
            val explicitBuilderFunction = ListBuilder<String>()::invoke

            val callableBuilderInstance by callable(ListBuilder<String>())
            val callableInvokeFunction by callable(ListBuilder<String>()::invoke)

            val capturingBuilderInstance by builder(emptyList(), ListBuilder<String>())
            val capturingBuilderInstanceShorthand by ListBuilder<String>()
        }

        override fun BuildContext.build(): Lists = ::Context {
            Lists(
                ::explicitBuilderInstance.evalOrDefault(emptyList()),
                ::explicitBuilderFunction.evalOrDefault(emptyList()),
                ::callableBuilderInstance.evalOrDefault(emptyList()),
                ::callableInvokeFunction.evalOrDefault(emptyList()),
                ::capturingBuilderInstance.eval(),
                ::capturingBuilderInstanceShorthand.evalOrDefault(emptyList()),
            )
        }

        data class Lists(
            val explicitBuilderInstance: List<String>,
            val explicitBuilderFunction: List<String>,
            val callableBuilderInstance: List<String>,
            val callableInvokeFunction: List<String>,
            val capturingBuilderInstance: List<String>,
            val capturingBuilderInstanceShorthand: List<String>,
        )
    }

    @TestFactory
    fun `all builder variants can be called using invoke`() = tests {

        AllVariantsBuilder().build {
            expecting { explicitBuilderInstance { +"a" + "b" } } that { isEqualTo(abList) }
            expecting { explicitBuilderFunction { +"a" + "b" } } that { isEqualTo(abList) }
            expecting { callableBuilderInstance { +"a" + "b" } } that { isEqualTo(abList) }
            expecting { callableInvokeFunction { +"a" + "b" } } that { isEqualTo(abList) }
            expecting { capturingBuilderInstance { +"a" + "b" } } that { isEqualTo(Unit) }
            expecting { capturingBuilderInstanceShorthand { +"a" + "b" } } that { isEqualTo(Unit) }
        }

        expecting {
            AllVariantsBuilder().build {
                explicitBuilderInstance { +"a" + "b" }
                explicitBuilderFunction { +"a" + "b" }
                callableBuilderInstance { +"a" + "b" }
                callableInvokeFunction { +"a" + "b" }
                capturingBuilderInstance { +"a" + "b" }
                capturingBuilderInstanceShorthand { +"a" + "b" }
            }
        } that { captured(abList) }
    }

    @TestFactory
    fun `all builder variants can be called using build`() = tests {

        AllVariantsBuilder().build {
            expecting { explicitBuilderInstance.build { +"a" + "b" } } that { isEqualTo(abList) }
            expecting { explicitBuilderFunction.build { +"a" + "b" } } that { isEqualTo(abList) }
            expecting { callableBuilderInstance.build { +"a" + "b" } } that { isEqualTo(abList) }
            expecting { callableInvokeFunction.build { +"a" + "b" } } that { isEqualTo(abList) }
            expecting { capturingBuilderInstance.build { +"a" + "b" } } that { isEqualTo(Unit) }
            expecting { capturingBuilderInstanceShorthand.build { +"a" + "b" } } that { isEqualTo(Unit) }
        }

        expecting {
            AllVariantsBuilder().build {
                explicitBuilderInstance.build { +"a" + "b" }
                explicitBuilderFunction.build { +"a" + "b" }
                callableBuilderInstance.build { +"a" + "b" }
                callableInvokeFunction.build { +"a" + "b" }
                capturingBuilderInstance.build { +"a" + "b" }
                capturingBuilderInstanceShorthand.build { +"a" + "b" }
            }
        } that { captured(abList) }
    }

    @TestFactory
    fun `all builder variants can be skipped with using`() = tests {

        AllVariantsBuilder().build {
            expecting { explicitBuilderInstance using abList } that { isEqualTo(abList) }
            expecting { explicitBuilderFunction using abList } that { isEqualTo(abList) }
            expecting { callableBuilderInstance using abList } that { isEqualTo(abList) }
            expecting { callableInvokeFunction using abList } that { isEqualTo(abList) }
            expecting { capturingBuilderInstance using abList } that { isEqualTo(Unit) }
            expecting { capturingBuilderInstanceShorthand using abList } that { isEqualTo(Unit) }
        }

        expecting {
            AllVariantsBuilder().build {
                explicitBuilderInstance using abList
                explicitBuilderFunction using abList
                callableBuilderInstance using abList
                callableInvokeFunction using abList
                capturingBuilderInstance using abList
                capturingBuilderInstanceShorthand using abList
            }
        } that { captured(abList) }
    }


    @TestFactory
    fun `all builder variants can be skipped with by`() = tests {

        AllVariantsBuilder().build {
            expecting { explicitBuilderInstance by abList } that { isEqualTo(abList) }
            expecting { explicitBuilderFunction by abList } that { isEqualTo(abList) }
            expecting { callableBuilderInstance by abList } that { isEqualTo(abList) }
            expecting { callableInvokeFunction by abList } that { isEqualTo(abList) }
            expecting { capturingBuilderInstance by abList } that { isEqualTo(Unit) }
            expecting { capturingBuilderInstanceShorthand by abList } that { isEqualTo(Unit) }
        }

        expecting {
            AllVariantsBuilder().build {
                explicitBuilderInstance by abList
                explicitBuilderFunction by abList
                callableBuilderInstance by abList
                callableInvokeFunction by abList
                capturingBuilderInstance by abList
                capturingBuilderInstanceShorthand by abList
            }
        } that { captured(abList) }
    }

    private fun Assertion.Builder<Lists>.captured(expected: List<String>) = compose("all empty but captured") {
        get("explicitBuilderInstance") { explicitBuilderInstance }.isEqualTo(emptyList())
        get("explicitBuilderFunction") { explicitBuilderFunction }.isEqualTo(emptyList())
        get("callableBuilderInstance") { callableBuilderInstance }.isEqualTo(emptyList())
        get("callableInvokeFunction") { callableInvokeFunction }.isEqualTo(emptyList())
        get("capturingBuilderInstance") { capturingBuilderInstance }.isEqualTo(expected)
        get("capturingBuilderInstanceShorthand") { capturingBuilderInstanceShorthand }.isEqualTo(expected)
    }.then { if (allPassed) pass() else fail() }
}
