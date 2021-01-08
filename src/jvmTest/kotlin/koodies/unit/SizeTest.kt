package koodies.unit

import koodies.io.path.appendBytes
import koodies.io.path.randomFile
import koodies.io.path.writeText
import koodies.test.UniqueId
import koodies.test.withTempDir
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.message
import java.nio.file.Path
import kotlin.io.path.appendText

@Execution(CONCURRENT)
class SizeTest {

    @Test
    fun `should use decimal unit by default`() {
        expectThat(42.Mega.bytes.toString()).isEqualTo("42.0 MB")
    }

    @Nested
    inner class WithBinaryPrefix {

        @TestFactory
        fun `should format integer binary form`() = listOf(
            4_200_000.Yobi.bytes to "4.20e+6 YiB",
            42.Yobi.bytes to "42.0 YiB",
            42.Zebi.bytes to "42.0 ZiB",
            42.Exbi.bytes to "42.0 EiB",
            42.Pebi.bytes to "42.0 PiB",
            42.Tebi.bytes to "42.0 TiB",
            42.Gibi.bytes to "42.0 GiB",
            42.Mebi.bytes to "42.0 MiB",
            42.Kibi.bytes to "42.0 KiB",
            42.bytes to "42 B",
        ).map { (size: Size, expected: String) ->
            val actual = size.toString<BinaryPrefix>()
            val actualNegative = (-size).toString<BinaryPrefix>()
            dynamicTest("$expected == $actual ← ${size.bytes}") {
                expectThat(actual).isEqualTo(expected)
            } to dynamicTest("-$expected == $actualNegative ← ${(-size).bytes}") {
                expectThat(actualNegative).isEqualTo("-$expected")
            }
        }.unzip()
            .let { (pos, neg) ->
                listOf(
                    dynamicContainer("positive", pos),
                    dynamicContainer("negative", neg),
                )
            }


        @TestFactory
        fun `should format fraction binary form`() = listOf(
            4_200.Gibi.bytes to "4.10 TiB",
            420.Gibi.bytes to "420 GiB",
            42.Gibi.bytes to "42.0 GiB",
            4.2.Gibi.bytes to "4.20 GiB",
            .42.Gibi.bytes to "430 MiB",
            .042.Gibi.bytes to "43.0 MiB",
            .0042.Gibi.bytes to "4.30 MiB",
        ).map { (size: Size, expected: String) ->
            val actual = size.toString<BinaryPrefix>()
            val actualNegative = (-size).toString<BinaryPrefix>()
            dynamicTest("$expected == $actual ← ${size.bytes}") {
                expectThat(actual).isEqualTo(expected)
            } to dynamicTest("-$expected == $actualNegative ← ${(-size).bytes}") {
                expectThat(actualNegative).isEqualTo("-$expected")
            }
        }.unzip()
            .let { (pos, neg) ->
                listOf(
                    dynamicContainer("positive", pos),
                    dynamicContainer("negative", neg),
                )
            }

        @TestFactory
        fun `should format 0 binary form`() = listOf(
            0.Yobi.bytes to "0 B",
            0.Kibi.bytes to "0 B",
            0.bytes to "0 B",
        ).map { (size: Size, expected: String) ->
            val actual = size.toString<BinaryPrefix>()
            dynamicTest("$expected == $actual ← ${size.bytes}") {
                expectThat(actual).isEqualTo(expected)
            }
        }

        @TestFactory
        fun `should throw on yet unsupported prefixes`() = listOf(
            BinaryPrefix.mibi to { number: Int -> number.mibi },
            BinaryPrefix.mubi to { number: Int -> number.mubi },
            BinaryPrefix.nabi to { number: Int -> number.nabi },
            BinaryPrefix.pibi to { number: Int -> number.pibi },
            BinaryPrefix.fembi to { number: Int -> number.fembi },
            BinaryPrefix.abi to { number: Int -> number.abi },
            BinaryPrefix.zebi to { number: Int -> number.zebi_ },
            BinaryPrefix.yobi to { number: Int -> number.yobi_ },
        ).map { (prefix, factory) ->
            dynamicTest("$prefix") {
                expectCatching { factory(0) }
                    .isFailure()
                    .isA<IllegalArgumentException>().message.isEqualTo("Small $prefix are currently not fully supported!")
            }
        }

        @TestFactory
        fun `should format to specific unit`() = listOf(
            4_200_000.Yobi.bytes to "4.84e+24 MiB",
            42.Yobi.bytes to "4.84e+19 MiB",
            42.Zebi.bytes to "4.73e+16 MiB",
            42.Exbi.bytes to "46179488366592.0000 MiB",
            42.Pebi.bytes to "45097156608.0000 MiB",
            42.Tebi.bytes to "44040192.0000 MiB",
            42.Gibi.bytes to "43008.0000 MiB",
            42.Mebi.bytes to "42.0000 MiB",
            52.Kibi.bytes to "0.0508 MiB",
            42.Kibi.bytes to "0.0410 MiB",
            42.bytes to "0.0000 MiB",
        ).map { (size: Size, expected: String) ->
            val actual = size.toString(BinaryPrefix.Mebi, 4)
            dynamicTest("$expected == $actual ← ${size.bytes}") {
                expectThat(actual).isEqualTo(expected)
            }
        }
    }


