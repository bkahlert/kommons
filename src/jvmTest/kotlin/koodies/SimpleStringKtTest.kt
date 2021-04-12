package koodies

import koodies.text.ansiRemoved
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

@Execution(SAME_THREAD)
class SimpleStringKtTest {

    private companion object

    @Nested
    inner class ForKClass {

        @Test
        fun `should return class name`() {
            expectThat(SimpleStringKtTest::class.simpleClassName)
                .ansiRemoved.isEqualTo("SimpleStringKtTest")
        }

        @Test
        fun `should return inner class name`() {
            expectThat(ForKClass::class.simpleClassName)
                .ansiRemoved.isEqualTo("SimpleStringKtTest.ForKClass")
        }

        @Test
        fun `should return companion class name`() {
            expectThat(Companion::class.simpleClassName)
                .ansiRemoved.isEqualTo("SimpleStringKtTest.Companion")
        }

        @Test
        fun `should return anonymous class name`() {
            expectThat((object : Any() {})::class.simpleClassName)
                .ansiRemoved.isEqualTo("SimpleStringKtTest.ForKClass.should return anonymous class name.1")
        }

        @Test
        fun `should return lambda class name`() {
            val lambda: (String) -> Int = { 42 }
            expectThat(lambda::class.simpleClassName)
                .ansiRemoved.isEqualTo("SimpleStringKtTest.ForKClass.should return lambda class name.lambda.1")
        }
    }


    @Nested
    inner class ForLambdas {
        private inner class Receiver

        private val returningLambda: () -> Int = { 42 }
        private val lambdaWithReceiver: Receiver.() -> Int = { 42 }
        private val lambdaWithArgument: (Float) -> Int = { _ -> 42 }
        private val lambdaWithArguments: (Int, Float) -> Int = { _, _ -> 42 }

        @Test
        fun `should format returning lambdas`() {
            expectThat(returningLambda.toSimpleString()) {
                isNotEqualTo(returningLambda.toString())
                isEqualTo("() -> Int")
            }
        }

        @Test
        fun `should format lambdas with receiver`() {
            expectThat(lambdaWithReceiver.toSimpleString()) {
                isNotEqualTo(lambdaWithReceiver.toString())
                isEqualTo("Receiver.() -> Int")
            }
        }

        @Test
        fun `should format lambdas with one argument`() {
            expectThat(lambdaWithArgument.toSimpleString()) {
                isNotEqualTo(lambdaWithArgument.toString())
                isEqualTo("(Float) -> Int")
            }
        }

        @Test
        fun `should truncate lambdas with more arguments`() {
            expectThat(lambdaWithArguments.toSimpleString()) {
                isNotEqualTo(lambdaWithArguments.toString())
                isEqualTo("(Int, ⋯) -> Int")
            }
        }
    }
}
