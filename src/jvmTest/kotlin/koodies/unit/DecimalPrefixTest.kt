package koodies.unit

import koodies.math.BigDecimal
import koodies.test.testEach
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isEqualTo

class DecimalPrefixTest {

    @TestFactory
    fun `should format integer decimal form`() = testEach(
        4_200_000.Yotta to "4200000000000000000000000000000",
        4_200.Yotta to "4200000000000000000000000000",
        42.Yotta to "42000000000000000000000000",
        42.Zetta to "42000000000000000000000",
        42.Exa to "42000000000000000000",
        42.Peta to "42000000000000000",
        42.Tera to "42000000000000",
        42.Giga to "42000000000",
        42.Mega to "42000000",
        42.kilo to "42000",
        42.hecto to "4200",
        42.deca to "420",
        42.deci to "4.2",
        42.centi to "0.42",
        42.milli to "0.042",
        42.micro to "0.000042",
        42.nano to "0.000000042",
        42.pico to "0.000000000042",
        42.femto to "0.000000000000042",
        42.atto to "0.000000000000000042",
        42.zepto to "0.000000000000000000042",
        42.yocto to "0.000000000000000000000042",
    ) { (value: BigDecimal, expected: String) ->
        expecting { value.toPlainString() } that { isEqualTo(expected) }
    }

    @TestFactory
    fun `should format fraction decimal form`() = testEach(
        4_200_000.Giga to "4200000000000000",
        4_200.Giga to "4200000000000",
        420.Giga to "420000000000",
        42.Giga to "42000000000",
        4.2.Giga to "4200000000.0",
        .42.Giga to "420000000.00",
        .042.Giga to "42000000.000",
        .0042.Giga to "4200000.0000",
        .00042.Giga to "420000.00000",
        .000042.Giga to "42000.000000",
        .0000042.Giga to "4200.0000000",
        .00000042.Giga to "420.00000000",
        .000000042.Giga to "42.000000000",
        .0000000042.Giga to "4.2000000000",
        .00000000042.Giga to "0.42000000000",
        .000000000042.Giga to "0.042000000000",
        .0000000000042.Giga to "0.0042000000000",
        .00000000000042.Giga to "0.00042000000000",
    ) { (value: BigDecimal, expected: String) ->
        expecting { value.toPlainString() } that { isEqualTo(expected) }
    }
}
