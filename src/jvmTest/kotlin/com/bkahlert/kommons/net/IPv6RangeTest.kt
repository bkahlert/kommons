package com.bkahlert.kommons.net

import com.bkahlert.kommons.test.testEachOld
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isEqualTo

class IPv6RangeTest {

    private val ip1 = IPv6Address.parse("abba:4efa:abba:4efb:abba:4efb:172.186.78.250")
    private val ip2 = IPv6Address.parse("abba:4efa:abba:4efb:abba:4efb:192.168.0.1")
    private val range = ip1..ip2

    @TestFactory
    fun `should be parsable`() = testEachOld(
        "abba:4efa:abba:4efb:abba:4efb:172.186.78.250..abba:4efa:abba:4efb:abba:4efb:192.168.0.1",
        "abba:4efa:abba:4efb:abba:4efb:172.186.78.250 ..abba:4efa:abba:4efb:abba:4efb:192.168.0.1",
        "abba:4efa:abba:4efb:abba:4efb:172.186.78.250.. abba:4efa:abba:4efb:abba:4efb:192.168.0.1",
        " abba:4efa:abba:4efb:abba:4efb:172.186.78.250..abba:4efa:abba:4efb:abba:4efb:192.168.0.1",
        "abba:4efa:abba:4efb:abba:4efb:172.186.78.250..abba:4efa:abba:4efb:abba:4efb:192.168.0.1 ",
    ) { rangeString: String ->
        expecting { rangeString.toIPv6Range() } that { range }
        expecting { ip6RangeOf(rangeString) } that { range }
    }

    @TestFactory
    fun `should have start`() = testEachOld(
        range,
        IPv6Range.from(ip1, ip2),
    ) {
        expecting { start } that { isEqualTo(ip1) }
    }

    @TestFactory
    fun `should have inclusive end`() = testEachOld(
        range,
        IPv6Range.from(ip1, ip2),
    ) {
        expecting { endInclusive } that { isEqualTo(ip2) }
    }

    @TestFactory
    fun `should be representable`() = testEachOld(
        range,
        IPv6Range.from(ip1, ip2),
    ) {
        expecting { toString() } that { isEqualTo("abba:4efa:abba:4efb:abba:4efb:acba:4efa..abba:4efa:abba:4efb:abba:4efb:c0a8:1") }
    }
}
