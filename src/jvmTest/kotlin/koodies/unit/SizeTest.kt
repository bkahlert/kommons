package koodies.unit

import koodies.io.path.getSize
import koodies.io.path.randomFile
import koodies.test.UniqueId
import koodies.test.testEach
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.math.BigInteger
import java.nio.file.Path
import kotlin.io.path.appendText

class SizeTest {

    @TestFactory
    fun `should return whole bytes`() = testEach(2.bytes, 1.1.bytes) { size ->
        expecting { size.wholeBytes } that { isEqualTo(BigInteger.TWO) }
    }

    @Test
    fun `should use decimal unit by default`() {
        expectThat(42.Mega.bytes.toString()).isEqualTo("42 MB")
    }

    @TestFactory
    fun `max length of representation to base`() = testEach(
        1.bytes to listOf(2 to 8, 8 to 3, 10 to 3, 16 to 2, 32 to 2),
        2.bytes to listOf(2 to 16, 8 to 6, 10 to 5, 16 to 4, 32 to 4),
        4.bytes to listOf(2 to 32, 8 to 11, 10 to 10, 16 to 8, 32 to 7),
    ) { (numBytes, expectedMaxLengths) ->
        expectedMaxLengths.forEach { (base, expectedMaxLength) ->
            expecting("max length of number encoded with $numBytes represented as string to the base $base")
            { numBytes.maxLengthOfRepresentationToBaseOf(base) } that { isEqualTo(expectedMaxLength) }
        }
    }

