package koodies.number

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class FormattingKtTest {

    @TestFactory
    fun `should format scientifically`() = listOf(
        "0.00000085" to "8.50e-7",
        "0.000002" to "2.00e-6",
        "0.0000001" to "1.00e-7",
        "0" to "0.00e0",
        "1" to "1.00e+0",
        "10" to "1.00e+1",
        "99999950000000000000000000000000000000000000000000" to "1.00e+50",
        "100000050000000000000000000000000000000000000000000" to "1.00e+50",
        "10000000000000000000000000000000000000000000" to "1.00e+43",
    ).flatMap { (value, expected) ->
        listOf(
            dynamicTest("BigDecimal: $value -> $expected") {
                expectThat(BigDecimal.parseString(value).formatScientifically()).isEqualTo(expected)
            },
            dynamicTest("Double: $value -> $expected") {
                expectThat(value.toDouble().formatScientifically()).isEqualTo(expected)
            },
        )
    }

    @TestFactory
    fun `should format to exact decimals`() = listOf(
        "0.00000085" to "0.0000008500",
        "0.000002" to "0.0000020000",
        "0.0000001" to "0.0000001000",
        "0" to "0.0000000000",
        "1" to "1.0000000000",
        "10" to "10.0000000000",
        "99999950000000000000000000000000000000000000000000" to "99999950000000000000000000000000000000000000000000.0000000000",
        "100000050000000000000000000000000000000000000000000" to "100000050000000000000000000000000000000000000000000.0000000000",
        "10000000000000000000000000000000000000000000" to "10000000000000000000000000000000000000000000.0000000000",
    ).flatMap { (value, expected) ->
        listOf(
            dynamicTest("BigDecimal: $value -> $expected") {
                expectThat(BigDecimal.parseString(value).formatToExactDecimals(10)).isEqualTo(expected)
            },
            dynamicTest("Double: $value -> $expected") {
                expectThat(value.toDouble().formatToExactDecimals(10)).isEqualTo(expected)
            },
        )
    }

    @TestFactory
    fun `should format up to decimals`() = listOf(
        "0.00000085" to "0.0000009",
        "0.000002" to "0.000002",
        "0.0000001" to "0.0000001",
        "0" to "0",
        "1" to "1",
        "10" to "10",
        "99999950000000000000000000000000000000000000000000" to "99999950000000000000000000000000000000000000000000",
        "100000050000000000000000000000000000000000000000000" to "100000050000000000000000000000000000000000000000000",
        "10000000000000000000000000000000000000000000" to "10000000000000000000000000000000000000000000",
    ).flatMap { (value, expected) ->
        listOf(
            dynamicTest("BigDecimal: $value -> $expected") {
                expectThat(BigDecimal.parseString(value).formatUpToDecimals(7)).isEqualTo(expected)
            },
            dynamicTest("Double: $value -> $expected") {
                expectThat(value.toDouble().formatUpToDecimals(7)).isEqualTo(expected)
            },
        )
    }
}
