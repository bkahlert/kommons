package koodies.net

import koodies.collections.to
import koodies.math.ubyteArrayOfDecimalString
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

@Execution(SAME_THREAD)
class IPv6AddressTest {

    @TestFactory
    fun `should be instantiatable`() =
        listOf(
            "::ffff:c0a8:1001".toIP(),
            ipOf("0:0:0::ffff:c0a8:1001"),
            IPv6Address.parse("0:0:0:0:0:ffff:c0a8:1001"),
            IPv6Address.parse("0:0:0:0:0:ffff:192.168.16.1"),
            IPv6Address(ubyteArrayOfDecimalString("281473913982977")),
        ).testEach { ip ->
            expecting { ip } that { toStringIsEqualTo("::ffff:c0a8:1001") }
        }

    @TestFactory
    fun `should throw on invalid value`() =
        listOf(
            { "-0:0:0::ffff:c0a8:1001".toIP() },
            { ipOf("0:::0::ffff:c0a8:1001") },
            { ipOf("0:0:0::ffffffff:c0a8:1001") },
            { IPv6Address.parse("0:0:0:0:0:ffff:c0a8:1001:0:0:0") },
            { IPv6Address.parse("0:0:0::xxxx:c0a8:1001") },
        ).testEach { ip ->
            expectThrows<IllegalArgumentException> { ip() }
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
        fun `should throw on empty range if greater start than end`() {
            expectCatching { range.endInclusive..range.start }.isFailure().isA<IllegalArgumentException>()
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
        IPv6Address.RANGE to (0 to IPv6Address.MAX_VALUE.inc() to IPv6Address.RANGE.start),
        DefaultIPv4toIPv6Mapping.range to (96 to IPv4Address.RANGE.smallestCommonSubnet.hostCount to IPv6Address.parse("::ffff:0:0")),
        Nat64IPv4toIPv6Mapping.range to (96 to IPv4Address.RANGE.smallestCommonSubnet.hostCount to IPv6Address.parse("64:ff9b::")),
    ).testEach { (range, expected) ->
        val (bitCount, hostCount, networkAddress) = expected
        with { range.toString() }.then {
            expecting { range.smallestCommonSubnet.prefixLength } that { isEqualTo(bitCount) }
            expecting { range.smallestCommonSubnet.hostCount } that { isEqualTo(hostCount) }
            expecting { range.smallestCommonSubnet.networkAddress } that { isEqualTo(networkAddress) }
        }
    }
}
