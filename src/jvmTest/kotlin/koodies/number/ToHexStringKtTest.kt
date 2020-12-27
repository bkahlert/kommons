package koodies.number

import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class ToHexStringKtTest {

    @TestFactory
    fun `should convert to hex representation`() = listOf(
        0 to "0",
        10 to "A",
        15 to "F",
        16 to "10",
        65535 to "FFFF",
        65536 to "10000",
    ).map { (dec, hex) ->
        dynamicTest("$dec ≟ $hex") {
            expectThat(dec.toHexString()).isEqualTo(hex)
        }
    }

    @TestFactory
    fun `should convert to padded hex representation`() = listOf(
        0 to "00",
        10 to "0A",
        15 to "0F",
        16 to "10",
        65535 to "FFFF",
        65536 to "010000",
    ).map { (dec, hex) ->
        dynamicTest("$dec ≟ $hex") {
            expectThat(dec.toHexString(pad = true)).isEqualTo(hex)
        }
    }
}
