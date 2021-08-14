package koodies.net

import koodies.test.testEach
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isEqualTo

class IPRangeTest {

    private val ip41 = IPv4Address.parse("172.186.78.250")
    private val ip42 = IPv4Address.parse("192.168.0.1")
    private val range4 = IPv4Range.from(ip41, ip42)

    private val ip61 = IPv6Address.parse("abba:4efa:abba:4efb:abba:4efb:172.186.78.250")
    private val ip62 = IPv6Address.parse("abba:4efa:abba:4efb:abba:4efb:192.168.0.1")
    private val range6 = IPv6Range.from(ip61, ip62)

    @TestFactory
    fun `should be parsable`() = testEach(
        listOf(
            "172.186.78.250..192.168.0.1",
            "172.186.78.250 ..192.168.0.1",
            "172.186.78.250.. 192.168.0.1",
            " 172.186.78.250..192.168.0.1",
            "172.186.78.250..192.168.0.1 ",
        ) to range4,
        listOf(
            "abba:4efa:abba:4efb:abba:4efb:172.186.78.250..abba:4efa:abba:4efb:abba:4efb:192.168.0.1",
            "abba:4efa:abba:4efb:abba:4efb:172.186.78.250 ..abba:4efa:abba:4efb:abba:4efb:192.168.0.1",
            "abba:4efa:abba:4efb:abba:4efb:172.186.78.250.. abba:4efa:abba:4efb:abba:4efb:192.168.0.1",
            " abba:4efa:abba:4efb:abba:4efb:172.186.78.250..abba:4efa:abba:4efb:abba:4efb:192.168.0.1",
            "abba:4efa:abba:4efb:abba:4efb:172.186.78.250..abba:4efa:abba:4efb:abba:4efb:192.168.0.1 ",
        ) to range6,
    ) { (rangeStrings, range) ->
        rangeStrings.forEach { rangeString ->
            expecting { rangeString.toIPRange() } that { isEqualTo(range) }
            expecting { ipRangeOf(rangeString) } that { isEqualTo(range) }
        }
    }
}
