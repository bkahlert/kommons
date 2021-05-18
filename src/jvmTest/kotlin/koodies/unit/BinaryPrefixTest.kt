package koodies.unit

import koodies.math.BigDecimal
import koodies.test.testEach
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isEqualTo

class BinaryPrefixTest {

    @TestFactory
    fun `should format integer binary form`() = testEach(
        4_200_000.Yobi to "5077488442381442533765939200000.0",
        4_200.Yobi to "5077488442381442533765939200.0",
        42.Yobi to "50774884423814425337659392.0",
        42.Zebi to "49584848070131274743808.0",
        42.Exbi to "48422703193487572992.0",
        42.Pebi to "47287796087390208.0",
        42.Tebi to "46179488366592.0",
        42.Gibi to "45097156608.0",
        42.Mebi to "44040192.0",
        42.Kibi to "43008.0",
        42.mibi to "0.04101720",
        42.mubi to "0.00004005432060",
        42.nabi to "0.00000003911554813320",
        42.pibi to "0.00000000003819877747446180",
        42.fembi to "0.00000000000003730349362740525840",
        42.abi to "0.00000000000000003642919299551294898240",
        42.zebi to "0.00000000000000000003557538378468061424086860",
        42.yobi to "0.00000000000000000000003474158572722716234459916540",
    ) { (value: BigDecimal, expected: String) ->
        expecting { value.toPlainString() } that { isEqualTo(expected) }
    }

    @TestFactory
    fun `should format fraction binary form`() = testEach(
        4_200_000.Gibi to "4509715660800000.0",
        4_200.Gibi to "4509715660800.0",
        420.Gibi to "450971566080.0",
        42.Gibi to "45097156608.0",
        4.2.Gibi to "4509715660.8",
        .42.Gibi to "450971566.08",
        .042.Gibi to "45097156.608",
        .0042.Gibi to "4509715.6608",
        .00042.Gibi to "450971.56608",
        .000042.Gibi to "45097.156608",
        .0000042.Gibi to "4509.7156608",
        .00000042.Gibi to "450.97156608",
        .000000042.Gibi to "45.097156608",
        .0000000042.Gibi to "4.5097156608",
        .00000000042.Gibi to "0.45097156608",
        .000000000042.Gibi to "0.045097156608",
        .0000000000042.Gibi to "0.0045097156608",
        .00000000000042.Gibi to "0.00045097156608",
    ) { (value: BigDecimal, expected: String) ->
        expecting { value.toPlainString() } that { isEqualTo(expected) }
    }
}
