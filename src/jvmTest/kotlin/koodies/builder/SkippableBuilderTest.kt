package koodies.builder

import koodies.builder.SkippableBuilderTest.AllVariantsBuilder.Lists
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.callable
import koodies.test.test
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
    fun `all builder variants can be called using invoke`() = abList.test {

        val built = AllVariantsBuilder().build {
            expect { explicitBuilderInstance { +"a" + "b" } }.that { isEqualTo(abList) }
            expect { explicitBuilderFunction { +"a" + "b" } }.that { isEqualTo(abList) }
            expect { callableBuilderInstance { +"a" + "b" } }.that { isEqualTo(abList) }
            expect { callableInvokeFunction { +"a" + "b" } }.that { isEqualTo(abList) }
            expect { capturingBuilderInstance { +"a" + "b" } }.that { isEqualTo(Unit) }
            expect { capturingBuilderInstanceShorthand { +"a" + "b" } }.that { isEqualTo(Unit) }
        }

        expect { built }.that { captured(abList) }
    }

    @TestFactory
    fun `all builder variants can be called using build`() = abList.test {

        val built = AllVariantsBuilder().build {
            expect { explicitBuilderInstance.build { +"a" + "b" } }.that { isEqualTo(abList) }
            expect { explicitBuilderFunction.build { +"a" + "b" } }.that { isEqualTo(abList) }
            expect { callableBuilderInstance.build { +"a" + "b" } }.that { isEqualTo(abList) }
            expect { callableInvokeFunction.build { +"a" + "b" } }.that { isEqualTo(abList) }
            expect { capturingBuilderInstance.build { +"a" + "b" } }.that { isEqualTo(Unit) }
            expect { capturingBuilderInstanceShorthand.build { +"a" + "b" } }.that { isEqualTo(Unit) }
        }

        expect { built }.that { captured(abList) }
    }

    @TestFactory
    fun `all builder variants can be skipped with using`() = abList.test {

        val built = AllVariantsBuilder().build {
            expect { explicitBuilderInstance using abList }.that { isEqualTo(abList) }
            expect { explicitBuilderFunction using abList }.that { isEqualTo(abList) }
            expect { callableBuilderInstance using abList }.that { isEqualTo(abList) }
            expect { callableInvokeFunction using abList }.that { isEqualTo(abList) }
            expect { capturingBuilderInstance using abList }.that { isEqualTo(Unit) }
            expect { capturingBuilderInstanceShorthand using abList }.that { isEqualTo(Unit) }
        }

        expect { built }.that { captured(abList) }
    }


    @TestFactory
    fun `all builder variants can be skipped with by`() = abList.test {

        val built = AllVariantsBuilder().build {
            expect { explicitBuilderInstance by abList }.that { isEqualTo(abList) }
            expect { explicitBuilderFunction by abList }.that { isEqualTo(abList) }
            expect { callableBuilderInstance by abList }.that { isEqualTo(abList) }
            expect { callableInvokeFunction by abList }.that { isEqualTo(abList) }
            expect { capturingBuilderInstance by abList }.that { isEqualTo(Unit) }
            expect { capturingBuilderInstanceShorthand by abList }.that { isEqualTo(Unit) }
        }

        expect { built }.that { captured(abList) }
    }

    private fun Assertion.Builder<Lists>.captured(expected: List<String>) = compose("all empty but captured") {
        get { explicitBuilderInstance }.isEqualTo(emptyList())
        get { explicitBuilderFunction }.isEqualTo(emptyList())
        get { callableBuilderInstance }.isEqualTo(emptyList())
        get { callableInvokeFunction }.isEqualTo(emptyList())
        get { capturingBuilderInstance }.isEqualTo(expected)
        get { capturingBuilderInstanceShorthand }.isEqualTo(expected)
    }.then { if (allPassed) pass() else fail() }
}
