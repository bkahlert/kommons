package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger
import koodies.collections.to
import koodies.test.isFailure
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class IPv6AddressTest {

    @TestFactory
    fun `should be instantiatable`() =
        listOf(
            "::ffff:c0a8:1001".toIp(),
            ipOf("0:0:0::ffff:c0a8:1001"),
            IPv6Address.parse("0:0:0:0:0:ffff:c0a8:1001"),
            IPv6Address.parse("0:0:0:0:0:ffff:192.168.16.1"),
            IPv6Address(BigInteger.parseString("281473913982977", 10)),
        ).testEach { ip ->
            expect { ip }.that { toStringIsEqualTo("::ffff:c0a8:1001") }
        }

    @TestFactory
    fun `should throw on invalid value`() =
        listOf(
            { "-0:0:0::ffff:c0a8:1001".toIp() },
            { ipOf("0:::0::ffff:c0a8:1001") },
            { ipOf("0:0:0::ffffffff:c0a8:1001") },
            { IPv6Address.parse("0:0:0:0:0:ffff:c0a8:1001:0:0:0") },
            { IPv6Address.parse("0:0:0::xxxx:c0a8:1001") },
        ).testEach { ip ->
            expectThrowing { ip() }.that { isFailure<IllegalArgumentException>() }
        }

    @Nested
    inner class Representation {
        private val ip = IPv6Address.parse("::ffff:c0a8:1001")

        @Test
        fun `should serialize using compressed representation`() {
            expectThat(ip).toStringIsEqualTo(ip.compressedRepresentation)
        }

        @Nested
        inner class FullRepresentation {

            @Test
            fun `should provide full representation`() {
                expectThat(ip.fullRepresentation).isEqualTo("0000:0000:0000:0000:0000:ffff:c0a8:1001")
            }

            @Test
            fun `should fully represent first address`() {
                expectThat(IPv6Address.DEFAULT_ROOT.fullRepresentation).isEqualTo("0000:0000:0000:0000:0000:0000:0000:0000")
            }

            @Test
            fun `should fully represent loopback address`() {
                expectThat(IPv6Address.LOOPBACK.fullRepresentation).isEqualTo("0000:0000:0000:0000:0000:0000:0000:0001")
            }

            @TestFactory
            fun `should fully represent network address`() = listOf(
                IPv6Address.IPv4_MAPPED_RANGE to "0000:0000:0000:0000:0000:ffff:0000:0000",
                IPv6Address.IPv4_NAT64_MAPPED_RANGE to "0064:ff9b:0000:0000:0000:0000:0000:0000",
            ).testEach { (range, expected) ->
                expect { range.subnet.networkAddress.fullRepresentation }.that { isEqualTo(expected) }
            }
        }

        @Nested
        inner class ConventionalRepresentation {

            @Test
            fun `should provide conventional representation`() {
                expectThat(ip.conventionalRepresentation).isEqualTo("0:0:0:0:0:ffff:c0a8:1001")
            }

            @Test
            fun `should conventionally represent first address`() {
                expectThat(IPv6Address.DEFAULT_ROOT.conventionalRepresentation).isEqualTo("0:0:0:0:0:0:0:0")
            }

            @Test
            fun `should conventionally represent loopback address`() {
                expectThat(IPv6Address.LOOPBACK.conventionalRepresentation).isEqualTo("0:0:0:0:0:0:0:1")
            }

            @TestFactory
            fun `should conventionally represent network address`() = listOf(
                IPv6Address.IPv4_MAPPED_RANGE to "0:0:0:0:0:ffff:0:0",
                IPv6Address.IPv4_NAT64_MAPPED_RANGE to "64:ff9b:0:0:0:0:0:0",
            ).testEach { (range, expected) ->
                expect { range.subnet.networkAddress.conventionalRepresentation }.that { isEqualTo(expected) }
            }
        }

        @Nested
        inner class CompressedRepresentation {

            @Test
            fun `should provide compressed representation`() {
                expectThat(ip.compressedRepresentation).isEqualTo("::ffff:c0a8:1001")
            }

            @Test
            fun `should represent compressed first address`() {
                expectThat(IPv6Address.DEFAULT_ROOT.compressedRepresentation).isEqualTo("::")
            }

            @Test
            fun `should represent compressed loopback address`() {
                expectThat(IPv6Address.LOOPBACK.compressedRepresentation).isEqualTo("::1")
            }

            @TestFactory
            fun `should represent compressed network address`() = listOf(
                IPv6Address.IPv4_MAPPED_RANGE to "::ffff:0:0",
                IPv6Address.IPv4_NAT64_MAPPED_RANGE to "64:ff9b::",
            ).testEach { (range, expected) ->
                expect { range.subnet.networkAddress.compressedRepresentation }.that { isEqualTo(expected) }
            }
        }
    }


    @Nested
    inner class Range {

        private val range = IPv6Address.parse("0:0:0:0:0:ffff:c0a8:1001")..IPv6Address.parse("0:0:0:0:0:ffff:c0a8:1003")

        @Test
        fun `should have start`() {
            expectThat(range.start).isEqualTo(IPv6Address.parse("0:0:0:0:0:ffff:c0a8:1001"))
        }

        @Test
        fun `should have endInclusive`() {
            expectThat(range.endInclusive).isEqualTo(IPv6Address.parse("0:0:0:0:0:ffff:c0a8:1003"))
        }

        @Test
        fun `should have contain IP`() {
            expectThat(range).contains(IPv6Address.parse("0:0:0:0:0:ffff:c0a8:1002"))
        }

        @Test
        fun `should have not contain IP`() {
            expectThat(range).not { contains(IPv6Address.parse("0:0:0:0:0:ffff:c0a8:1004")) }
        }

        @Test
        fun `should return empty range if greater start than end`() {
            expectThat(range.endInclusive..range.start).isEmpty()
        }

        @Test
        fun `should have subnet`() {
            expectThat(range.subnet).toStringIsEqualTo("::ffff:c0a8:1000/126")
        }

        @Test
        fun `should have usable`() {
            expectThat(range.usable).isEqualTo(IPv6Address.parse("::ffff:c0a8:1001")..IPv6Address.parse("::ffff:c0a8:1002"))
        }

        @Test
        fun `should have firstUsableHost`() {
            expectThat(range.firstUsableHost).isEqualTo(IPv6Address.parse("::ffff:c0a8:1001"))
        }

        @Test
        fun `should have lastUsableHost`() {
            expectThat(range.lastUsableHost).isEqualTo(IPv6Address.parse("::ffff:c0a8:1002"))
        }

        @Test
        fun `should serialize to string`() {
            expectThat(range).toStringIsEqualTo("::ffff:c0a8:1001..::ffff:c0a8:1003")
        }
    }

    @Nested
    inner class Subnet {

        private val range = IPv6Address.parse("::ffff:10.55.0.2")..IPv6Address.parse("::ffff:10.55.0.6")
        private val subnet = range.subnet

        @Test
        fun `should have subnetBitCount`() {
            expectThat(subnet.bitCount).isEqualTo(125)
        }

        @Test
        fun `should have wildcardBitCount`() {
            expectThat(subnet.wildcardBitCount).isEqualTo(3)
        }

        @Test
        fun `should have hostCount`() {
            expectThat(subnet.hostCount).isEqualTo(BigInteger.fromInt(8))
        }

        @Test
        fun `should have usableHostCount`() {
            expectThat(subnet.usableHostCount).isEqualTo(BigInteger.fromInt(6))
        }

        @Test
        fun `should have networkAddress`() {
            expectThat(subnet.networkAddress).isEqualTo(IPv6Address.parse("::ffff:10.55.0.0"))
        }

        @Test
        fun `should have broadcastAddress`() {
            expectThat(subnet.broadcastAddress).isEqualTo(IPv6Address.parse("::ffff:10.55.0.7"))
        }

        @Test
        fun `should have firstHost`() {
            expectThat(subnet.firstHost).isEqualTo(IPv6Address.parse("::ffff:10.55.0.1"))
        }

        @Test
        fun `should have lastHost`() {
            expectThat(subnet.lastHost).isEqualTo(IPv6Address.parse("::ffff:10.55.0.6"))
        }

        @Test
        fun `should have subnetMask`() {
            expectThat(subnet.mask).toStringIsEqualTo("ffff:ffff:ffff:ffff:ffff:ffff:ffff:fff8")
        }

        @Test
        fun `should serialize to string`() {
            expectThat(subnet).toStringIsEqualTo("::ffff:a37:0/125")
        }
    }

    @TestFactory
    fun `should contain range`() = listOf(
        IPv6Address.RANGE to (0 to BigInteger.parseString("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16) + 1 to IPv6Address.RANGE.start),
        IPv6Address.IPv4_MAPPED_RANGE to (96 to IPv4Address.RANGE.subnet.hostCount to IPv6Address.parse("::ffff:0:0")),
        IPv6Address.IPv4_NAT64_MAPPED_RANGE to (96 to IPv4Address.RANGE.subnet.hostCount to IPv6Address.parse("64:ff9b::")),
    ).testEach { (range, expected) ->
        val (bitCount, hostCount, networkAddress) = expected
        with { range.toString() }.then {
            expect { range.subnet.bitCount }.that { isEqualTo(bitCount) }
            expect { range.subnet.hostCount }.that { isEqualTo(hostCount) }
            expect { range.subnet.networkAddress }.that { isEqualTo(networkAddress) }
        }
    }
}
