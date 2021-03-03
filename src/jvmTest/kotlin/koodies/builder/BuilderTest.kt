package koodies.builder

import koodies.test.test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class BuilderTest {

    private class CustomContext(private val list: MutableList<String>) {
        operator fun String.unaryPlus() = list.addAll(split(","))
    }

    private class CustomBuilder : Builder<CustomContext.() -> Int, Pair<Int, List<String>>> {
        override fun invoke(init: CustomContext.() -> Int): Pair<Int, List<String>> {
            val contextState = mutableListOf<String>()
            val initResult = CustomContext(contextState).init()
            return initResult to contextState
        }
    }

    private val builder = CustomBuilder()

    private val init: CustomContext.() -> Int = {
        +"a,b"
        +"c"
        42
    }

    private val built = (42 to listOf("a", "b", "c"))

    private val transform: Pair<Int, List<String>>.() -> String = { "${second.joinToString()}, $first" }
    private val transformed: String = "a, b, c, 42"

    private val transformMultiple: Pair<Int, List<String>>.() -> List<String> = { (39..first).map { i -> "$i-" + second.drop(1).joinToString("-") } }
    private val transformedMultiple: List<String> = listOf("39-b-c", "40-b-c", "41-b-c", "42-b-c")

    @TestFactory
    fun should() = test(builder) {
        group("build") {
            expect { invoke(init) }.that { isEqualTo(built) }
            expect { build(init) }.that { isEqualTo(built) }
        }

        group("skip") {
            expect { invoke(built) }.that { isEqualTo(built) }
            expect { instead(built) }.that { isEqualTo(built) }
        }

        group("build transform") {
            expect { build(init, transform) }.that { isEqualTo(transformed) }
        }

        group("build to") {
            group("list") {
                val destinationList = mutableListOf<Pair<Int, List<String>>>()
                with { buildTo(destinationList, init) }.then {
                    expect { this }.that { isEqualTo(built) }
                    expect { destinationList }.that { containsExactly(built) }
                }
            }
            group("function") {
                var destinationFunctionArgument: Pair<Int, List<String>>? = null
                fun destinationFunction(value: Pair<Int, List<String>>) {
                    destinationFunctionArgument = value
                }
                with { buildTo(::destinationFunction, init) }.then {
                    expect { this }.that { isEqualTo(built) }
                    expect { destinationFunctionArgument }.that { isEqualTo(built) }
                }
            }
        }

        group("build transform to") {
            group("list") {
                val destinationList = mutableListOf<String>()
                with { buildTo(init, destinationList, transform) }.then {
                    expect { this }.that { isEqualTo(transformed) }
                    expect { destinationList }.that { containsExactly(transformed) }
                }
            }
            group("function") {
                var destinationFunctionArgument: String? = null
                fun destinationFunction(value: String) {
                    destinationFunctionArgument = value
                }
                with { buildTo(init, ::destinationFunction, transform) }.then {
                    expect { this }.that { isEqualTo(transformed) }
                    expect { destinationFunctionArgument }.that { isEqualTo(transformed) }
                }
            }
        }

        group("build multiple") {
            with { buildMultiple(init, transformMultiple) }.then {
                expect { this }.that { isEqualTo(transformedMultiple) }
            }
        }

        group("build multiple to") {
            group("list") {
                val destinationList = mutableListOf<String>()
                with { buildMultipleTo(init, destinationList, transformMultiple) }.then {
                    expect { this }.that { isEqualTo(transformedMultiple) }
                    expect { destinationList }.that { containsExactly(transformedMultiple) }
                }
            }
            group("function") {
                var destinationFunctionArguments: MutableList<String> = mutableListOf()
                fun destinationFunction(value: String) {
                    destinationFunctionArguments.add(value)
                }
                with { buildMultipleTo(init, ::destinationFunction, transformMultiple) }.then {
                    expect { this }.that { isEqualTo(transformedMultiple) }
                    expect { destinationFunctionArguments }.that { containsExactly(transformedMultiple) }
                }
            }
        }
    }
}
