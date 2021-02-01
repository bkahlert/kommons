package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger
import koodies.collections.to
import koodies.net.IPv4Address.Companion.RFC1918_16block
import koodies.net.IPv4Address.Companion.RFC1918_20block
import koodies.net.IPv4Address.Companion.RFC1918_24block
import koodies.net.IPv4Subnet.Companion.div
import koodies.net.IPv4Subnet.Companion.smallestCommonSubnet
import koodies.number.toUBytes
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
class IPv4AddressTest {

    @TestFactory
    fun `should be instantiatable`() =
        listOf(
            "192.168.16.1".toIp(),
            ipOf("192.168.16.1"),
            IPv4Address.parse("192.168.16.1"),
            IPv4Address(ubyteArrayOf(192.toUByte(), 168.toUByte(), 16.toUByte(), 1.toUByte())),
            IPv4Address(3232239617u.toUBytes()),
        ).testEach { ip ->
            expect { ip }.that { toStringIsEqualTo("192.168.16.1") }
        }

    @TestFactory
    fun `should throw on invalid value`() =
        listOf(
            { "-1.168.16.1".toIp() },
            { ipOf("192.999.16.1") },
            { IPv4Address.parse("192.168.16.1.2") },
            { IPv4Address.parse("192.168.16.x") },
        ).testEach { ip ->
            expectThrowing { ip() }.that { isFailure<IllegalArgumentException>() }
        }

    @Nested
    inner class Range {

        private val range = IPv4Address.parse("192.168.16.1")..IPv4Address.parse("192.168.16.3")

        @Test
        fun `should have start`() {
            expectThat(range.start).isEqualTo(IPv4Address.parse("192.168.16.1"))
        }

        @Test
        fun `should have endInclusive`() {
            expectThat(range.endInclusive).isEqualTo(IPv4Address.parse("192.168.16.3"))
        }

        @Test
        fun `should have contain IP`() {
            expectThat(range).contains(IPv4Address.parse("192.168.16.2"))
        }

        @Test
        fun `should have not contain IP`() {
            expectThat(range).not { contains(IPv4Address.parse("192.168.16.4")) }
        }

        @Test
        fun `should return empty range if greater start than end`() {
            expectThat(range.endInclusive..range.start).isEmpty()
        }

        @Test
        fun `should have subnet`() {
            expectThat(range.smallestCommonSubnet).toStringIsEqualTo("192.168.16.0/30")
        }

        @Test
        fun `should have usable`() {
            expectThat(range.smallestCommonSubnet.usable).isEqualTo(IPv4Address.parse("192.168.16.1")..IPv4Address.parse("192.168.16.2"))
        }

        @Test
        fun `should have firstUsableHost`() {
            expectThat(range.smallestCommonSubnet.firstUsableHost).isEqualTo(IPv4Address.parse("192.168.16.1"))
        }

        @Test
        fun `should have lastUsableHost`() {
            expectThat(range.smallestCommonSubnet.lastUsableHost).isEqualTo(IPv4Address.parse("192.168.16.2"))
        }

        @Test
        fun `should serialize to string`() {
            expectThat(range).toStringIsEqualTo("192.168.16.1..192.168.16.3")
        }
    }

    @Test
    fun `should create subnet`() {
        val ip = IPv4Address.parse("10.55.0.2")
        val cidr = 29
        expectThat(ip / cidr).isEqualTo(IPv4Subnet.from(ip, cidr))
    }

    @TestFactory
    fun `should contain RFC1918 blocks`() = listOf(
        RFC1918_24block to (8 to BigInteger.parseString("16777216", 10) to IPv4Address.parse("10.0.0.0")),
        RFC1918_20block to (12 to BigInteger.parseString("1048576", 10) to IPv4Address.parse("172.16.0.0")),
        RFC1918_16block to (16 to BigInteger.parseString("65536", 10) to IPv4Address.parse("192.168.0.0")),
    ).testEach { (range, expected) ->
        val (bitCount, hostCount, networkAddress) = expected
        with { range.toString() }.then {
            expect { range.smallestCommonSubnet.prefixLength }.that { isEqualTo(bitCount) }
            expect { range.smallestCommonSubnet.hostCount }.that { isEqualTo(hostCount) }
            expect { range.smallestCommonSubnet.networkAddress }.that { isEqualTo(networkAddress) }
        }
    }
}
