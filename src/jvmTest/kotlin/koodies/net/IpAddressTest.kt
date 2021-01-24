package koodies.net

import koodies.test.test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

@Execution(CONCURRENT)
class IpAddressTest {

    @Nested
    inner class Parse {

        @TestFactory
        fun `should parse IPv4 address`() =
            listOf(
                "192.168.16.1",
                "192.168.016.1",
                "192.168.016.001",
            ).test { ip ->
                expectThat(ip.toIp()).isEqualTo(IPv4Address.parse("192.168.16.1"))
            }

        @TestFactory
        fun `should parse IPv6 address`() =
            listOf(
                "::ffff:c0a8:1001",
                "0:0::ffff:192.168.016.001",
                "0:0:0:0:0:ffff:c0a8:1001",
            ).test { ip ->
                expectThat(ip.toIp()).isEqualTo(IPv6Address.parse("::ffff:c0a8:1001"))
            }

        @TestFactory
        fun `should throw on invalid IP address`() =
            listOf(
                { "-1.168.16.1".toIp() },
                { IPv4Address.parse("192.168.16.x") },
                { "-0:0:0::ffff:c0a8:1001".toIp() },
                { IPv6Address.parse("0:0:0::xxxx:c0a8:1001") },
            ).test { ip ->
                expectCatching { ip() }.isFailure().isA<IllegalArgumentException>()
            }
    }

    @Nested
    inner class Conversion {
        private val ipv4 = IPv4Address.parse("192.168.16.1")
        private val ipv6 = IPv6Address.parse("::ffff:192.168.16.1")

        @Test
        fun `should convert IPv4 to IPv6 address`() {
            expectThat(ipv4.toIPv6Address()).isEqualTo(ipv6)
        }

        @Test
        fun `should convert IPv6 to IPv4 address`() {
            expectThat(ipv6.toIPv4Address()).isEqualTo(ipv4)
        }

        @TestFactory
        fun `should throw on mapping non-mappable IP6 address`() {
            expectCatching { IPv6Address.RANGE.endInclusive.toIPv4Address() }.isFailure().isA<IllegalArgumentException>()
        }
    }
}