    @Nested
    inner class WithDecimalPrefix {

        @TestFactory
        fun `should format integer decimal form`() = listOf(
            4_200_000.Yotta.bytes to "4.20e+6 YB",
            42.Yotta.bytes to "42.0 YB",
            42.Zetta.bytes to "42.0 ZB",
            42.Exa.bytes to "42.0 EB",
            42.Peta.bytes to "42.0 PB",
            42.Tera.bytes to "42.0 TB",
            42.Giga.bytes to "42.0 GB",
            42.Mega.bytes to "42.0 MB",
            42.kilo.bytes to "42.0 KB",
            42.bytes to "42 B",
        ).map { (size: Size, expected: String) ->
            val actual = size.toString<DecimalPrefix>()
            val actualNegative = (-size).toString<DecimalPrefix>()
            dynamicTest("$expected == $actual ← ${size.bytes}") {
                expectThat(actual).isEqualTo(expected)
            } to dynamicTest("-$expected == $actualNegative ← ${(-size).bytes}") {
                expectThat(actualNegative).isEqualTo("-$expected")
            }
        }.unzip()
            .let { (pos, neg) ->
                listOf(
                    dynamicContainer("positive", pos),
                    dynamicContainer("negative", neg),
                )
            }


        @TestFactory
        fun `should format fraction decimal form`() = listOf(
            4_200.Giga.bytes to "4.20 TB",
            420.Giga.bytes to "420 GB",
            42.Giga.bytes to "42.0 GB",
            4.2.Giga.bytes to "4.20 GB",
            .42.Giga.bytes to "420 MB",
            .042.Giga.bytes to "42.0 MB",
            .0042.Giga.bytes to "4.20 MB",
        ).map { (size: Size, expected: String) ->
            val actual = size.toString<DecimalPrefix>()
            val actualNegative = (-size).toString<DecimalPrefix>()
            dynamicTest("$expected == $actual ← ${size.bytes}") {
                expectThat(actual).isEqualTo(expected)
            } to dynamicTest("-$expected == $actualNegative ← ${(-size).bytes}") {
                expectThat(actualNegative).isEqualTo("-$expected")
            }
        }.unzip()
            .let { (pos, neg) ->
                listOf(
                    dynamicContainer("positive", pos),
                    dynamicContainer("negative", neg),
                )
            }

        @TestFactory
        fun `should format 0 decimal form`() = listOf(
            0.Yotta.bytes to "0 B",
            0.kilo.bytes to "0 B",
            0.bytes to "0 B",
        ).map { (size: Size, expected: String) ->
            val actual = "$size"
            dynamicTest("$expected == $actual ← ${size.bytes}") {
                expectThat(actual).isEqualTo(expected)
            }
        }

        @TestFactory
        fun `should throw on yet unsupported prefixes`() = listOf(
            DecimalPrefix.deci to { number: Int -> number.deci },
            DecimalPrefix.centi to { number: Int -> number.centi },
            DecimalPrefix.milli to { number: Int -> number.milli },
            DecimalPrefix.micro to { number: Int -> number.micro },
            DecimalPrefix.nano to { number: Int -> number.nano },
            DecimalPrefix.pico to { number: Int -> number.pico },
            DecimalPrefix.femto to { number: Int -> number.femto },
            DecimalPrefix.atto to { number: Int -> number.atto },
            DecimalPrefix.zepto to { number: Int -> number.zepto },
            DecimalPrefix.yocto to { number: Int -> number.yocto },
        ).map { (prefix, factory) ->
            dynamicTest("$prefix") {
                expectCatching { factory(0) }
                    .isFailure()
                    .isA<IllegalArgumentException>().message.isEqualTo("Small $prefix are currently not fully supported!")
            }
        }

        @TestFactory
        fun `should format to specific unit`() = listOf(
            4_200_000.Yotta.bytes to "4.20e+24 MB",
            42.Yotta.bytes to "4.20e+19 MB",
            42.Zetta.bytes to "4.20e+16 MB",
            42.Exa.bytes to "42000000000000.0000 MB",
            42.Peta.bytes to "42000000000.0000 MB",
            42.Tera.bytes to "42000000.0000 MB",
            42.Giga.bytes to "42000.0000 MB",
            42.Mega.bytes to "42.0000 MB",
            520.hecto.bytes to "0.0520 MB", // ⛳️
            420.hecto.bytes to "0.0420 MB", // 🌽
            52.kilo.bytes to "0.0520 MB",
            42.kilo.bytes to "0.0420 MB",
            42.bytes to "0.0000 MB",
        ).map { (size: Size, expected: String) ->
            val actual = size.toString(DecimalPrefix.Mega, 4)
            dynamicTest("$expected == $actual ← ${size.bytes}") {
                expectThat(actual).isEqualTo(expected)
            }
        }
    }


