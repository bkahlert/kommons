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
        { BooleanBuilder(true) } to true,
        { BooleanBuilder { true } } to true,
        { BooleanBuilder(false) } to false,
        { BooleanBuilder { false } } to false,

        { BooleanBuilder.instead(true) } to true,
        { BooleanBuilder.build { true } } to true,
        { BooleanBuilder.instead(false) } to false,
        { BooleanBuilder.build { false } } to false,
    ) { (build, expected) ->
        expect { build() }.that { isEqualTo(expected) }
    }

    @TestFactory
    fun `should provide lower case on off domain booleans`() = testEach(
        { BooleanBuilder.OnOff(true) } to true,
        { BooleanBuilder.OnOff { on } } to true,
        { BooleanBuilder.OnOff(false) } to false,
        { BooleanBuilder.OnOff { off } } to false,

        { BooleanBuilder.OnOff.instead(true) } to true,
        { BooleanBuilder.OnOff.build { on } } to true,
        { BooleanBuilder.OnOff.instead(false) } to false,
        { BooleanBuilder.OnOff.build { off } } to false,
    ) { (build, expected) ->
        expect { build() }.that { isEqualTo(expected) }
    }

    @TestFactory
    fun `should provide UPPER CASE ON OFF domain booleans`() = testEach(
        { BooleanBuilder.ON_OFF(true) } to true,
        { BooleanBuilder.ON_OFF { ON } } to true,
        { BooleanBuilder.ON_OFF(false) } to false,
        { BooleanBuilder.ON_OFF { OFF } } to false,

        { BooleanBuilder.ON_OFF.instead(true) } to true,
        { BooleanBuilder.ON_OFF.build { ON } } to true,
        { BooleanBuilder.ON_OFF.instead(false) } to false,
        { BooleanBuilder.ON_OFF.build { OFF } } to false,
    ) { (build, expected) ->
        expect { build() }.that { isEqualTo(expected) }
    }
}