    @Nested
    inner class WithBinaryPrefix {

        @TestFactory
        fun `should format integer binary form`() = testEach(
            4_200_000.Yobi.bytes to "4.20e+6 YiB",
            42.Yobi.bytes to "42 YiB",
            42.Zebi.bytes to "42 ZiB",
            42.Exbi.bytes to "42 EiB",
            42.Pebi.bytes to "42 PiB",
            42.Tebi.bytes to "42 TiB",
            42.Gibi.bytes to "42 GiB",
            42.Mebi.bytes to "42 MiB",
            42.Kibi.bytes to "42 KiB",
            42.bytes to "42 B",
            42.mibi.bytes to "42 miB",
            42.mubi.bytes to "42 uiB",
            42.nabi.bytes to "42 niB",
            42.pibi.bytes to "42 piB",
            42.fembi.bytes to "42 fiB",
            42.abi.bytes to "42 aiB",
            42.zebi.bytes to "42 ZiB",
            42.yobi.bytes to "42 YiB",
        ) { (size: Size, expected: String) ->
            expecting { size.toString<BinaryPrefix>() } that { isEqualTo(expected) }
            expecting { (-size).toString<BinaryPrefix>() } that { isEqualTo("-$expected") }
        }

        @TestFactory
        fun `should format fraction binary form`() = testEach(
            4_200_000.Gibi.bytes to "4.01 PiB",
            4_200.Gibi.bytes to "4.1 TiB",
            420.Gibi.bytes to "420 GiB",
            42.Gibi.bytes to "42 GiB",
            4.2.Gibi.bytes to "4.2 GiB",
            .42.Gibi.bytes to "430 MiB",
            .042.Gibi.bytes to "43 MiB",
            .0042.Gibi.bytes to "4.3 MiB",
            .00042.Gibi.bytes to "440 KiB",
            .000042.Gibi.bytes to "44 KiB",
            .0000042.Gibi.bytes to "4.4 KiB",
            .00000042.Gibi.bytes to "451 B",
            .000000042.Gibi.bytes to "45 B",
            .0000000042.Gibi.bytes to "5 B",
            .00000000042.Gibi.bytes to "462 miB",
            .000000000042.Gibi.bytes to "46.2 miB",
            .0000000000042.Gibi.bytes to "4.62 miB",
            .00000000000042.Gibi.bytes to "473 uiB",
        ) { (size: Size, expected: String) ->
            expecting { size.toString<BinaryPrefix>() } that { isEqualTo(expected) }
            expecting { (-size).toString<BinaryPrefix>() } that { isEqualTo("-$expected") }
        }

        @TestFactory
        fun `should format 0 binary form`() = testEach(
            0.Yobi.bytes to "0 B",
            0.Kibi.bytes to "0 B",
            0.bytes to "0 B",
        ) { (size: Size, expected: String) ->
            expecting { size.toString<BinaryPrefix>() } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should format to specific unit`() = testEach(
            4_200_000.Yobi.bytes to "5.99e+51 ZiB",
            42.Yobi.bytes to "5.99e+46 ZiB",
            42.Zebi.bytes to "5.85e+43 ZiB",
            42.Exbi.bytes to "5.72e+40 ZiB",
            42.Pebi.bytes to "5.58e+37 ZiB",
            42.Tebi.bytes to "5.45e+34 ZiB",
            42.Gibi.bytes to "5.32e+31 ZiB",
            42.Mebi.bytes to "5.20e+28 ZiB",
            42.Kibi.bytes to "5.08e+25 ZiB",
            42.bytes to "4.96e+22 ZiB",
            42.mibi.bytes to "4.84e+19 ZiB",
            42.mubi.bytes to "4.73e+16 ZiB",
            42.nabi.bytes to "46179488365824.5 ZiB",
            42.pibi.bytes to "45097156607.9986 ZiB",
            42.fembi.bytes to "44040192 ZiB",
            42.abi.bytes to "43008 ZiB",
            42.zebi.bytes to "42 ZiB",
            42.yobi.bytes to "0.041 ZiB",
        ) { (size: Size, expected: String) ->
            expecting { size.toString(BinaryPrefix.zebi, 4) } that { isEqualTo(expected) }
        }
    }

    @Nested
    inner class WithDecimalPrefix {

        @TestFactory
        fun `should format integer decimal form`() = testEach(
            4_200_000.Yotta.bytes to "4.20e+6 YB",
            42.Yotta.bytes to "42 YB",
            42.Zetta.bytes to "42 ZB",
            42.Exa.bytes to "42 EB",
            42.Peta.bytes to "42 PB",
            42.Tera.bytes to "42 TB",
            42.Giga.bytes to "42 GB",
            42.Mega.bytes to "42 MB",
            42.kilo.bytes to "42 KB",
            42.hecto.bytes to "4.2 KB",
            42.deca.bytes to "420 B",
            42.bytes to "42 B",
            42.deci.bytes to "4 B",
            42.centi.bytes to "420 mB",
            42.milli.bytes to "42 mB",
            42.micro.bytes to "42 μB",
            42.nano.bytes to "42 nB",
            42.pico.bytes to "42 pB",
            42.femto.bytes to "42 fB",
            42.atto.bytes to "42 aB",
            42.zepto.bytes to "42 zB",
            42.yocto.bytes to "42 yB",
        ) { (size: Size, expected: String) ->
            expecting { size.toString<DecimalPrefix>() } that { isEqualTo(expected) }
            expecting { (-size).toString<DecimalPrefix>() } that { isEqualTo("-$expected") }
        }

        @TestFactory
        fun `should format fraction decimal form`() = testEach(
            4_200.Giga.bytes to "4.2 TB",
            420.Giga.bytes to "420 GB",
            42.Giga.bytes to "42 GB",
            4.2.Giga.bytes to "4.2 GB",
            .42.Giga.bytes to "420 MB",
            .042.Giga.bytes to "42 MB",
            .0042.Giga.bytes to "4.2 MB",
            .00042.Giga.bytes to "420 KB",
            .000042.Giga.bytes to "42 KB",
            .0000042.Giga.bytes to "4.2 KB",
            .00000042.Giga.bytes to "420 B",
            .000000042.Giga.bytes to "42 B",
            .0000000042.Giga.bytes to "4 B",
            .00000000042.Giga.bytes to "420 mB",
            .000000000042.Giga.bytes to "42 mB",
            .0000000000042.Giga.bytes to "4.2 mB",
            .00000000000042.Giga.bytes to "420 μB",
        ) { (size: Size, expected: String) ->
            expecting { size.toString<DecimalPrefix>() } that { isEqualTo(expected) }
            expecting { (-size).toString<DecimalPrefix>() } that { isEqualTo("-$expected") }
        }

        @TestFactory
        fun `should format 0 decimal form`() = testEach(
            0.Yotta.bytes to "0 B",
            0.kilo.bytes to "0 B",
            0.bytes to "0 B",
        ) { (size: Size, expected: String) ->
            expecting { "$size" } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should format to specific unit`() = testEach(
            4_200_000.Yotta.bytes to "4.20e+51 zB",
            42.Yotta.bytes to "4.20e+46 zB",
            42.Zetta.bytes to "4.20e+43 zB",
            42.Exa.bytes to "4.20e+40 zB",
            42.Peta.bytes to "4.20e+37 zB",
            42.Tera.bytes to "4.20e+34 zB",
            42.Giga.bytes to "4.20e+31 zB",
            42.Mega.bytes to "4.20e+28 zB",
            42.kilo.bytes to "4.20e+25 zB",
            42.hecto.bytes to "4.20e+24 zB",
            42.deca.bytes to "4.20e+23 zB",
            42.bytes to "4.20e+22 zB",
            42.deci.bytes to "4.20e+21 zB",
            42.centi.bytes to "4.20e+20 zB",
            42.milli.bytes to "4.20e+19 zB",
            42.micro.bytes to "4.20e+16 zB",
            42.nano.bytes to "42000000000000 zB",
            42.pico.bytes to "42000000000 zB",
            42.femto.bytes to "42000000 zB",
            42.atto.bytes to "42000 zB",
            42.zepto.bytes to "42 zB",
            42.yocto.bytes to "0.042 zB",
        ) { (size: Size, expected: String) ->
            expecting { size.toString(DecimalPrefix.zepto, 4) } that { isEqualTo(expected) }
        }
    }


    @Nested
    inner class AsSize {

        private fun Path.createFile(): Path = randomFile().apply { repeat(2500) { appendText("1234567890") } }

        @Test
        fun `should format size human-readable (10^x)`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(createFile().getSize().toString<DecimalPrefix>()).isEqualTo("25 KB")
        }

        @Test
        fun `should format size human-readable (2^y)`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(createFile().getSize().toString<BinaryPrefix>()).isEqualTo("24.4 KiB")
        }
    }

    @Nested
    inner class Parsing {

        @Nested
        inner class WithBinaryPrefix {

            @Test
            fun `should parse integer with no spacing`() {
                expectThat("2GiB".toSize()).isEqualTo(2.Gibi.bytes)
            }

            @Test
            fun `should parse integer with spacing`() {
                expectThat("2 GiB".toSize()).isEqualTo(2.Gibi.bytes)
            }

            @Test
            fun `should parse fractional with no spacing`() {
                expectThat("2.505GiB".toSize()).isEqualTo(2.505.Gibi.bytes)
            }

            @Test
            fun `should parse fractional with spacing`() {
                expectThat("2.505 GiB".toSize()).isEqualTo(2.505.Gibi.bytes)
            }

            @Test
            fun `should parse three-letter KiB`() {
                expectThat("2 KiB".toSize()).isEqualTo(2.Kibi.bytes)
            }

            @Test
            fun `should parse upper-case kilo byte`() {
                expectThat("2 KB".toSize()).isEqualTo(2.Kibi.bytes)
            }
        }

        @Nested
        inner class WithDecimalPrefix {
            @Test
            fun `should parse integer with no spacing`() {
                expectThat("2GB".toSize()).isEqualTo(2.Giga.bytes)
            }

            @Test
            fun `should parse integer with spacing`() {
                expectThat("2 GB".toSize()).isEqualTo(2.Giga.bytes)
            }

            @Test
            fun `should parse fractional with no spacing`() {
                expectThat("2.505GB".toSize()).isEqualTo(2.505.Giga.bytes)
            }

            @Test
            fun `should parse fractional with spacing`() {
                expectThat("2.505 GB".toSize()).isEqualTo(2.505.Giga.bytes)
            }

            @Test
            fun `should parse lower-case kilo byte`() {
                expectThat("2 kB".toSize()).isEqualTo(2.kilo.bytes)
            }
        }

        @Nested
        inner class WithNoPrefix {
            @Test
            fun `should parse integer with no spacing`() {
                expectThat("2B".toSize()).isEqualTo(2.bytes)
            }

            @Test
            fun `should parse integer with spacing`() {
                expectThat("2 B".toSize()).isEqualTo(2.bytes)
            }

            @Test
            fun `should parse fractional with no spacing`() {
                expectThat("2.505B".toSize()).isEqualTo(2.505.bytes)
            }

            @Test
            fun `should parse fractional with spacing`() {
                expectThat("2.505 B".toSize()).isEqualTo(2.505.bytes)
            }
        }

        @Nested
        inner class WithNoUnit {

            @Test
            fun `should parse integer with no spacing`() {
                expectThat("2".toSize()).isEqualTo(2.bytes)
            }

            @Test
            fun `should parse fractional with no spacing`() {
                expectThat("2.505".toSize()).isEqualTo(2.505.bytes)
            }
        }
    }

    @Nested
    inner class SumBy {
        @Test
        fun `should sum by`() {
            expectThat(sequenceOf(1.bytes, 2.kilo.bytes, 3.Mebi.bytes).sumBy { it })
                .isEqualTo(1.bytes + 2.kilo.bytes + 3.Mebi.bytes)
        }
    }
}

val Assertion.Builder<out Path>.size get() = get { getSize() }

fun <T : Path> Assertion.Builder<T>.hasSize(size: Size) =
    assert("has $size") {
        val actualSize = it.getSize()
        when (actualSize == size) {
            true -> pass()
            else -> fail("was $actualSize (${actualSize.bytes} B; Δ: ${actualSize - size})")
        }
    }