    @Nested
    inner class AsSize {

        private fun Path.createFile(): Path = randomFile().apply { repeat(2500) { appendText("1234567890") } }

        @Test
        fun `should format size human-readable (10^x)`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(createFile().size.toString<DecimalPrefix>()).isEqualTo("25.0 KB")
        }

        @Test
        fun `should format size human-readable (2^y)`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(createFile().size.toString<BinaryPrefix>()).isEqualTo("24.4 KiB")
        }
    }

    @Nested
    inner class Conversion {
        private val binFactor = BinaryPrefix.Kibi.factor
        private val decFactor = DecimalPrefix.kilo.factor

        @TestFactory
        fun `should format to specific unit`() = listOf(
            42.Yobi.bytes to 42.bytes * binFactor * binFactor * binFactor * binFactor * binFactor * binFactor * binFactor * binFactor,
            42.Zebi.bytes to 42.bytes * binFactor * binFactor * binFactor * binFactor * binFactor * binFactor * binFactor,
            42.Exbi.bytes to 42.bytes * binFactor * binFactor * binFactor * binFactor * binFactor * binFactor,
            42.Pebi.bytes to 42.bytes * binFactor * binFactor * binFactor * binFactor * binFactor,
            42.Tebi.bytes to 42.bytes * binFactor * binFactor * binFactor * binFactor,
            42.Gibi.bytes to 42.bytes * binFactor * binFactor * binFactor,
            42.Mebi.bytes to 42.bytes * binFactor * binFactor,
            42.Kibi.bytes to 42.bytes * binFactor,
            42.bytes to 42.bytes,

            42.Yotta.bytes to 42.bytes * decFactor * decFactor * decFactor * decFactor * decFactor * decFactor * decFactor * decFactor,
            42.Zetta.bytes to 42.bytes * decFactor * decFactor * decFactor * decFactor * decFactor * decFactor * decFactor,
            42.Exa.bytes to 42.bytes * decFactor * decFactor * decFactor * decFactor * decFactor * decFactor,
            42.Peta.bytes to 42.bytes * decFactor * decFactor * decFactor * decFactor * decFactor,
            42.Tera.bytes to 42.bytes * decFactor * decFactor * decFactor * decFactor,
            42.Giga.bytes to 42.bytes * decFactor * decFactor * decFactor,
            42.Mega.bytes to 42.bytes * decFactor * decFactor,
            42.kilo.bytes to 42.bytes * decFactor,
            42.hecto.bytes to 42.bytes * 10 * 10, // ⛳️
            42.deca.bytes to 42.bytes * 10, // 🌽
            42.bytes to 42.bytes,
        ).flatMap { (decimalSize: Size, binarySize: Size) ->
            listOf(
                dynamicTest("$decimalSize == $binarySize") {
                    expectThat(decimalSize).isEqualTo(binarySize)
                },
                dynamicTest("${decimalSize.bytes} == ${binarySize.bytes}") {
                    expectThat(decimalSize.bytes).isEqualTo(binarySize.bytes)
                },
            )
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
    inner class FileSize {

        private fun Path.getSmall() = randomFile("small").writeText("123")
        private fun Path.getMedium() = randomFile("medium").writeText("123456")
        private fun Path.getLarge() = randomFile("large").appendBytes(ByteArray(3_123_456))

        @Test
        fun `should compare files by size`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val largeFile = getLarge()
            val smallFile = getSmall()
            val mediumFile = getMedium()
            expectThat(listOf(largeFile, smallFile, mediumFile).sortedWith(Size.FileSizeComparator)).containsExactly(smallFile, mediumFile, largeFile)
        }

        @Test
        fun `should have size`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(getLarge().size).isEqualTo(3_123_456.bytes)
        }

        @Test
        fun `should have rounded size`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(getLarge().roundedSize).isEqualTo(3_000_000.bytes)
        }
    }
}

val Assertion.Builder<out Path>.size get() = get { size }

fun <T : Path> Assertion.Builder<T>.hasSize(size: Size) =
    assert("has $size") {
        val actualSize = it.size
        when (actualSize == size) {
            true -> pass()
            else -> fail("was $actualSize (${actualSize.bytes} B; Δ: ${actualSize - size})")
        }
    }
