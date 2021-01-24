package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.toBigInteger
import koodies.collections.to
import koodies.net.IPv4Address.Companion.RFC1918_16block
import koodies.net.IPv4Address.Companion.RFC1918_20block
import koodies.net.IPv4Address.Companion.RFC1918_24block
import koodies.test.test
import koodies.test.tests
import koodies.test.toStringIsEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

@Execution(CONCURRENT)
class IPv4AddressTest {

    @TestFactory
    fun `should be instantiatable`() =
        listOf(
            "192.168.16.1".toIp(),
            ipOf("192.168.16.1"),
            IPv4Address.parse("192.168.16.1"),
            IPv4Address(192.toByte(), 168.toByte(), 16.toByte(), 1.toByte()),
            IPv4Address(3232239617u.toInt()),
        ).test { ip ->
            expectThat(ip).toStringIsEqualTo("192.168.16.1")
        }

    @TestFactory
    fun `should throw on invalid value`() =
        listOf(
            { "-1.168.16.1".toIp() },
            { ipOf("192.999.16.1") },
            { IPv4Address.parse("192.168.16.1.2") },
            { IPv4Address.parse("192.168.16.x") },
        ).test { ip ->
            expectCatching { ip() }.isFailure().isA<IllegalArgumentException>()
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
        fun `should throw on greater start than end`() {
            expectCatching { range.endInclusive..range.start }.isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should have subnet`() {
            expectThat(range.subnet).toStringIsEqualTo("192.168.16.0/30")
        }

        @Test
        fun `should have usable`() {
            expectThat(range.usable).isEqualTo(IPv4Address.parse("192.168.16.1")..IPv4Address.parse("192.168.16.2"))
        }

        @Test
        fun `should have firstUsableHost`() {
            expectThat(range.firstUsableHost).isEqualTo(IPv4Address.parse("192.168.16.1"))
        }

        @Test
        fun `should have lastUsableHost`() {
            expectThat(range.lastUsableHost).isEqualTo(IPv4Address.parse("192.168.16.2"))
        }

        @Test
        fun `should serialize to string`() {
            expectThat(range).toStringIsEqualTo("192.168.16.1..192.168.16.3")
        }
    }

    @Nested
    inner class Subnet {

        private val range = IPv4Address.parse("10.55.0.2")..IPv4Address.parse("10.55.0.6")
        private val subnet = range.subnet

        @Test
        fun `should have subnetBitCount`() {
            expectThat(subnet.bitCount).isEqualTo(29)
        }

        @Test
        fun `should have wildcardBitCount`() {
            expectThat(subnet.wildcardBitCount).isEqualTo(3)
        }

        @Test
        fun `should have hostCount`() {
            expectThat(subnet.hostCount).isEqualTo(8.toBigInteger())
        }

        @Test
        fun `should have usableHostCount`() {
            expectThat(subnet.usableHostCount).isEqualTo(6.toBigInteger())
        }

        @Test
        fun `should have networkAddress`() {
            expectThat(subnet.networkAddress).isEqualTo(IPv4Address.parse("10.55.0.0"))
        }

        @Test
        fun `should have broadcastAddress`() {
            expectThat(subnet.broadcastAddress).isEqualTo(IPv4Address.parse("10.55.0.7"))
        }

        @Test
        fun `should have firstHost`() {
            expectThat(subnet.firstHost).isEqualTo(IPv4Address.parse("10.55.0.1"))
        }

        @Test
        fun `should have lastHost`() {
            expectThat(subnet.lastHost).isEqualTo(IPv4Address.parse("10.55.0.6"))
        }

        @Test
        fun `should have subnetMask`() {
            expectThat(subnet.mask).toStringIsEqualTo("255.255.255.248")
        }

        @Test
        fun `should serialize to string`() {
            expectThat(subnet).toStringIsEqualTo("10.55.0.0/29")
        }
    }

    @TestFactory
    fun `should contain RFC1918 blocks`() = listOf(
        RFC1918_24block to (8 to BigInteger.parseString("16777216", 10) to IPv4Address.parse("10.0.0.0")),
        RFC1918_20block to (12 to BigInteger.parseString("1048576", 10) to IPv4Address.parse("172.16.0.0")),
        RFC1918_16block to (16 to BigInteger.parseString("65536", 10) to IPv4Address.parse("192.168.0.0")),
    ).tests { (range, expected) ->
        val (bitCount, hostCount, networkAddress) = expected
        container(range.toString()) {
            test("should have subnetByteCount") { expectThat(range.subnet.bitCount).isEqualTo(bitCount) }
            test("should have hostCount") { expectThat(range.subnet.hostCount).isEqualTo(hostCount) }
            test("should have networkAddress") { expectThat(range.subnet.networkAddress).isEqualTo(networkAddress) }
        }
    }
}
