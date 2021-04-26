package koodies.builder

import koodies.test.testEach
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class BooleanBuilderTest {

    @TestFactory
    fun `should accept booleans by default`() = testEach(
        { BooleanBuilder { true } } to true,
        { BooleanBuilder { false } } to false,
        { BooleanBuilder.build { true } } to true,
        { BooleanBuilder.build { false } } to false,

        { BooleanBuilder.using(true) } to true,
        { BooleanBuilder.by(true) } to true,
        { BooleanBuilder.using(false) } to false,
        { BooleanBuilder.by(false) } to false,
    ) { (build, expected) ->
        expecting { build() } that { isEqualTo(expected) }
    }

    @TestFactory
    fun `should provide lower case on off domain booleans`() = testEach(
        { BooleanBuilder.OnOff { on } } to true,
        { BooleanBuilder.OnOff { off } } to false,
        { BooleanBuilder.OnOff.build { on } } to true,
        { BooleanBuilder.OnOff.build { off } } to false,

        { BooleanBuilder.OnOff.using(true) } to true,
        { BooleanBuilder.OnOff.by(true) } to true,
        { BooleanBuilder.OnOff.using(false) } to false,
        { BooleanBuilder.OnOff.by(false) } to false,
    ) { (build, expected) ->
        expecting { build() } that { isEqualTo(expected) }
    }

    @TestFactory
    fun `should provide UPPER CASE ON OFF domain booleans`() = testEach(
        { BooleanBuilder.ON_OFF { ON } } to true,
        { BooleanBuilder.ON_OFF { OFF } } to false,
        { BooleanBuilder.ON_OFF.build { ON } } to true,
        { BooleanBuilder.ON_OFF.build { OFF } } to false,

        { BooleanBuilder.ON_OFF.using(true) } to true,
        { BooleanBuilder.ON_OFF.using(true) } to true,
        { BooleanBuilder.ON_OFF.using(false) } to false,
        { BooleanBuilder.ON_OFF.using(false) } to false,
    ) { (build, expected) ->
        expecting { build() } that { isEqualTo(expected) }
    }
}
