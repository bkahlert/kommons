package koodies.test

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThan
import strikt.assertions.isTrue
import strikt.assertions.size
import java.util.stream.Stream
import kotlin.streams.toList

@Execution(CONCURRENT)
class TestsTest {
    private val subjects = listOf(1, 2, 3, 4, 5)

    @Nested
    inner class Tests {

        @Test
        fun `should run create one container for each subject`() {
            val tests = subjects.tests { subject ->
                test("test 1: $subject") { }
                test("test 2: $subject") { }
            }

            expectThat(tests) {
                size.isEqualTo(subjects.size)
                all {
                    isA<DynamicContainer>()
                        .get("tests") { children.toList() }
                        .all {
                            isA<DynamicTest>()
                                .get("display name") { displayName }
                                .matchesCurlyPattern("test {}: {}")
                        }
                }
            }
        }

        @Test
        fun `should run all tests`() {
            val executions1 = subjects.associateWith { false }.toMutableMap()
            val executions2 = subjects.associateWith { false }.toMutableMap()
            subjects.tests { subject ->
                test("test 1: $subject") {
                    executions1[subject] = true
                    expectThat(subject).isLessThan(3)
                }
                test("test 2: $subject") {
                    executions2[subject] = true
                    expectThat(true).isTrue()
                }
            }.runTests()

            expect {
                that(executions1.values).all { isTrue() }
                that(executions2.values).all { isTrue() }
            }
        }
    }

//    @Nested
//    inner class DependentTests {
//
//        @Test
//        fun `should run create one container for each subject`() {
//            val tests = subjects.tests { subject ->
//                test("test 1: $subject") { }
//                test("test 2: $subject") { }
//            }
//
//            expectThat(tests) {
//                size.isEqualTo(subjects.size)
//                all {
//                    isA<DynamicContainer>()
//                        .get("tests") { children.toList() }
//                        .all {
//                            isA<DynamicTest>()
//                                .get("display name") { displayName }
//                                .matchesCurlyPattern("test {}: {}")
//                        }
//                }
//            }
//        }
//
//        @Test
//        fun `should run hide dependent tests on failure`() {
//            val executions1 = subjects.associateWith { false }.toMutableMap()
//            val executions2 = subjects.associateWith { false }.toMutableMap()
//            subjects.tests(dep) { subject ->
//                test("test 1: $subject") {
//                    executions1[subject] = true
//                    expectThat(subject).isLessThan(3)
//                }
//                test("test 2: $subject") {
//                    executions2[subject] = true
//                    expectThat(true).isTrue()
//                }
//            }.runTests()
//
//            expect {
//                that(executions1.values).all { isTrue() }
//                that(executions2.values).all { isTrue() }
//            }
//        }
//    }
}

private fun List<DynamicNode>.runTests() = forEach {
    println("Running ${it.displayName}")
    it.runTests()
}

private fun Stream<DynamicNode>.runTests() = forEach { it.runTests() }

private fun DynamicNode.runTests() {
    println("Running $displayName")
    when (this) {
        is DynamicContainer -> {
            children.forEach { it.runTests() }
        }
        is DynamicTest -> kotlin.runCatching {
            executable.execute()
        }
        else -> println("$testSourceUri has unknown type")
    }
}
