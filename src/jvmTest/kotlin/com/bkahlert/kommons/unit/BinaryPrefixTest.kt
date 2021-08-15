package com.bkahlert.kommons.unit

import com.bkahlert.kommons.math.BigDecimal
import com.bkahlert.kommons.test.testEach
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isEqualTo

class BinaryPrefixTest {

    @TestFactory
    fun `should format integer binary form`() = testEach(
        4_200_000.Yobi to "5077488442381442533765939200000",
        4_200.Yobi to "5077488442381442533765939200",
        42.Yobi to "50774884423814425337659392",
        42.Zebi to "49584848070131274743808",
        42.Exbi to "48422703193487572992",
        42.Pebi to "47287796087390208",
        42.Tebi to "46179488366592",
        42.Gibi to "45097156608",
        42.Mebi to "44040192",
        42.Kibi to "43008",
        42.mibi to "0.0410172",
        42.mubi to "0.0000400543206",
        42.nabi to "0.0000000391155481332",
        42.pibi to "0.0000000000381987774744618",
        42.fembi to "0.0000000000000373034936274052584",
        42.abi to "0.0000000000000000364291929955129489824",
        42.zebi to "0.0000000000000000000355753837846806142408686",
        42.yobi to "0.0000000000000000000000347415857272271623445991654",
    ) { (value: BigDecimal, expected: String) ->
        expecting { value.toPlainString() } that { isEqualTo(expected) }
    }

    @TestFactory
    fun `should format fraction binary form`() = testEach(
        4_200_000.Gibi to "4509715660800000",
        4_200.Gibi to "4509715660800",
        420.Gibi to "450971566080",
        42.Gibi to "45097156608",
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
