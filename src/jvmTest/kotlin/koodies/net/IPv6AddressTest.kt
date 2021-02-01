package koodies.net

import koodies.collections.to
import koodies.net.IPv4Subnet.Companion.smallestCommonSubnet
import koodies.net.IPv6Notation.compressedRepresentation
import koodies.net.IPv6Subnet.Companion.div
import koodies.net.IPv6Subnet.Companion.smallestCommonSubnet
import koodies.number.ubyteArrayOfDecimalString
import koodies.test.isFailure
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class IPv6AddressTest {

    @TestFactory
    fun `should be instantiatable`() =
        listOf(
            "::ffff:c0a8:1001".toIp(),
            ipOf("0:0:0::ffff:c0a8:1001"),
            IPv6Address.parse("0:0:0:0:0:ffff:c0a8:1001"),
            IPv6Address.parse("0:0:0:0:0:ffff:192.168.16.1"),
            IPv6Address(ubyteArrayOfDecimalString("281473913982977")),
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
            expectThat(range.smallestCommonSubnet).toStringIsEqualTo("::ffff:c0a8:1000/126")
        }

        @Test
        fun `should have usable`() {
            expectThat(range.smallestCommonSubnet.usable).isEqualTo(IPv6Address.parse("::ffff:c0a8:1001")..IPv6Address.parse("::ffff:c0a8:1002"))
        }

        @Test
        fun `should have firstUsableHost`() {
            expectThat(range.smallestCommonSubnet.firstUsableHost).isEqualTo(IPv6Address.parse("::ffff:c0a8:1001"))
        }

        @Test
        fun `should have lastUsableHost`() {
            expectThat(range.smallestCommonSubnet.lastUsableHost).isEqualTo(IPv6Address.parse("::ffff:c0a8:1002"))
        }

        @Test
        fun `should serialize to string`() {
            expectThat(range).toStringIsEqualTo("::ffff:c0a8:1001..::ffff:c0a8:1003")
        }
    }

    @Test
    fun `should create subnet`() {
        val ip = IPv6Address.parse("::ffff:10.55.0.2")
        val cidr = 125
        expectThat(ip / cidr).isEqualTo(IPv6Subnet.from(ip, cidr))
    }

    @TestFactory
    fun `should contain range`() = listOf(
        IPv6Address.RANGE to (0 to IPv6Address.MAX_VALUE + 1 to IPv6Address.RANGE.start),
        DefaultIPv4toIPv6Mapping.range to (96 to IPv4Address.RANGE.smallestCommonSubnet.hostCount to IPv6Address.parse("::ffff:0:0")),
        Nat64IPv4toIPv6Mapping.range to (96 to IPv4Address.RANGE.smallestCommonSubnet.hostCount to IPv6Address.parse("64:ff9b::")),
    ).testEach { (range, expected) ->
        val (bitCount, hostCount, networkAddress) = expected
        with { range.toString() }.then {
            expect { range.smallestCommonSubnet.prefixLength }.that { isEqualTo(bitCount) }
            expect { range.smallestCommonSubnet.hostCount }.that { isEqualTo(hostCount) }
            expect { range.smallestCommonSubnet.networkAddress }.that { isEqualTo(networkAddress) }
        }
    }
}
