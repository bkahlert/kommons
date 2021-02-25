package koodies.net

import koodies.net.DefaultIPv4toIPv6Mapping.toIPv6Address
import koodies.net.IPv6Subnet.Companion.getSmallestCommonSubnet
import koodies.test.testEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

@Execution(SAME_THREAD)
class IPAddressTest {

    @Nested
    inner class Parse {

        @Test
        internal fun name() {
            val ip4 = ip4Of("192.168.16.25")
            val ip6: IPv6Address = ip4.toIPv6Address()
            val range: IPv6Range = ip6.."::ffff:c0a8:1028".toIPv6() // ::ffff:c0a8:1019..::ffff:c0a8:1028
            val subnet = ip6 / 122 // ::ffff:c0a8:1000/122
            check(getSmallestCommonSubnet(range) == subnet) // ✔
            check(subnet.broadcastAddress.toInetAddress().isSiteLocalAddress) // ✔
        }

        @TestFactory
        fun `should parse IPv4 address`() =
            listOf(
                "192.168.16.1",
                "192.168.016.1",
                "192.168.016.001",
            ).testEach { ip ->
                expect { ip.toIP() }.that { isEqualTo(IPv4Address.parse("192.168.16.1")) }
            }

        @TestFactory
        fun `should parse IPv6 address`() =
            listOf(
                "::ffff:c0a8:1001",
                "0:0::ffff:192.168.016.001",
                "0:0:0:0:0:ffff:c0a8:1001",
            ).testEach { ip ->
                expect { ip.toIP() }.that { isEqualTo(IPv6Address.parse("::ffff:c0a8:1001")) }
            }

        @TestFactory
        fun `should throw on invalid IP address`() =
            listOf(
                { "-1.168.16.1".toIP() },
                { IPv4Address.parse("192.168.16.x") },
                { "-0:0:0::ffff:c0a8:1001".toIP() },
                { IPv6Address.parse("0:0:0::xxxx:c0a8:1001") },
            ).testEach { ip ->
                expectThrowing { ip() }.that { isFailure().isA<IllegalArgumentException>() }
            }
    }
}
