package koodies.builder

import koodies.test.test
import org.junit.jupiter.api.TestFactory
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

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
            expecting { invoke(init) } that { isEqualTo(built) }
            expecting { build(init) } that { isEqualTo(built) }
        }

        group("build transform") {
            expecting { build(init, transform) } that { isEqualTo(transformed) }
        }

        group("build to") {
            group("list") {
                val destinationList = mutableListOf<Pair<Int, List<String>>>()
                with { buildTo(destinationList, init) }.then {
                    expecting { this } that { isEqualTo(built) }
                    expecting { destinationList } that { containsExactly(built) }
                }
            }
            group("function") {
                var destinationFunctionArgument: Pair<Int, List<String>>? = null
                fun destinationFunction(value: Pair<Int, List<String>>) {
                    destinationFunctionArgument = value
                }
                with { buildTo(::destinationFunction, init) }.then {
                    expecting { this } that { isEqualTo(built) }
                    expecting { destinationFunctionArgument } that { isEqualTo(built) }
                }
            }
        }

        group("build transform to") {
            group("list") {
                val destinationList = mutableListOf<String>()
                with { buildTo(init, destinationList, transform) }.then {
                    expecting { this } that { isEqualTo(transformed) }
                    expecting { destinationList } that { containsExactly(transformed) }
                }
            }
            group("function") {
                var destinationFunctionArgument: String? = null
                fun destinationFunction(value: String) {
                    destinationFunctionArgument = value
                }
                with { buildTo(init, ::destinationFunction, transform) }.then {
                    expecting { this } that { isEqualTo(transformed) }
                    expecting { destinationFunctionArgument } that { isEqualTo(transformed) }
                }
            }
        }

        group("build multiple") {
            with { buildMultiple(init, transformMultiple) }.then {
                expecting { this } that { isEqualTo(transformedMultiple) }
            }
        }

        group("build multiple to") {
            group("list") {
                val destinationList = mutableListOf<String>()
                with { buildMultipleTo(init, destinationList, transformMultiple) }.then {
                    expecting { this } that { isEqualTo(transformedMultiple) }
                    expecting { destinationList } that { containsExactly(transformedMultiple) }
                }
            }
            group("function") {
                var destinationFunctionArguments: MutableList<String> = mutableListOf()
                fun destinationFunction(value: String) {
                    destinationFunctionArguments.add(value)
                }
                with { buildMultipleTo(init, ::destinationFunction, transformMultiple) }.then {
                    expecting { this } that { isEqualTo(transformedMultiple) }
                    expecting { destinationFunctionArguments } that { containsExactly(transformedMultiple) }
                }
            }
        }
    }
}
